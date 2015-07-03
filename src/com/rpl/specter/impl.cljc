(ns com.rpl.specter.impl
  #?(:cljs (:require-macros [com.rpl.specter.prot-opt-invoke
              :refer [mk-optimized-invocation]]))
  (:use [com.rpl.specter.protocols :only
    [comp-paths*
     select* transform* collect-val select-full* transform-full*]])
  (:require [com.rpl.specter.protocols :as p]
            [clojure.walk :as walk]
            [clojure.core.reducers :as r]
            [clojure.string :as s])
  )

#?(
:clj
(do
(defmacro throw* [etype & args]
  `(throw (new ~etype (pr-str ~@args))))

(defmacro throw-illegal [& args]
  `(throw* IllegalArgumentException ~@args)))


:cljs
(defn throw-illegal [& args]
  (throw (js/Error. (apply str args)))
  )
)

(defn benchmark [iters afn]
  (time
   (dotimes [_ iters]
     (afn))))

(deftype ExecutorFunctions [type select-executor transform-executor])

(def StructureValsPathExecutor
  (->ExecutorFunctions
    :svalspath
    (fn [selector structure]
      (selector [] structure
        (fn [vals structure]
          (if-not (empty? vals) [(conj vals structure)] [structure]))))
    (fn [transformer transform-fn structure]
      (transformer [] structure
        (fn [vals structure]
          (if (empty? vals)
            (transform-fn structure)
            (apply transform-fn (conj vals structure))))))
    ))

(def StructurePathExecutor
  (->ExecutorFunctions
    :spath
    (fn [selector structure]
      (selector structure (fn [structure] [structure])))
    (fn [transformer transform-fn structure]
      (transformer structure transform-fn))
    ))

(deftype TransformFunctions [executors selector transformer])


(defprotocol CoerceTransformFunctions
  (coerce-path [this]))

(defn- seq-contains? [aseq val]
  (->> aseq
       (filter (partial = val))
       empty?
       not))

(defn no-prot-error-str [obj]
  (str "Protocol implementation cannot be found for object.
        Extending Specter protocols should not be done inline in a deftype definition
        because that prevents Specter from finding the protocol implementations for
        optimized performance. Instead, you should extend the protocols via an
        explicit extend-protocol call. \n" obj))

#?(
:clj

(defn find-protocol-impl! [prot obj]
  (let [ret (find-protocol-impl prot obj)]
    (if (= ret obj)
      (throw-illegal (no-prot-error-str obj))
      ret
      )))
)

#?(:clj
(do
(defn structure-path-impl [this]
  (if (fn? this)
    ;;TODO: this isn't kosher, it uses knowledge of internals of protocols
    (-> p/StructurePath :impls (get clojure.lang.AFn))
    (find-protocol-impl! p/StructurePath this)))

(defn collector-impl [this]
  (find-protocol-impl! p/Collector this))

(defn structure-vals-path-impl [this]
  (find-protocol-impl! p/StructureValsPath this))
))


#?(:cljs
(do
(defn structure-path-impl [obj]
  {:select* (mk-optimized-invocation p/StructurePath obj select* 2)
   :transform* (mk-optimized-invocation p/StructurePath obj transform* 2)
   })

(defn collector-impl [obj]
  {:collect-val (mk-optimized-invocation p/Collector obj collect-val 1)
   })

(defn structure-vals-path-impl [obj]
  {:select-full* (mk-optimized-invocation p/StructureValsPath obj select-full* 3)
   :transform-full* (mk-optimized-invocation p/StructureValsPath obj transform-full* 3)
   })
))

(defn coerce-structure-vals-path [this]
  (let [pimpl (structure-vals-path-impl this)
        selector (:select-full* pimpl)
        transformer (:transform-full* pimpl)]
    (->TransformFunctions
      StructureValsPathExecutor
      (fn [vals structure next-fn]
        (selector this vals structure next-fn))
      (fn [vals structure next-fn]
        (transformer this vals structure next-fn)))
    ))

