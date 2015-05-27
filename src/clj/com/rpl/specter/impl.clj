(ns com.rpl.specter.impl
  (:use [com.rpl.specter protocols])
  (:require [clojure.walk :as walk]
            [clojure.core.reducers :as r])
  )

(defmacro throw* [etype & args]
  `(throw (new ~etype (pr-str ~@args))))

(defmacro throw-illegal [& args]
  `(throw* IllegalArgumentException ~@args))

(defn benchmark [iters afn]
  (time
   (dotimes [_ iters]
     (afn))))

(deftype ExecutorFunctions [type select-executor update-executor])

(def StructureValsPathExecutor
  (->ExecutorFunctions
    :svalspath
    (fn [selector structure]
      (selector [] structure
        (fn [vals structure]
          (if-not (empty? vals) [(conj vals structure)] [structure]))))
    (fn [updater update-fn structure]
      (updater [] structure
        (fn [vals structure]
          (if (empty? vals)
            (update-fn structure)
            (apply update-fn (conj vals structure))))))
    ))

(def StructurePathExecutor
  (->ExecutorFunctions
    :spath
    (fn [selector structure]
      (selector structure (fn [structure] [structure])))
    (fn [updater update-fn structure]
      (updater structure update-fn))
    ))

(deftype TransformFunctions [executors selector updater])


(defprotocol CoerceTransformFunctions
  (coerce-path [this]))

