(ns com.rpl.specter.impl
  #+cljs (:require-macros
            [com.rpl.specter.prot-opt-invoke
              :refer [mk-optimized-invocation]]
            [com.rpl.specter.defhelpers :refer [define-ParamsNeededPath]]
            )
  (:use [com.rpl.specter.protocols :only
    [select* transform* collect-val]])
  (:require [com.rpl.specter.protocols :as p]
            [clojure.walk :as walk]
            #+clj [clojure.core.reducers :as r]
            [clojure.string :as s]
            #+clj [com.rpl.specter.defhelpers :as dh]
            )
  #+clj
  (:import [com.rpl.specter Util])
  )

(defn spy [e]
  (println e)
  e)

(defprotocol PathComposer
  (comp-paths* [paths]))

(defn- smart-str* [o]
  (if (coll? o)
    (pr-str o)
    (str o)))

(defn smart-str [& elems]
  (apply str (map smart-str* elems)))

#+clj
(defmacro throw* [etype & args]
  `(throw (new ~etype (smart-str ~@args))))

#+clj
(defmacro throw-illegal [& args]
  `(throw* IllegalArgumentException ~@args))


#+cljs
(defn throw-illegal [& args]
  (throw (js/Error. (apply str args)))
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

(def LeanPathExecutor
  (->ExecutorFunctions
    :leanpath
    (fn [params params-idx selector structure]
      (selector structure (fn [structure] [structure])))
    (fn [params params-idx transformer transform-fn structure]
      (transformer structure transform-fn))
    ))

(defrecord TransformFunctions [executors selector transformer])

(defrecord CompiledPath [transform-fns params params-idx])

(defn compiled-path? [o]
  (instance? CompiledPath o))

(defn no-params-compiled-path [transform-fns]
  (->CompiledPath transform-fns nil 0))


(declare bind-params*)

#+clj
(defmacro fast-object-array [i]
  `(com.rpl.specter.Util/makeObjectArray ~i))

#+cljs
(defn fast-object-array [i]
  (object-array i))


#+clj
(dh/define-ParamsNeededPath
  true
  clojure.lang.IFn
  invoke
  (applyTo [this args]
    (let [a (object-array args)]
      (com.rpl.specter.impl/bind-params* this a 0))))

#+cljs
(define-ParamsNeededPath
  false
  cljs.core/IFn
  -invoke
  (-invoke [this p01 p02 p03 p04 p05 p06 p07 p08 p09 p10
                 p11 p12 p13 p14 p15 p16 p17 p18 p19 p20
                 rest]
    (let [a (object-array
              (concat 
                [p01 p02 p03 p04 p05 p06 p07 p08 p09 p10
                p11 p12 p13 p14 p15 p16 p17 p18 p19 p20]
                rest))]
      (com.rpl.specter.impl/bind-params* this a 0))
    ))

(defn params-needed-path? [o]
  (instance? ParamsNeededPath o))

(defn bind-params* [^ParamsNeededPath params-needed-path params idx]
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

#+clj

(defn find-protocol-impl! [prot obj]
  (let [ret (find-protocol-impl prot obj)]
    (if (= ret obj)
      (throw-illegal (no-prot-error-str obj))
      ret
      )))

#+clj
(defn structure-path-impl [this]
  (if (fn? this)
    ;;TODO: this isn't kosher, it uses knowledge of internals of protocols
    (-> p/Navigator :impls (get clojure.lang.AFn))
    (find-protocol-impl! p/Navigator this)))

#+clj
(defn collector-impl [this]
  (find-protocol-impl! p/Collector this))


#+cljs
(defn structure-path-impl [obj]
  {:select* (mk-optimized-invocation p/Navigator obj select* 2)
   :transform* (mk-optimized-invocation p/Navigator obj transform* 2)
   })

#+cljs
(defn collector-impl [obj]
  {:collect-val (mk-optimized-invocation p/Collector obj collect-val 1)
   })

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
        LeanPathExecutor
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
  (or (fn? obj) (satisfies? p/Navigator obj)))

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
  
  #+clj java.util.List #+cljs cljs.core/PersistentVector
  (coerce-path [this]
    (comp-paths* this))

  #+cljs cljs.core/IndexedSeq
  #+cljs (coerce-path [this]
           (coerce-path (vec this)))
  #+cljs cljs.core/EmptyList
  #+cljs (coerce-path [this]
           (coerce-path (vec this)))
  #+cljs cljs.core/List
  #+cljs (coerce-path [this]
           (coerce-path (vec this)))
  #+cljs cljs.core/LazySeq
  #+cljs (coerce-path [this]
           (coerce-path (vec this)))
  
  #+clj Object #+cljs default
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
          params-idx (:params-idx path)
          selector (-> path :transform-fns :selector)
          transformer (-> path :transform-fns :transformer)]
      (if (empty? params)
        path
        (no-params-compiled-path
          (->TransformFunctions
            RichPathExecutor
            (fn [x-params x-params-idx vals structure next-fn]
              (selector params params-idx vals structure
                (fn [_ _ vals-next structure-next]
                  (next-fn x-params x-params-idx vals-next structure-next)
                  )))
            (fn [x-params x-params-idx vals structure next-fn]
              (transformer params params-idx vals structure
                (fn [_ _ vals-next structure-next]
                  (next-fn x-params x-params-idx vals-next structure-next)
                  ))))
          )))))

(extend-protocol PathComposer
  nil
  (comp-paths* [sp]
    (coerce-path sp))
  #+clj Object #+cljs default
  (comp-paths* [sp]
    (coerce-path sp))
  #+clj java.util.List #+cljs cljs.core/PersistentVector
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


(defn num-needed-params [path]
  (if (instance? CompiledPath path)
    0
    (:num-needed-params path)))


;; cell implementation idea taken from prismatic schema library
(defprotocol PMutableCell
  #+clj (get_cell [cell])
  (set_cell [cell x]))

(deftype MutableCell [^:volatile-mutable q]
  PMutableCell
  #+clj (get_cell [cell] q)
  (set_cell [this x] (set! q x)))

(defn mutable-cell
  ([] (mutable-cell nil))
  ([init] (MutableCell. init)))

(defn set-cell! [cell val]
  (set_cell cell val))

(defn get-cell [cell]
  #+clj (get_cell cell) #+cljs (.-q cell)
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
  #+clj clojure.lang.PersistentVector #+cljs cljs.core/PersistentVector
  (set-first [v val]
    (assoc v 0 val))
  (set-last [v val]
    (assoc v (-> v count dec) val))
  #+clj Object #+cljs default
  (set-first [l val]
    (set-first-list l val))
  (set-last [l val]
    (set-last-list l val)
    ))

(defn walk-until [pred on-match-fn structure]
  (if (pred structure)
    (on-match-fn structure)
    (walk/walk (partial walk-until pred on-match-fn) identity structure)
    ))

(defn fn-invocation? [f]
  (or (instance? clojure.lang.Cons f)
      (instance? clojure.lang.LazySeq f)
      (list? f)))

(defn codewalk-until [pred on-match-fn structure]
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

(defn not-selected?*
  [compiled-path structure]
  (->> structure
       (compiled-select* compiled-path)
       empty?))

(defn selected?*
  [compiled-path structure]
  (not (not-selected?* compiled-path structure)))

;; returns vector of all results
(defn walk-select [pred continue-fn structure]
  (let [ret (mutable-cell [])
        walker (fn this [structure]
                 (if (pred structure)
                   (conj-all! ret (continue-fn structure))
                   (walk/walk this identity structure))
                 )]
    (walker structure)
    (get-cell ret)
    ))

(defn key-select [akey structure next-fn]
  (next-fn (get structure akey)))

(defn key-transform [akey structure next-fn]
  (assoc structure akey (next-fn (get structure akey))
  ))

#+clj
(defn all-select [structure next-fn]
  (into [] (r/mapcat next-fn structure)))

#+cljs
(defn all-select [structure next-fn]
  (into [] (mapcat #(next-fn %)) structure))

#+cljs
(defn queue? [coll]
  (= (type coll) (type #queue [])))

#+clj
(defn queue? [coll]
  (instance? clojure.lang.PersistentQueue coll))

#+clj
(defn all-transform [structure next-fn]
  (let [empty-structure (empty structure)]
    (cond (and (list? empty-structure) (not (queue? empty-structure)))
          ;; this is done to maintain order, otherwise lists get reversed
          (doall (map next-fn structure))

          (map? structure)
          (->> structure (r/map vec) (r/map next-fn) (into empty-structure))

          :else
          (->> structure (r/map next-fn) (into empty-structure))
      )))

#+cljs
(defn all-transform [structure next-fn]
  (let [empty-structure (empty structure)]
    (if (and (list? empty-structure) (not (queue? empty-structure)))
        ;; this is done to maintain order, otherwise lists get reversed
        (doall (map next-fn structure))
        (into empty-structure (map #(next-fn %)) structure)
        )))

(deftype AllNavigator [])

(extend-protocol p/Navigator
  AllNavigator
  (select* [this structure next-fn]
    (all-select structure next-fn))
  (transform* [this structure next-fn]
    (all-transform structure next-fn)))

(deftype ValCollect [])

(extend-protocol p/Collector
  ValCollect
  (collect-val [this structure]
    structure))

(deftype PosNavigator [getter setter])

(extend-protocol p/Navigator
  PosNavigator
  (select* [this structure next-fn]
    (if-not (empty? structure)
      (next-fn ((.-getter this) structure))))
  (transform* [this structure next-fn]
    (if (empty? structure)
      structure
      ((.-setter this) structure (next-fn ((.-getter this) structure))))))

(defn srange-select [structure start end next-fn]
  (next-fn (-> structure vec (subvec start end))))

(defn srange-transform [structure start end next-fn]
  (let [structurev (vec structure)
        newpart (next-fn (-> structurev (subvec start end)))
        res (concat (subvec structurev 0 start)
                    newpart
                    (subvec structurev end (count structure)))]
    (if (vector? structure)
      (vec res)
      res
      )))

(extend-protocol p/Navigator
  nil
  (select* [this structure next-fn]
    (next-fn structure))
  (transform* [this structure next-fn]
    (next-fn structure)
    ))

(defn retrieve-cond-selector [cond-paths structure]
  (->> cond-paths
       (partition 2)
       (drop-while (fn [[c-selector _]]
                     (->> structure
                          (compiled-select* c-selector)
                          empty?)))
       first
       second
       ))

(defn filter-select [afn structure next-fn]
  (if (afn structure)
    (next-fn structure)))

(defn filter-transform [afn structure next-fn]
  (if (afn structure)
    (next-fn structure)
    structure))

(defn compiled-selector [^com.rpl.specter.impl.CompiledPath path]
  (let [^com.rpl.specter.impl.TransformFunctions tfns (.-transform-fns path)]
    (.-selector tfns)))

(defn compiled-transformer [^com.rpl.specter.impl.CompiledPath path]
  (let [^com.rpl.specter.impl.TransformFunctions tfns (.-transform-fns path)]
    (.-transformer tfns)))

(defn params-needed-selector [^com.rpl.specter.impl.ParamsNeededPath path]
  (let [^com.rpl.specter.impl.TransformFunctions tfns (.-transform-fns path)]
    (.-selector tfns)))

(defn params-needed-transformer [^com.rpl.specter.impl.ParamsNeededPath path]
  (let [^com.rpl.specter.impl.TransformFunctions tfns (.-transform-fns path)]
      (.-transformer tfns)))


(defrecord LocalSym
  [val sym])

(defrecord VarUse
  [var sym])

(defrecord SpecialFormUse
  [val code])

(defrecord FnInvocation
  ;; op and params elems can be any of the above
  [op params code])

(defrecord CachedPathInfo
  [precompiled ; can be null
   params-maker ; can be null
   ])

(defonce PATH-CACHE
  #+clj (mutable-cell (java.util.HashMap.))
  #+cljs (atom {})
  )

(def ^:dynamic *must-cache-paths* false)

(defn must-cache-paths!
  ([] (must-cache-paths! true))
  ([v] (alter-var-root #'*must-cache-paths* (constantly v))))

#+clj
(defn add-path-cache! [k v]
  ;; This looks very inefficient but is actually the best approach
  ;; without real inline caches. The total number of copies will be the
  ;; number of Specter callsites (plus perhaps a few more if 
  ;; multiple callsites attempt to be cached at exactly the same time).
  ;; Eventually it will stabilize and there won't be any more garbage generated.
  ;; The `select` performance using this cache strategy is ~20% faster for
  ;; the [:a :b :c] path use case than ConcurrentHashMap. 
  (let [newmap (java.util.HashMap. ^java.util.HashMap (get-cell PATH-CACHE))]
    (.put newmap k v)
    (set-cell! PATH-CACHE newmap)
    ))

#+clj
(defn get-path-cache [k]
  (.get ^java.util.HashMap (get-cell PATH-CACHE) k))

#+cljs
(defn add-cache! [k v]
  (swap! PATH-CACHE (fn [m] (assoc m k v))))

#+cljs
(defn get-cache [k]
  (get @PATH-CACHE k))

(defn- extract-original-code [p]
  (cond
    (instance? LocalSym p) (:sym p)
    (instance? VarUse p) (:sym p)
    (instance? SpecialFormUse p) (:code p)
    (instance? FnInvocation p) (:code p)
    :else p
    ))

(defn- valid-navigator? [v]
  (or (structure-path? v)
      (satisfies? p/Collector v)
      (instance? CompiledPath v)))

(defn magic-fail! [failed-atom]
  (reset! failed-atom true)
  nil)

(def pred*
  (->ParamsNeededPath
    (->TransformFunctions
      RichPathExecutor
      (fn [params params-idx vals structure next-fn]
        (let [afn (aget ^objects params params-idx)]
          (if (afn structure)
            (next-fn params (inc params-idx) vals structure)
            )))
      (fn [params params-idx vals structure next-fn]
        (let [afn (aget ^objects params params-idx)]
          (if (afn structure)
            (next-fn params (inc params-idx) vals structure)
            structure
            ))))
    1
    ))

(defn- magic-precompilation* [p params-atom failed-atom]
  (let [magic-fail! (fn [& reason]
                      (if *must-cache-paths*
                        (println "Failed to cache path:" (apply str reason)))
                      (reset! failed-atom true)
                      nil)]
    (cond
      (vector? p)
      (mapv
        #(magic-precompilation* % params-atom failed-atom)
        p)

      (instance? LocalSym p)
      (magic-fail! "Local symbol " (:sym p) " where navigator expected")

      (instance? VarUse p)
      (let [v (:var p)
            vv (var-get v)]
        (cond (-> v meta :dynamic) (magic-fail! "Var " (:sym p) " is dynamic")
              (valid-navigator? vv) vv
              :else (magic-fail! "Var " (:sym p) " is not a navigator")
              ))

      (instance? SpecialFormUse p)
      (if (-> p :code first (= 'fn*))
        (do
          (swap! params-atom conj (:code p))
          pred*
          )
        (magic-fail! "Special form " (:code p) " where navigator expected")
        )

      (instance? FnInvocation p)
      (let [op (:op p)
            ps (:params p)]
        (if (instance? VarUse op)
          (let [v (:var op)
                vv (var-get v)]
            (if (-> v meta :dynamic)
              (magic-fail! "Var " (:sym op) " is dynamic")
              (cond
                (instance? ParamsNeededPath vv)
                ;;TODO: if all params are constants, then just bind the path right here
                ;;otherwise, add the params
                ;;  - could extend this to see if it contains nested function calls which
                ;;    are only on constants
                (do
                  (swap! params-atom #(vec (concat % ps)))
                  vv
                  )

                (and (fn? vv) (-> vv meta :pathedfn))
                (let [subpath (mapv #(magic-precompilation* % params-atom failed-atom)
                                    ps)]
                  (if @failed-atom
                    nil
                    (apply vv subpath)
                    ))

                :else
                (magic-fail! "Var " (:sym op) " must be either a parameterized "
                  "navigator or a higher order pathed constructor function")
                )))
          (magic-fail! "Code at " (extract-original-code p) " is in "
            "function invocation position and must be either a parameterized "
            "navigator or a higher order pathed constructor function"
            )
          ))

      :else
      (cond (set? p)
            (do (swap! params-atom conj p)
                pred*)

            (keyword? p)
            p

            :else
            (magic-fail! "Code " p " is not a valid navigator or can't be factored")
            )
      )))

(defn magic-precompilation [prepared-path used-locals]
  (let [params-atom (atom [])
        failed-atom (atom false)
        path (magic-precompilation* prepared-path params-atom failed-atom)
        ]
    (if @failed-atom
      (if *must-cache-paths*
        (throw-illegal "Failed to cache path")
        (->CachedPathInfo nil nil))
      (let [precompiled (comp-paths* path)
            params-code (mapv extract-original-code @params-atom)
            array-sym (gensym "array")
            params-maker
            (if-not (empty? params-code)
              (eval
                `(fn [~@used-locals]
                   (let [~array-sym (fast-object-array ~(count params-code))]
                     ~@(map-indexed
                         (fn [i c]
                          `(aset ~array-sym ~i ~c))
                         params-code
                         )
                     ~array-sym
                     ))))
            ]
        ;; TODO: error if precompiled is compiledpath and there are params or
        ;; precompiled is paramsneededpath and there are no params...
        (->CachedPathInfo precompiled params-maker)
        ))
    ))


(defn compiled-select-one* [path structure]
  (let [res (compiled-select* path structure)]
    (when (> (count res) 1)
      (throw-illegal "More than one element found for params: " path structure))
    (first res)
    ))

(defn compiled-select-one!* [path structure]
  (let [res (compiled-select* path structure)]
    (when (not= 1 (count res)) (throw-illegal "Expected exactly one element for params: " path structure))
    (first res)
    ))

(defn compiled-select-first* [path structure]
  (first (compiled-select* path structure)))

(defn compiled-setval* [path val structure]
  (compiled-transform* path (fn [_] val) structure))

(defn compiled-replace-in*
  [path transform-fn structure & {:keys [merge-fn] :or {merge-fn concat}}]
  (let [state (mutable-cell nil)]
    [(compiled-transform* path
             (fn [& args]
               (let [res (apply transform-fn args)]
                 (if res
                   (let [[ret user-ret] res]
                     (->> user-ret
                          (merge-fn (get-cell state))
                          (set-cell! state))
                     ret)
                   (last args)
                   )))
             structure)
     (get-cell state)]
    ))

#+clj
(defn extend-protocolpath* [protpath protpath-prot extensions]
  (let [extensions (partition 2 extensions)
        m (-> protpath-prot :sigs keys first)
        expected-params (num-needed-params protpath)]
    (doseq [[atype apath] extensions]
      (let [p (comp-paths* apath)
            rp (assoc p :transform-fns (coerce-tfns-rich (:transform-fns p)))
            needed-params (num-needed-params rp)]
        (if-not (= needed-params expected-params)
          (throw-illegal "Invalid number of params in extended protocol path, expected "
              expected-params " but got " needed-params))
        (extend atype protpath-prot {m (fn [_] rp)})
        ))))