(defn coerce-collector [this]
  (let [cfn (->> this
                 collector-impl
                 :collect-val
                 )
        afn (fn [vals structure next-fn]
              (next-fn (conj vals (cfn this structure)) structure)
              )]
    (->TransformFunctions StructureValsPathExecutor afn afn)))


(defn coerce-structure-path [this]
  (let [pimpl (structure-path-impl this)
        selector (:select* pimpl)
        transformer (:transform* pimpl)]
    (->TransformFunctions
      StructurePathExecutor
      (fn [structure next-fn]
        (selector this structure next-fn))
      (fn [structure next-fn]
        (transformer this structure next-fn))
    )))

(defn coerce-structure-path-direct [this]
  (let [pimpl (structure-path-impl this)
        selector (:select* pimpl)
        transformer (:transform* pimpl)]
    (->TransformFunctions
      StructureValsPathExecutor
      (fn [vals structure next-fn]
        (selector this structure (fn [structure] (next-fn vals structure))))
      (fn [vals structure next-fn]
        (transformer this structure (fn [structure] (next-fn vals structure))))
    )))

(defn structure-path? [obj]
  (or (fn? obj) (satisfies? p/StructurePath obj)))

(extend-protocol CoerceTransformFunctions
  nil ; needs its own path because it doesn't count as an Object
  (coerce-path [this]
    (coerce-structure-path nil))

  TransformFunctions
  (coerce-path [this]
    this)

  
  #?(:clj java.util.List :cljs cljs.core/PersistentVector)
  (coerce-path [this]
    (comp-paths* this))

  #?@(:cljs [
    cljs.core/IndexedSeq
    (coerce-path [this]
      (coerce-path (vec this)))
    cljs.core/EmptyList
    (coerce-path [this]
      (coerce-path (vec this)))
    cljs.core/List
    (coerce-path [this]
      (coerce-path (vec this)))
    ])
  
  #?(:clj Object :cljs default)
  (coerce-path [this]
    (cond (structure-path? this) (coerce-structure-path this)
          (satisfies? p/Collector this) (coerce-collector this)
          (satisfies? p/StructureValsPath this) (coerce-structure-vals-path this)
          :else (throw-illegal (no-prot-error-str this))
      )))


(defn extype [^TransformFunctions f]
  (let [^ExecutorFunctions exs (.-executors f)]
    (.-type exs)
    ))

(defn- combine-same-types [[^TransformFunctions f & _ :as all]]
  (if (empty? all)
    (coerce-path nil)
    (let [^ExecutorFunctions exs (.-executors f)

          t (.-type exs)

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
                 (combiner (.-selector curr) (.-selector next))
                 (combiner (.-transformer curr) (.-transformer next))
                 ))
              all))))

(defn coerce-structure-vals [^TransformFunctions tfns]
  (if (= (extype tfns) :svalspath)
    tfns
    (let [selector (.-selector tfns)
          transformer (.-transformer tfns)]
      (->TransformFunctions
        StructureValsPathExecutor
        (fn [vals structure next-fn]
          (selector structure (fn [structure] (next-fn vals structure))))
        (fn [vals structure next-fn]
          (transformer structure (fn [structure] (next-fn vals structure))))
        ))))

(extend-protocol p/StructureValsPathComposer
  nil
  (comp-paths* [sp]
    (coerce-path sp))
  #?(:clj Object :cljs default)
  (comp-paths* [sp]
    (coerce-path sp))
  #?(:clj java.util.List :cljs cljs.core/PersistentVector)
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
        (satisfies? p/Collector this) (coerce-collector this)
        (satisfies? p/StructureValsPath this) (coerce-structure-vals-path this)
        (instance? TransformFunctions this) (coerce-structure-vals this)
        :else (throw-illegal (no-prot-error-str this))
  ))