(defn no-prot-error-str [obj]
  (str "Protocol implementation cannot be found for object.
        Extending Specter protocols should not be done inline in a deftype definition
        because that prevents Specter from finding the protocol implementations for 
        optimized performance. Instead, you should extend the protocols via an 
        explicit extend-protocol call. \n" obj))

(defn find-protocol-impl! [prot obj]
  (let [ret (find-protocol-impl prot obj)]
    (if (= ret obj)
      (throw-illegal (no-prot-error-str obj))
      ret
      )))

(defn coerce-structure-vals-path [this]
  (let [pimpl (find-protocol-impl! StructureValsPath this)
        selector (:select-full* pimpl)
        updater (:update-full* pimpl)]
    (->TransformFunctions
      StructureValsPathExecutor
      (fn [vals structure next-fn]
        (selector this vals structure next-fn))
      (fn [vals structure next-fn]
        (updater this vals structure next-fn)))
    ))

(defn coerce-collector [this]
  (let [cfn (->> this
                 (find-protocol-impl! Collector)
                 :collect-val
                 )
        afn (fn [vals structure next-fn]
              (next-fn (conj vals (cfn this structure)) structure)
              )]
    (->TransformFunctions StructureValsPathExecutor afn afn)))


(defn structure-path-impl [this]
  (if (fn? this) 
    ;;TODO: this isn't kosher, it uses knowledge of internals of protocols
    (-> StructurePath :impls (get clojure.lang.AFn))
    (find-protocol-impl! StructurePath this)))

(defn coerce-structure-path [this]
  (let [pimpl (structure-path-impl this)
        selector (:select* pimpl)
        updater (:update* pimpl)]
    (->TransformFunctions
      StructurePathExecutor
      (fn [structure next-fn]
        (selector this structure next-fn))
      (fn [structure next-fn]
        (updater this structure next-fn))
    )))

(defn coerce-structure-path-direct [this]
  (let [pimpl (structure-path-impl this)
        selector (:select* pimpl)
        updater (:update* pimpl)]
    (->TransformFunctions
      StructureValsPathExecutor
      (fn [vals structure next-fn]
        (selector this structure (fn [structure] (next-fn vals structure))))
      (fn [vals structure next-fn]
        (updater this structure (fn [structure] (next-fn vals structure))))
    )))

(defn obj-extends? [prot obj]
  (->> obj (find-protocol-impl prot) nil? not))

(defn structure-path? [obj]
  (or (fn? obj) (obj-extends? StructurePath obj)))

(extend-protocol CoerceTransformFunctions

  TransformFunctions
  (coerce-path [this]
    this)

  java.util.List
  (coerce-path [this]
    (comp-paths* this))

  Object
  (coerce-path [this]
    (cond (structure-path? this) (coerce-structure-path this)
          (obj-extends? Collector this) (coerce-collector this)
          (obj-extends? StructureValsPath this) (coerce-structure-vals-path this)
          :else (throw-illegal (no-prot-error-str this))
      )))


(defn extype [^TransformFunctions f]
  (let [^ExecutorFunctions exs (.executors f)]
    (.type exs)
    ))

(defn- combine-same-types [[^TransformFunctions f & _ :as all]]
  (let [^ExecutorFunctions exs (.executors f)
        
        t (.type exs)

        combiner
        (if (= t :svalspath)
          (fn [curr next]
            (fn [vals structure next-fn]
              (curr vals structure
                    (fn [vals-next structure-next]
                      (next vals-next structure-next next-fn)
                      ))))
          (fn [curr next]
            (fn [structure next-fn]
              (curr structure (fn [structure] (next structure next-fn)))))
          )]
          
    (reduce (fn [^TransformFunctions curr ^TransformFunctions next]
              (->TransformFunctions
                exs
                (combiner (.selector curr) (.selector next))
                (combiner (.updater curr) (.updater next))
                ))
             all)))

(defn coerce-structure-vals [^TransformFunctions tfns]
  (if (= (extype tfns) :svalspath)
    tfns
    (let [selector (.selector tfns)
          updater (.updater tfns)]
      (->TransformFunctions
        StructureValsPathExecutor
        (fn [vals structure next-fn]
          (selector structure (fn [structure] (next-fn vals structure))))
        (fn [vals structure next-fn]
          (updater structure (fn [structure] (next-fn vals structure))))
        ))))

(extend-protocol StructureValsPathComposer
  Object
  (comp-paths* [sp]
    (coerce-path sp))
  java.util.List
  (comp-paths* [structure-paths]
    (let [combined (->> structure-paths
                        (map coerce-path)
                        (partition-by extype)
                        (map combine-same-types)
                        )]
      (if (= 1 (count combined))
        (first combined)
        (->> combined
             (map coerce-structure-vals)
             combine-same-types)
        ))))

(defn coerce-structure-vals-direct [this]
  (cond (structure-path? this) (coerce-structure-path-direct this)
        (obj-extends? Collector this) (coerce-collector this)
        (obj-extends? StructureValsPath this) (coerce-structure-vals-path this)
        (instance? TransformFunctions this) this
        :else (throw-illegal (no-prot-error-str this))
  ))

;;this composes paths together much faster than comp-paths* but the resulting composition 
;;won't execute as fast. Useful for when select/update are used without pre-compiled paths
;;(where cost of compiling dominates execution time)
(defn comp-unoptimal [sp]
  (if (instance? java.util.List sp)
    (->> sp
         (map (fn [p] (-> p coerce-structure-vals-direct)))
         combine-same-types)    
    (coerce-path sp)))

;; cell implementation idea taken from prismatic schema library
(definterface PMutableCell
  (get_cell ^Object [])
  (set_cell [^Object x]))

(deftype MutableCell [^:volatile-mutable ^Object q]
  PMutableCell
  (get_cell [this] q)
  (set_cell [this x] (set! q x)))

(defn mutable-cell ^PMutableCell
  ([] (mutable-cell nil))
  ([init] (MutableCell. init)))

(defn set-cell! [^PMutableCell cell val]
  (.set_cell cell val))

(defn get-cell [^PMutableCell cell]
  (.get_cell cell))

(defn update-cell! [cell afn]
  (let [ret (afn (get-cell cell))]
    (set-cell! cell ret)
    ret))

(defn- append [coll elem]
  (-> coll vec (conj elem)))

(defprotocol SetExtremes
  (set-first [s val])
  (set-last [s val]))

(defn- set-first-list [l v]
  (cons v (rest l)))

(defn- set-last-list [l v]
  (append (butlast l) v))

(extend-protocol SetExtremes
  clojure.lang.PersistentVector
  (set-first [v val]
    (assoc v 0 val))
  (set-last [v val]
    (assoc v (-> v count dec) val))
  Object
  (set-first [l val]
    (set-first-list l val))
  (set-last [l val]
    (set-last-list l val)
    ))

(defn- walk-until [pred on-match-fn structure]
  (if (pred structure)
    (on-match-fn structure)    
    (walk/walk (partial walk-until pred on-match-fn) identity structure)    
    ))

(defn- fn-invocation? [f]
  (or (instance? clojure.lang.Cons f)
      (instance? clojure.lang.LazySeq f)
      (list? f)))

(defn- codewalk-until [pred on-match-fn structure]
  (if (pred structure)
    (on-match-fn structure)
    (let [ret (walk/walk (partial codewalk-until pred on-match-fn) identity structure)]
      (if (and (fn-invocation? structure) (fn-invocation? ret))        
        (with-meta ret (meta structure))
        ret
        ))))

(defn- conj-all! [cell elems]
  (set-cell! cell (concat (get-cell cell) elems)))

;; returns vector of all results
(defn- walk-select [pred continue-fn structure]
  (let [ret (mutable-cell [])
        walker (fn this [structure]
                 (if (pred structure)
                   (conj-all! ret (continue-fn structure))
                   (walk/walk this identity structure))
                 )]
    (walker structure)
    (get-cell ret)
    ))

(defn- filter+ancestry [afn aseq]
  (let [aseq (vec aseq)]
    (reduce (fn [[s m :as orig] i]
              (let [e (get aseq i)
                    pos (count s)]
                (if (afn e)
                  [(conj s e) (assoc m pos i)]
                  orig
                  )))
            [[] {}]            
            (range (count aseq))
            )))

(defn key-select [akey structure next-fn]
  (next-fn (get structure akey)))

(defn key-update [akey structure next-fn]
  (assoc structure akey (next-fn (get structure akey))
  ))

(deftype AllStructurePath [])

(extend-protocol StructurePath
  AllStructurePath
  (select* [this structure next-fn]
    (into [] (r/mapcat next-fn structure)))
  (update* [this structure next-fn]
    (let [empty-structure (empty structure)]
      (if (list? empty-structure)
        ;; this is done to maintain order, otherwise lists get reversed
        (doall (map next-fn structure))
        (->> structure (r/map next-fn) (into empty-structure))
        ))))

(deftype ValCollect [])

(extend-protocol Collector
  ValCollect
  (collect-val [this structure]
    structure))

(deftype LastStructurePath [])

(extend-protocol StructurePath
  LastStructurePath
  (select* [this structure next-fn]
    (next-fn (last structure)))
  (update* [this structure next-fn]
    (set-last structure (next-fn (last structure)))))

(deftype FirstStructurePath [])

(extend-protocol StructurePath
  FirstStructurePath
  (select* [this structure next-fn]
    (next-fn (first structure)))
  (update* [this structure next-fn]
    (set-first structure (next-fn (first structure)))))

(deftype WalkerStructurePath [afn])

(extend-protocol StructurePath
  WalkerStructurePath
  (select* [^WalkerStructurePath this structure next-fn]
    (walk-select (.afn this) next-fn structure))
  (update* [^WalkerStructurePath this structure next-fn]
    (walk-until (.afn this) next-fn structure)))

(deftype CodeWalkerStructurePath [afn])

(extend-protocol StructurePath
  CodeWalkerStructurePath
  (select* [^CodeWalkerStructurePath this structure next-fn]
    (walk-select (.afn this) next-fn structure))
  (update* [^CodeWalkerStructurePath this structure next-fn]
    (codewalk-until (.afn this) next-fn structure)))


(deftype FilterStructurePath [afn])

(extend-protocol StructurePath
  FilterStructurePath
  (select* [^FilterStructurePath this structure next-fn]
    (->> structure (filter (.afn this)) doall next-fn))
  (update* [^FilterStructurePath this structure next-fn]
    (let [[filtered ancestry] (filter+ancestry (.afn this) structure)
          ;; the vec is necessary so that we can get by index later
          ;; (can't get by index for cons'd lists)
          next (vec (next-fn filtered))]
      (reduce (fn [curr [newi oldi]]
                (assoc curr oldi (get next newi)))
              (vec structure)
              ancestry))))

(deftype KeyPath [akey])

(extend-protocol StructurePath
  KeyPath
  (select* [^KeyPath this structure next-fn]
    (key-select (.akey this) structure next-fn))
  (update* [^KeyPath this structure next-fn]
    (key-update (.akey this) structure next-fn)
    ))

(deftype SelectCollector [sel-fn selector])
  
(extend-protocol Collector
  SelectCollector
  (collect-val [^SelectCollector this structure]
    ((.sel-fn this) (.selector this) structure)))

(deftype SRangePath [start-fn end-fn])

(extend-protocol StructurePath
  SRangePath
  (select* [^SRangePath this structure next-fn]
    (let [start ((.start-fn this) structure)
          end ((.end-fn this) structure)]
      (next-fn (-> structure vec (subvec start end)))
      ))
  (update* [^SRangePath this structure next-fn]
    (let [start ((.start-fn this) structure)
          end ((.end-fn this) structure)
          structurev (vec structure)
          newpart (next-fn (-> structurev (subvec start end)))
          res (concat (subvec structurev 0 start)
                      newpart
                      (subvec structurev end (count structure)))]
      (if (vector? structure)
        (vec res)
        res
        ))))

(deftype ViewPath [view-fn])

(extend-protocol StructurePath
  ViewPath
  (select* [^ViewPath this structure next-fn]
    (->> structure ((.view-fn this)) next-fn))
  (update* [^ViewPath this structure next-fn]
    (->> structure ((.view-fn this)) next-fn)
    ))

(deftype IdentityPath [])

(extend-protocol StructurePath
  IdentityPath
  (select* [this structure next-fn]
    (next-fn structure))
  (update* [this structure next-fn]
    (next-fn structure)
    ))

(deftype PutValCollector [val])

(extend-protocol Collector
  PutValCollector
  (collect-val [^PutValCollector this structure]
    (.val this)
    ))

