(ns com.rpl.specter.impl
  #?(:cljs (:require-macros [com.rpl.specter.prot-opt-invoke
              :refer [mk-optimized-invocation]]))
  (:use [com.rpl.specter.protocols :only
    [select* transform* collect-val]])
  (:require [com.rpl.specter.protocols :as p]
            [clojure.walk :as walk]
            [clojure.core.reducers :as r]
            [clojure.string :as s])
  )

(def ^:dynamic *tmp-closure*)
(defn closed-code [closure body]
  (let [lv (mapcat #(vector % `(*tmp-closure* '~%))
                   (keys closure))]
    (binding [*tmp-closure* closure]
      (eval `(let [~@lv] ~body)))))

(defprotocol PathComposer
  (comp-paths* [paths]))

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

(def RichPathExecutor
  (->ExecutorFunctions
    :richpath
    (fn [params params-idx selector structure]
      (selector params params-idx [] structure
        (fn [_ _ vals structure]
          (if-not (empty? vals) [(conj vals structure)] [structure]))))
    (fn [params params-idx transformer transform-fn structure]
      (transformer params params-idx [] structure
        (fn [_ _ vals structure]
          (if (empty? vals)
            (transform-fn structure)
            (apply transform-fn (conj vals structure))))))
    ))

(def StructurePathExecutor
  (->ExecutorFunctions
    :spath
    (fn [params params-idx selector structure]
      (selector structure (fn [structure] [structure])))
    (fn [params params-idx transformer transform-fn structure]
      (transformer structure transform-fn))
    ))

(defrecord TransformFunctions [executors selector transformer])

(defrecord CompiledPath [transform-fns params params-idx])

(defn no-params-compiled-path [transform-fns]
  (->CompiledPath transform-fns nil 0))

;;TODO: this must implement IFn so it can be transformed to CompiledPath
;; (just calls bind-params)
(defrecord ParamsNeededPath [transform-fns num-needed-params])


(defn bind-params [^ParamsNeededPath params-needed-path params idx]
  (->CompiledPath
    (.-transform-fns params-needed-path)
    params
    idx))

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
))

(defn coerce-collector [this]
  (let [cfn (->> this
                 collector-impl
                 :collect-val
                 )
        afn (fn [params params-idx vals structure next-fn]
              (next-fn params params-idx (conj vals (cfn this structure)) structure)
              )]
    (no-params-compiled-path
      (->TransformFunctions RichPathExecutor afn afn)
      )))


(defn coerce-structure-path [this]
  (let [pimpl (structure-path-impl this)
        selector (:select* pimpl)
        transformer (:transform* pimpl)]
    (no-params-compiled-path
      (->TransformFunctions
        StructurePathExecutor
        (fn [structure next-fn]
          (selector this structure next-fn))
        (fn [structure next-fn]
          (transformer this structure next-fn)))
      )))

(defn coerce-structure-path-rich [this]
  (let [pimpl (structure-path-impl this)
        selector (:select* pimpl)
        transformer (:transform* pimpl)]
    (no-params-compiled-path
      (->TransformFunctions
        RichPathExecutor
        (fn [params params-idx vals structure next-fn]
          (selector this structure (fn [structure] (next-fn params params-idx vals structure))))
        (fn [params params-idx vals structure next-fn]
          (transformer this structure (fn [structure] (next-fn params params-idx vals structure)))))
      )))

(defn structure-path? [obj]
  (or (fn? obj) (satisfies? p/StructurePath obj)))

(defprotocol CoercePath
  (coerce-path [this]))

(extend-protocol CoercePath
  nil ; needs its own path because it doesn't count as an Object
  (coerce-path [this]
    (coerce-structure-path nil))

  CompiledPath
  (coerce-path [this]
    this)

  ParamsNeededPath
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
          :else (throw-illegal (no-prot-error-str this))
      )))


(defn extype [^TransformFunctions f]
  (let [^ExecutorFunctions exs (.-executors f)]
    (.-type exs)
    ))

(defn- combine-same-types [[^TransformFunctions f & _ :as all]]
  (let [^ExecutorFunctions exs (.-executors f)

        t (.-type exs)

        combiner
        (if (= t :richpath)
          (fn [curr next]
            (fn [params params-idx vals structure next-fn]
              (curr params params-idx vals structure
                    (fn [params-next params-idx-next vals-next structure-next]
                      (next params-next params-idx-next vals-next structure-next next-fn)
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
            all)))

(defn coerce-tfns-rich [^TransformFunctions tfns]
  (if (= (extype tfns) :richpath)
    tfns
    (let [selector (.-selector tfns)
          transformer (.-transformer tfns)]
      (->TransformFunctions
        RichPathExecutor
        (fn [params params-idx vals structure next-fn]
          (selector structure (fn [structure] (next-fn params params-idx vals structure))))
        (fn [params params-idx vals structure next-fn]
          (transformer structure (fn [structure] (next-fn params params-idx vals structure))))
        ))))

(defn capture-params-internally [path]
  (if-not (instance? CompiledPath path)
    path
    (let [params (:params path)
          selector (-> path :transform-fns :selector)
          transformer (-> path :transform-fns :transformer)]
      (if (empty? params)
        path
        (->CompiledPath
          (->TransformFunctions
            RichPathExecutor
            (fn [x-params params-idx vals structure next-fn]
              (selector params 0 vals structure
                (fn [_ _ vals-next structure-next]
                  (next-fn x-params params-idx vals-next structure-next)
                  )))
            (fn [x-params params-idx vals structure next-fn]
              (transformer params 0 vals structure
                (fn [_ _ vals-next structure-next]
                  (next-fn x-params params-idx vals-next structure-next)
                  ))))
          params
          0
          )))))

(extend-protocol PathComposer
  nil
  (comp-paths* [sp]
    (coerce-path sp))
  #?(:clj Object :cljs default)
  (comp-paths* [sp]
    (coerce-path sp))
  #?(:clj java.util.List :cljs cljs.core/PersistentVector)
  (comp-paths* [structure-paths]
    (if (empty? structure-paths)
      (coerce-path nil)
      (let [coerced (->> structure-paths
                         (map coerce-path)
                         (map capture-params-internally))
            combined (->> coerced
                          (map :transform-fns)
                          (partition-by extype)
                          (map combine-same-types)
                          )
            result-tfn (if (= 1 (count combined))
                         (first combined)
                         (->> combined
                              (map coerce-tfns-rich)
                              combine-same-types)
                         )
            needs-params-paths (filter #(instance? ParamsNeededPath %) coerced)]
        (if (empty? needs-params-paths)
          (no-params-compiled-path result-tfn)
          (->ParamsNeededPath
            (coerce-tfns-rich result-tfn)
            (->> needs-params-paths
                 (map :num-needed-params)
                 (reduce +))
            ))
        ))))


;; parameterized path helpers

(defn determine-params-impls [[name1 & impl1] [name2 & impl2]]
  (if (= name1 'select*)
    [impl1 impl2]
    [impl2 impl1]))


(def PARAMS-SYM (vary-meta (gensym "params") assoc :tag 'objects))
(def PARAMS-IDX-SYM (gensym "params-idx"))

(defn paramspath* [bindings num-params [impl1 impl2]]
  (let [[[[_ s-structure-sym s-next-fn-sym] & select-body]
         [[_ t-structure-sym t-next-fn-sym] & transform-body]]
         (determine-params-impls impl1 impl2)

        params-sym (gensym "params")
        params-idx-sym (gensym "params-idx")]
    `(->ParamsNeededPath
       (->TransformFunctions
         RichPathExecutor
         (fn [~PARAMS-SYM ~PARAMS-IDX-SYM vals# ~s-structure-sym next-fn#]
           (let [~s-next-fn-sym (fn [structure#]
                                  (next-fn#                                    
                                    ~PARAMS-SYM
                                    (+ ~PARAMS-IDX-SYM ~num-params)
                                    vals#
                                    structure#))
                 ~@bindings]
             ~@select-body
             ))
         (fn [~PARAMS-SYM ~PARAMS-IDX-SYM vals# ~t-structure-sym next-fn#]
           (let [~t-next-fn-sym (fn [structure#]
                                  (next-fn#
                                    ~PARAMS-SYM
                                    (+ ~PARAMS-IDX-SYM ~num-params)
                                    vals#                                    
                                    structure#))
                 ~@bindings]
             ~@transform-body
             )))
       ~num-params
       )))

(defn num-needed-params [path]
  (if (instance? CompiledPath path)
    0
    (:num-needed-params path)))

(defn params-paramspath* [bindings impls]
  (let [num-params-seq (->> bindings
                           (map last)
                           (map num-needed-params)
                           (reductions +)
                           (cons 0))
        num-params (last num-params-seq)
        closure (->> bindings (map rest) (into {}))
        make-paths (->> bindings
                     (map (fn [offset [late-sym path-sym path]]
                            [late-sym
                             (if (instance? CompiledPath path)
                              path-sym
                              `(bind-params ~path-sym ~PARAMS-SYM (+ ~PARAMS-IDX-SYM ~offset))
                              )
                             ])
                            num-params-seq)
                     (apply concat))
        params-needed-path (closed-code closure (paramspath* make-paths num-params impls))]
    (if (= num-params 0)
      (bind-params params-needed-path nil 0)
      params-needed-path)
    ))

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
  [^com.rpl.specter.impl.CompiledPath path structure]
  (let [^com.rpl.specter.impl.TransformFunctions tfns (.-transform-fns path)
        ^com.rpl.specter.impl.ExecutorFunctions ex (.-executors tfns)]
    ((.-select-executor ex) (.-params path) (.-params-idx path) (.-selector tfns) structure)
    ))

(defn compiled-transform*
  [^com.rpl.specter.impl.CompiledPath path transform-fn structure]
  (let [^com.rpl.specter.impl.TransformFunctions tfns (.-transform-fns path)
        ^com.rpl.specter.impl.ExecutorFunctions ex (.-executors tfns)]
    ((.-transform-executor ex) (.-params path) (.-params-idx path) (.-transformer tfns) transform-fn structure)
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