;;this composes paths together much faster than comp-paths* but the resulting composition
;;won't execute as fast. Useful for when select/transform are used without pre-compiled paths
;;(where cost of compiling dominates execution time)
(defn comp-unoptimal [sp]
  (if (instance? #?(:clj java.util.List :cljs cljs.core/PersistentVector) sp)
    (->> sp
         (map coerce-structure-vals-direct)
         combine-same-types)
    (coerce-path sp)))

;; cell implementation idea taken from prismatic schema library
(defprotocol PMutableCell
  #?(:clj (get_cell [cell]))
  (set_cell [cell x]))

(deftype MutableCell [^:volatile-mutable q]
  PMutableCell
  #?(:clj (get_cell [cell] q))
  (set_cell [this x] (set! q x)))

(defn mutable-cell
  ([] (mutable-cell nil))
  ([init] (MutableCell. init)))

(defn set-cell! [cell val]
  (set_cell cell val))

(defn get-cell [cell]
  #?(:clj (get_cell cell) :cljs (.-q cell))
  )

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
  #?(:clj clojure.lang.PersistentVector :cljs cljs.core/PersistentVector)
  (set-first [v val]
    (assoc v 0 val))
  (set-last [v val]
    (assoc v (-> v count dec) val))
  #?(:clj Object :cljs default)
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

(defn compiled-select*
  [^com.rpl.specter.impl.TransformFunctions tfns structure]
  (let [^com.rpl.specter.impl.ExecutorFunctions ex (.-executors tfns)]
    ((.-select-executor ex) (.-selector tfns) structure)
    ))

(defn compiled-transform*
  [^com.rpl.specter.impl.TransformFunctions tfns transform-fn structure]
  (let [^com.rpl.specter.impl.ExecutorFunctions ex (.-executors tfns)]
    ((.-transform-executor ex) (.-transformer tfns) transform-fn structure)
    ))

(defn selected?*
  [compiled-path structure]
  (->> structure
       (compiled-select* compiled-path)
       empty?
       not))

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

(defn- filter+ancestry [path aseq]
  (let [aseq (vec aseq)]
    (reduce (fn [[s m :as orig] i]
              (let [e (get aseq i)
                    pos (count s)]
                (if (selected?* path e)
                  [(conj s e) (assoc m pos i)]
                  orig
                  )))
            [[] {}]
            (range (count aseq))
            )))

(defn key-select [akey structure next-fn]
  (next-fn (get structure akey)))

(defn key-transform [akey structure next-fn]
  (assoc structure akey (next-fn (get structure akey))
  ))

(deftype AllStructurePath [])

(extend-protocol p/StructurePath
  AllStructurePath
  (select* [this structure next-fn]
    (into [] (r/mapcat next-fn structure)))
  (transform* [this structure next-fn]
    (let [empty-structure (empty structure)]
      (if (list? empty-structure)
        ;; this is done to maintain order, otherwise lists get reversed
        (doall (map next-fn structure))
        (->> structure (r/map next-fn) (into empty-structure))
        ))))

(deftype ValCollect [])

(extend-protocol p/Collector
  ValCollect
  (collect-val [this structure]
    structure))

(deftype PosStructurePath [getter setter])

(extend-protocol p/StructurePath
  PosStructurePath
  (select* [this structure next-fn]
    (if-not (empty? structure)
      (next-fn ((.-getter this) structure))))
  (transform* [this structure next-fn]
    (if (empty? structure)
      structure
      ((.-setter this) structure (next-fn ((.-getter this) structure))))))

(deftype WalkerStructurePath [afn])

(extend-protocol p/StructurePath
  WalkerStructurePath
  (select* [^WalkerStructurePath this structure next-fn]
    (walk-select (.-afn this) next-fn structure))
  (transform* [^WalkerStructurePath this structure next-fn]
    (walk-until (.-afn this) next-fn structure)))

(deftype CodeWalkerStructurePath [afn])

(extend-protocol p/StructurePath
  CodeWalkerStructurePath
  (select* [^CodeWalkerStructurePath this structure next-fn]
    (walk-select (.-afn this) next-fn structure))
  (transform* [^CodeWalkerStructurePath this structure next-fn]
    (codewalk-until (.-afn this) next-fn structure)))


(deftype FilterStructurePath [path])

(extend-protocol p/StructurePath
  FilterStructurePath
  (select* [^FilterStructurePath this structure next-fn]
    (->> structure (filter #(selected?* (.-path this) %)) doall next-fn))
  (transform* [^FilterStructurePath this structure next-fn]
    (let [[filtered ancestry] (filter+ancestry (.-path this) structure)
          ;; the vec is necessary so that we can get by index later
          ;; (can't get by index for cons'd lists)
          next (vec (next-fn filtered))]
      (reduce (fn [curr [newi oldi]]
                (assoc curr oldi (get next newi)))
              (vec structure)
              ancestry))))

(deftype KeyPath [akey])

(extend-protocol p/StructurePath
  KeyPath
  (select* [^KeyPath this structure next-fn]
    (key-select (.-akey this) structure next-fn))
  (transform* [^KeyPath this structure next-fn]
    (key-transform (.-akey this) structure next-fn)
    ))

(deftype SelectCollector [sel-fn selector])

(extend-protocol p/Collector
  SelectCollector
  (collect-val [^SelectCollector this structure]
    ((.-sel-fn this) (.-selector this) structure)))

(deftype SRangePath [start-fn end-fn])

(extend-protocol p/StructurePath
  SRangePath
  (select* [^SRangePath this structure next-fn]
    (let [start ((.-start-fn this) structure)
          end ((.-end-fn this) structure)]
      (next-fn (-> structure vec (subvec start end)))
      ))
  (transform* [^SRangePath this structure next-fn]
    (let [start ((.-start-fn this) structure)
          end ((.-end-fn this) structure)
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

(extend-protocol p/StructurePath
  ViewPath
  (select* [^ViewPath this structure next-fn]
    (->> structure ((.-view-fn this)) next-fn))
  (transform* [^ViewPath this structure next-fn]
    (->> structure ((.-view-fn this)) next-fn)
    ))

(deftype PutValCollector [val])

(extend-protocol p/Collector
  PutValCollector
  (collect-val [^PutValCollector this structure]
    (.-val this)
    ))


(extend-protocol p/StructurePath
  nil
  (select* [this structure next-fn]
    (next-fn structure))
  (transform* [this structure next-fn]
    (next-fn structure)
    ))


(deftype ConditionalPath [cond-pairs])

(defn- retrieve-selector [cond-pairs structure]
  (->> cond-pairs
       (drop-while (fn [[c-selector _]]
                     (->> structure
                          (compiled-select* c-selector)
                          empty?)))
       first
       second
       ))

;;TODO: test nothing matches case
(extend-protocol p/StructurePath
  ConditionalPath
  (select* [this structure next-fn]
    (if-let [selector (retrieve-selector (.-cond-pairs this) structure)]
      (->> (compiled-select* selector structure)
           (mapcat next-fn)
           doall)))
  (transform* [this structure next-fn]
    (if-let [selector (retrieve-selector (.-cond-pairs this) structure)]
      (compiled-transform* selector next-fn structure)
      structure
      )))

(deftype MultiPath [paths])

(extend-protocol p/StructurePath
  MultiPath
  (select* [this structure next-fn]
    (->> (.-paths this)
         (mapcat #(compiled-select* % structure))
         (mapcat next-fn)
         doall
         ))
  (transform* [this structure next-fn]
    (reduce
      (fn [structure selector]
        (compiled-transform* selector next-fn structure))
      structure
      (.-paths this))
    ))

(defn filter-select [afn structure next-fn]
  (if (afn structure)
    (next-fn structure)))

(defn filter-transform [afn structure next-fn]
  (if (afn structure)
    (next-fn structure)
    structure))

