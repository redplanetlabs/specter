(ns com.rpl.specter.impl
  #+cljs (:require-macros
            [com.rpl.specter.prot-opt-invoke
              :refer [mk-optimized-invocation]]
            [com.rpl.specter.defhelpers :refer [define-ParamsNeededPath]]
            [com.rpl.specter.util-macros :refer [doseqres]]
            )
  (:use [com.rpl.specter.protocols :only
          [select* transform* collect-val]]
        #+clj [com.rpl.specter.util-macros :only [doseqres]]
)
  (:require [com.rpl.specter.protocols :as p]
            [clojure.walk :as walk]
            #+clj [clojure.core.reducers :as r]
            [clojure.string :as s]
            #+clj [com.rpl.specter.defhelpers :as dh]
            #+clj [riddley.walk :as riddley]
            )
  #+clj
  (:import [com.rpl.specter Util MutableCell])
  )

(def NONE ::NONE)

(defn spy [e]
  (println "SPY:")
  (println (pr-str e))
  e)

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
  (throw (js/Error. (apply str args))))

;; need to get the expansion function like this so that 
;; this code compiles in a clojure environment where cljs.analyzer
;; namespace does not exist
#+clj
(defn cljs-analyzer-macroexpand-1 []
  (eval 'cljs.analyzer/macroexpand-1))

;; this version is for bootstrap cljs
#+cljs
(defn cljs-analyzer-macroexpand-1 []
  ^:cljs.analyzer/no-resolve cljs.analyzer/macroexpand-1)

#+clj
(defn clj-macroexpand-all [form]
  (riddley/macroexpand-all form))

#+cljs
(defn clj-macroexpand-all [form]
  (throw-illegal "not implemented"))

#+clj
(defn intern* [ns name val] (intern ns name val))

#+cljs
(defn intern* [ns name val]
  (throw-illegal "intern not supported in ClojureScript"))

;; so that macros.clj compiles appropriately when
;; run in cljs (this code isn't called in that case)
#+cljs
(defn gen-uuid-str []
  (throw-illegal "Cannot get UUID in Javascript"))

#+clj
(defn gen-uuid-str []
  (str (java.util.UUID/randomUUID)))

(defn benchmark [iters afn]
  (time
   (dotimes [_ iters]
     (afn))))

(deftype ExecutorFunctions [type traverse-executor transform-executor])

(def RichPathExecutor
  (->ExecutorFunctions
    :richpath
    (fn [params params-idx selector result-fn structure]
      (selector params params-idx [] structure
        (fn [_ _ vals structure]
          (result-fn
            (if (identical? vals [])
              structure
              (conj vals structure))))))
    (fn [params params-idx transformer transform-fn structure]
      (transformer params params-idx [] structure
        (fn [_ _ vals structure]
          (if (identical? [] vals)
            (transform-fn structure)
            (apply transform-fn (conj vals structure))))))
    ))

(def LeanPathExecutor
  (->ExecutorFunctions
    :leanpath
    (fn [params params-idx selector result-fn structure]
      (selector structure result-fn))
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

(defprotocol PathComposer
  (do-comp-paths [paths]))

(defn comp-paths* [p]
  (if (compiled-path? p) p (do-comp-paths p))
  )

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
    (do-comp-paths this))

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
  (do-comp-paths [sp]
    (coerce-path sp))
  #+clj Object #+cljs default
  (do-comp-paths [sp]
    (coerce-path sp))
  #+clj java.util.List #+cljs cljs.core/PersistentVector
  (do-comp-paths [structure-paths]
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
#+cljs
(defprotocol PMutableCell
  (set_cell [cell x]))

#+cljs
(deftype MutableCell [^:volatile-mutable q]
  PMutableCell
  (set_cell [this x] (set! q x)))

#+cljs
(defn mutable-cell
  ([] (mutable-cell nil))
  ([init] (MutableCell. init)))

#+cljs
(defn set-cell! [cell val]
  (set_cell cell val))

#+cljs
(defn get-cell [cell]
  #+clj (get_cell cell) #+cljs (.-q cell)
  )


#+clj
(defn mutable-cell
  ([] (mutable-cell nil))
  ([v] (MutableCell. v)))

#+clj
(defn get-cell [^MutableCell c]
  (.get c))

#+clj
(defn set-cell! [^MutableCell c v]
  (.set c v))


(defn update-cell! [cell afn]
  (let [ret (afn (get-cell cell))]
    (set-cell! cell ret)
    ret))

(defn- append [coll elem]
  (-> coll vec (conj elem)))

(defprotocol AddExtremes
  (append-all [structure elements])
  (prepend-all [structure elements]))

(extend-protocol AddExtremes
  nil
  (append-all [_ elements]
    elements)
  (prepend-all [_ elements]
    elements)

  #+clj clojure.lang.PersistentVector #+cljs cljs.core/PersistentVector
  (append-all [structure elements]
    (reduce conj structure elements))
  (prepend-all [structure elements]
    (let [ret (transient [])]
      (as-> ret <>
            (reduce conj! <> elements)
            (reduce conj! <> structure)
            (persistent! <>)
            )))

  #+clj Object #+cljs default
  (append-all [structure elements]
    (concat structure elements))
  (prepend-all [structure elements]
    (concat elements structure))
  )


(defprotocol UpdateExtremes
  (update-first [s afn])
  (update-last [s afn]))

(defprotocol GetExtremes
  (get-first [s])
  (get-last [s]))

(defprotocol FastEmpty
  (fast-empty? [s]))

(defn- update-first-list [l afn]
  (cons (afn (first l)) (rest l)))

(defn- update-last-list [l afn]
  (append (butlast l) (afn (last l))))

#+clj
(defn vec-count [^clojure.lang.IPersistentVector v]
  (.length v))

#+cljs
(defn vec-count [v]
  (count v))

#+clj
(defn transient-vec-count [^clojure.lang.ITransientVector v]
  (.count v))

#+cljs
(defn transient-vec-count [v]
  (count v))

(extend-protocol UpdateExtremes
  #+clj clojure.lang.PersistentVector #+cljs cljs.core/PersistentVector
  (update-first [v afn]
    (let [val (nth v 0)]
      (assoc v 0 (afn val))
      ))
  (update-last [v afn]
    ;; type-hinting vec-count to ^int caused weird errors with case
    (let [c (int (vec-count v))]
      (case c
        1 (let [[e] v] [(afn e)])
        2 (let [[e1 e2] v] [e1 (afn e2)])
        (let [i (dec c)]
          (assoc v i (afn (nth v i)))
          ))))
  #+clj Object #+cljs default
  (update-first [l val]
    (update-first-list l val))
  (update-last [l val]
    (update-last-list l val)
    ))

(extend-protocol GetExtremes
  #+clj clojure.lang.IPersistentVector #+cljs cljs.core/PersistentVector
  (get-first [v]
    (nth v 0))
  (get-last [v]
    (peek v))
  #+clj Object #+cljs default
  (get-first [s]
    (first s))
  (get-last [s]
    (last s)
    ))


(extend-protocol FastEmpty
  nil
  (fast-empty? [_] true)

  #+clj clojure.lang.IPersistentVector #+cljs cljs.core/PersistentVector
  (fast-empty? [v]
    (= 0 (vec-count v)))
  #+clj clojure.lang.ITransientVector #+cljs cljs.core/TransientVector
  (fast-empty? [v]
    (= 0 (transient-vec-count v)))
  #+clj Object #+cljs default
  (fast-empty? [s]
    (empty? s))
  )

(defn walk-until [pred on-match-fn structure]
  (if (pred structure)
    (on-match-fn structure)
    (walk/walk (partial walk-until pred on-match-fn) identity structure)
    ))

(defn fn-invocation? [f]
  (or #+clj  (instance? clojure.lang.Cons f)
      #+clj  (instance? clojure.lang.LazySeq f)
      #+cljs (instance? cljs.core.LazySeq f)
      (list? f)))

(defn codewalk-until [pred on-match-fn structure]
  (if (pred structure)
    (on-match-fn structure)
    (let [ret (walk/walk (partial codewalk-until pred on-match-fn) identity structure)]
      (if (and (fn-invocation? structure) (fn-invocation? ret))
        (with-meta ret (meta structure))
        ret
        ))))


(def collected?*
  (->ParamsNeededPath
    (->TransformFunctions
      RichPathExecutor
      (fn [params params-idx vals structure next-fn]
        (let [afn (aget ^objects params params-idx)]
          (if (afn vals)
            (next-fn params (inc params-idx) vals structure)
            NONE
            )))
      (fn [params params-idx vals structure next-fn]
        (let [afn (aget ^objects params params-idx)]
          (if (afn vals)
            (next-fn params (inc params-idx) vals structure)
            structure
            ))))
    1
    ))

(def DISPENSE*
  (no-params-compiled-path
    (->TransformFunctions
      RichPathExecutor
      (fn [params params-idx vals structure next-fn]
        (next-fn params params-idx [] structure))
      (fn [params params-idx vals structure next-fn]
        (next-fn params params-idx [] structure))
      )))

(defn transform-fns-field [^CompiledPath path]
  (.-transform-fns path))

(defn executors-field [^TransformFunctions tfns]
  (.-executors tfns))

(defn traverse-executor-field [^ExecutorFunctions ex]
  (.-traverse-executor ex))

(defn params-field [^CompiledPath path]
  (.-params path))

(defn params-idx-field [^CompiledPath path]
  (.-params-idx path))

(defn selector-field [^TransformFunctions tfns]
  (.-selector tfns))

;; amazingly doing this as a macro shows a big effect in the 
;; benchmark for getting a value out of a nested map
#+clj
(defmacro compiled-traverse* [path result-fn structure]
  `(let [tfns# (transform-fns-field ~path)
         ex# (executors-field tfns#)]
    ((traverse-executor-field ex#)
      (params-field ~path)
      (params-idx-field ~path)
      (selector-field tfns#)
      ~result-fn
      ~structure)
    ))

#+cljs
(defn compiled-traverse* [path result-fn structure]
  (let [tfns (transform-fns-field path)
        ex (executors-field tfns)]
    ((traverse-executor-field ex)
      (params-field path)
      (params-idx-field path)
      (selector-field tfns)
      result-fn
      structure)
    ))

(defn do-compiled-traverse [apath structure]
  (reify #+clj clojure.lang.IReduce #+cljs cljs.core/IReduce
    (#+clj reduce #+cljs -reduce
      [this afn]
      (#+clj .reduce #+cljs -reduce this afn (afn)))
    (#+clj reduce #+cljs -reduce
      [this afn start]
      (let [cell (mutable-cell start)]
        (compiled-traverse*
          apath
          (fn [elem]
            (let [curr (get-cell cell)]
              (set-cell! cell (afn curr elem))
              ))
          structure
          )
        (get-cell cell)
        ))))

(defn compiled-select* [path structure]
  (let [res (mutable-cell (transient []))
        result-fn (fn [structure]
                    (let [curr (get-cell res)]
                      (set-cell! res (conj! curr structure))
                      ))]
    (compiled-traverse* path result-fn structure)
    (persistent! (get-cell res))
    ))

(defn compiled-select-one* [path structure]
  (let [res (mutable-cell NONE)
        result-fn (fn [structure]
                    (let [curr (get-cell res)]
                      (if (identical? curr NONE)
                        (set-cell! res structure)
                        (throw-illegal "More than one element found in structure: " structure)
                        )))]
    (compiled-traverse* path result-fn structure)
    (let [ret (get-cell res)]
      (if (identical? ret NONE)
        nil
        ret
        ))))

(defn compiled-select-one!* [path structure]
  (let [res (mutable-cell NONE)
        result-fn (fn [structure]
                    (let [curr (get-cell res)]
                      (if (identical? curr NONE)
                        (set-cell! res structure)
                        (throw-illegal "More than one element found in structure: " structure)
                        )))]
    (compiled-traverse* path result-fn structure)
    (let [ret (get-cell res)]
      (if (identical? NONE ret)
        (throw-illegal "Found no elements for select-one! on " structure))
      ret
      )))

(defn compiled-select-first* [path structure]
  (let [res (mutable-cell NONE)
        result-fn (fn [structure]
                    (let [curr (get-cell res)]
                      (if (identical? curr NONE)
                        (set-cell! res structure))))]
    (compiled-traverse* path result-fn structure)
    (let [ret (get-cell res)]
      (if (identical? ret NONE)
        nil
        ret
        ))))

(defn compiled-select-any* [path structure]
  (compiled-traverse* path identity structure))

(defn compiled-selected-any?* [path structure]
  (not= NONE (compiled-select-any* path structure)))

(defn compiled-transform*
  [^com.rpl.specter.impl.CompiledPath path transform-fn structure]
  (let [^com.rpl.specter.impl.TransformFunctions tfns (.-transform-fns path)
        ^com.rpl.specter.impl.ExecutorFunctions ex (.-executors tfns)]
    ((.-transform-executor ex) (.-params path) (.-params-idx path) (.-transformer tfns) transform-fn structure)
    ))

(defn not-selected?*
  [compiled-path structure]
  (->> structure
       (compiled-select-any* compiled-path)
       (identical? NONE)))

(defn selected?*
  [compiled-path structure]
  (not (not-selected?* compiled-path structure)))

(defn walk-select [pred continue-fn structure]
  (let [ret (mutable-cell NONE)
        walker (fn this [structure]
                 (if (pred structure)
                   (let [r (continue-fn structure)]
                     (if-not (identical? r NONE)
                       (set-cell! ret r))
                     r
                     )
                   (walk/walk this identity structure)
                   ))]
    (walker structure)
    (get-cell ret)
    ))

(defn key-select [akey structure next-fn]
  (next-fn (get structure akey)))

(defn key-transform [akey structure next-fn]
  (assoc structure akey (next-fn (get structure akey))
  ))

(defn all-select [structure next-fn]
  (doseqres NONE [e structure]
    (next-fn e)))

#+cljs
(defn queue? [coll]
  (= (type coll) (type #queue [])))

#+clj
(defn queue? [coll]
  (instance? clojure.lang.PersistentQueue coll))

(defprotocol AllTransformProtocol
  (all-transform [structure next-fn]))

(defn- non-transient-map-all-transform [structure next-fn empty-map]
  (reduce-kv
    (fn [m k v]
      (let [[newk newv] (next-fn [k v])]
        (assoc m newk newv)
        ))
    empty-map
    structure
    ))

(extend-protocol AllTransformProtocol
  nil
  (all-transform [structure next-fn]
    nil
    )

  ;; in cljs they're PersistentVector so don't need a special case
  #+clj clojure.lang.MapEntry
  #+clj
  (all-transform [structure next-fn]
    (let [newk (next-fn (key structure))
          newv (next-fn (val structure))]
      (clojure.lang.MapEntry. newk newv)
      ))

  #+clj clojure.lang.PersistentVector #+cljs cljs.core/PersistentVector
  (all-transform [structure next-fn]
    (mapv next-fn structure))

  #+clj
  clojure.lang.PersistentArrayMap
  #+clj
  (all-transform [structure next-fn]
    (let [k-it (.keyIterator structure)
          v-it (.valIterator structure)]
      (loop [ret {}]
        (if (.hasNext k-it)
          (let [k (.next k-it)
                v (.next v-it)
                [newk newv] (next-fn [k v])]
            (recur (assoc ret newk newv)))
          ret
          ))))

  #+cljs
  cljs.core/PersistentArrayMap
  #+cljs
  (all-transform [structure next-fn]
    (non-transient-map-all-transform structure next-fn {})
    )

  #+clj clojure.lang.PersistentTreeMap #+cljs cljs.core/PersistentTreeMap
  (all-transform [structure next-fn]
    (non-transient-map-all-transform structure next-fn (sorted-map))
    )

  #+clj clojure.lang.PersistentHashMap #+cljs cljs.core/PersistentHashMap
  (all-transform [structure next-fn]
    (persistent!
      (reduce-kv
        (fn [m k v]
          (let [[newk newv] (next-fn [k v])]
            (assoc! m newk newv)
            ))
        (transient
          #+clj clojure.lang.PersistentHashMap/EMPTY #+cljs cljs.core.PersistentHashMap.EMPTY
          )
        structure
        )))


  #+clj
  Object
  #+clj
  (all-transform [structure next-fn]
    (let [empty-structure (empty structure)]
      (cond (and (list? empty-structure) (not (queue? empty-structure)))
            ;; this is done to maintain order, otherwise lists get reversed
            (doall (map next-fn structure))

            (map? structure)
            ;; reduce-kv is much faster than doing r/map through call to (into ...)
            (reduce-kv
              (fn [m k v]
                (let [[newk newv] (next-fn [k v])]
                  (assoc m newk newv)
                  ))
              empty-structure
              structure
              )

            :else
            (->> structure (r/map next-fn) (into empty-structure))
        )))

  #+cljs
  default
  #+cljs 
  (all-transform [structure next-fn]
    (let [empty-structure (empty structure)]
      (if (and (list? empty-structure) (not (queue? empty-structure)))
        ;; this is done to maintain order, otherwise lists get reversed
        (doall (map next-fn structure))
        (into empty-structure (map #(next-fn %)) structure)
        )))
  )

(deftype AllNavigator [])

(extend-protocol p/Navigator
  AllNavigator
  (select* [this structure next-fn]
    (all-select structure next-fn))
  (transform* [this structure next-fn]
    (all-transform structure next-fn)))

(defprotocol MapValsTransformProtocol
  (map-vals-transform [structure next-fn]))

(defn map-vals-non-transient-transform [structure empty-map next-fn]
  (reduce-kv
    (fn [m k v]
      (assoc m k (next-fn v)))
    empty-map
    structure))

(extend-protocol MapValsTransformProtocol
  nil
  (map-vals-transform [structure next-fn]
    nil
    )

  #+clj
  clojure.lang.PersistentArrayMap
  #+clj
  (map-vals-transform [structure next-fn]
    (let [k-it (.keyIterator structure)
          v-it (.valIterator structure)]
      (loop [ret {}]
        (if (.hasNext k-it)
          (let [k (.next k-it)
                v (.next v-it)]
            (recur (assoc ret k (next-fn v))))
          ret
          ))))

  #+cljs
  cljs.core/PersistentArrayMap
  #+cljs
  (map-vals-transform [structure next-fn]
    (map-vals-non-transient-transform structure {} next-fn)
    )

  #+clj clojure.lang.PersistentTreeMap #+cljs cljs.core/PersistentTreeMap
  (map-vals-transform [structure next-fn]
    (map-vals-non-transient-transform structure (sorted-map) next-fn)
    )

  #+clj clojure.lang.PersistentHashMap #+cljs cljs.core/PersistentHashMap
  (map-vals-transform [structure next-fn]
    (persistent!
      (reduce-kv
        (fn [m k v]
          (assoc! m k (next-fn v)))
        (transient
          #+clj clojure.lang.PersistentHashMap/EMPTY #+cljs cljs.core.PersistentHashMap.EMPTY
          )
        structure
        )))

  #+clj Object #+cljs default
  (map-vals-transform [structure next-fn]
    (reduce-kv
      (fn [m k v]
        (assoc m k (next-fn v)))
      (empty structure)
      structure))
  )


(deftype ValCollect [])

(extend-protocol p/Collector
  ValCollect
  (collect-val [this structure]
    structure))

(deftype PosNavigator [getter updater])

(extend-protocol p/Navigator
  PosNavigator
  (select* [this structure next-fn]
    (if-not (fast-empty? structure)
      (next-fn ((.-getter this) structure))
      NONE))
  (transform* [this structure next-fn]
    (if (fast-empty? structure)
      structure
      ((.-updater this) structure next-fn))))

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

(defn- matching-indices [aseq p]
  (keep-indexed (fn [i e] (if (p e) i)) aseq))

(defn matching-ranges [aseq p]
  (first
    (reduce
      (fn [[ranges curr-start curr-last :as curr] i]
        (cond
          (nil? curr-start)
          [ranges i i]

          (= i (inc curr-last))
          [ranges curr-start i]

          :else
          [(conj ranges [curr-start (inc curr-last)]) i i]
          ))
      [[] nil nil]
      (concat (matching-indices aseq p) [-1])
    )))

(extend-protocol p/Navigator
  nil
  (select* [this structure next-fn]
    (next-fn structure))
  (transform* [this structure next-fn]
    (next-fn structure)
    ))

(deftype TransientEndNavigator [])

(extend-protocol p/Navigator
  TransientEndNavigator
  (select* [this structure next-fn]
    (next-fn []))
  (transform* [this structure next-fn]
    (let [res (next-fn [])]
      (reduce conj! structure res))))

(defn extract-basic-filter-fn [path]
  (cond (fn? path)
        path

        (and (coll? path)
             (every? fn? path))
        (reduce
          (fn [combined afn]
            (fn [structure]
              (and (combined structure) (afn structure))
              ))
          path
          )))

(defn if-select [structure next-fn then-tester late-then late-else]
  (let [apath (if (then-tester structure)
                late-then
                late-else)]
    (compiled-traverse* apath next-fn structure)))

(defn if-transform [structure next-fn then-tester late-then late-else]
  (let [apath (if (then-tester structure)
                late-then
                late-else)]
    (compiled-transform* apath next-fn structure)
    ))

(defn filter-select [afn structure next-fn]
  (if (afn structure)
    (next-fn structure)
    NONE))

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

(defrecord LayeredNav [underlying])

(defn layered-nav? [o] (instance? LayeredNav o))

(defn layered-nav-underlying [^LayeredNav ln]
  (.-underlying ln))

(defn verify-layerable! [anav]
  (if-not
    (and (instance? ParamsNeededPath anav)
         (> (:num-needed-params anav) 0))
    (throw-illegal "defnavconstructor must be used on a navigator defined with
      defnav with at least one parameter")
    ))

(defn layered-wrapper [anav]
  (verify-layerable! anav)
  (fn ([a1] (->LayeredNav (anav a1)))
      ([a1 a2] (->LayeredNav (anav a1 a2)))
      ([a1 a2 a3] (->LayeredNav (anav a1 a2 a3)))
      ([a1 a2 a3 a4] (->LayeredNav (anav a1 a2 a3 a4)))
      ([a1 a2 a3 a4 a5] (->LayeredNav (anav a1 a2 a3 a4 a5)))
      ([a1 a2 a3 a4 a5 a6] (->LayeredNav (anav a1 a2 a3 a4 a5 a6)))
      ([a1 a2 a3 a4 a5 a6 a7] (->LayeredNav (anav a1 a2 a3 a4 a5 a6 a7)))
      ([a1 a2 a3 a4 a5 a6 a7 a8] (->LayeredNav (anav a1 a2 a3 a4 a5 a6 a7 a8)))
      ([a1 a2 a3 a4 a5 a6 a7 a8 a9] (->LayeredNav (anav a1 a2 a3 a4 a5 a6 a7 a8 a9)))
      ([a1 a2 a3 a4 a5 a6 a7 a8 a9 a10] (->LayeredNav (anav a1 a2 a3 a4 a5 a6 a7 a8 a9 a10)))
      ([a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 & args]
        (->LayeredNav (apply anav a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 args)))
    ))

(defrecord LocalSym
  [val sym])

(defrecord VarUse
  [val var sym])

(defrecord SpecialFormUse
  [val code])

(defrecord FnInvocation
  ;; op and params elems can be any of the above
  [op params code])

(defrecord CachedPathInfo
  [precompiled ; can be null
   params-maker ; can be null
   ])

(def MUST-CACHE-PATHS (mutable-cell false))

(defn must-cache-paths!
  ([] (must-cache-paths! true))
  ([v] (set-cell! MUST-CACHE-PATHS v)))

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

#+cljs
(defn handle-params [precompiled params-maker possible-params]
  (let [params (fast-object-array (count params-maker))]
    (dotimes [i (count params-maker)]
      (aset params i ((get possible-params (get params-maker i)))))
    (bind-params* precompiled params 0)
    ))

(def pred*
  (->ParamsNeededPath
    (->TransformFunctions
      RichPathExecutor
      (fn [params params-idx vals structure next-fn]
        (let [afn (aget ^objects params params-idx)]
          (if (afn structure)
            (next-fn params (inc params-idx) vals structure)
            NONE
            )))
      (fn [params params-idx vals structure next-fn]
        (let [afn (aget ^objects params params-idx)]
          (if (afn structure)
            (next-fn params (inc params-idx) vals structure)
            structure
            ))))
    1
    ))

(def rich-compiled-path-proxy
  (->ParamsNeededPath
    (->TransformFunctions
      RichPathExecutor
      (fn [params params-idx vals structure next-fn]
        (let [apath ^CompiledPath (aget ^objects params params-idx)
              transform-fns ^TransformFunctions (.-transform-fns apath)
              selector (.-selector transform-fns)]
          (selector
            (.-params apath)
            (.-params-idx apath)
            vals
            structure
            (fn [_ _ vals-next structure-next]
              (next-fn params params-idx vals-next structure-next))
            )))
      (fn [params params-idx vals structure next-fn]
        (let [apath ^CompiledPath (aget ^objects params params-idx)
              transform-fns ^TransformFunctions (.-transform-fns apath)
              transformer (.-transformer transform-fns)]
          (transformer
            (.-params apath)
            (.-params-idx apath)
            vals
            structure
            (fn [_ _ vals-next structure-next]
              (next-fn params params-idx vals-next structure-next))
            ))))
    1
    ))

(defn- variadic-arglist? [al]
  (contains? (set al) '&))

(defn- arglist-for-params-count [arglists c code]
  (let [ret (->> arglists
                 (filter
                   (fn [al]
                     (or (= (count al) c)
                         (variadic-arglist? al))
                     ))
                 first)
        len (count ret)]
    (when-not ret
      (throw-illegal "Invalid # arguments at " code))
    (if (variadic-arglist? ret)
      (srange-transform ret (- len 2) len
        (fn [_] (repeatedly (- c (- len 2)) gensym)))
      ret
      )))

(defn- magic-precompilation* [p params-atom failed-atom]
  (let [magic-fail! (fn [& reason]
                      (if (get-cell MUST-CACHE-PATHS)
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
            vv (:val p)]
        (cond (-> v meta :dynamic)
              (magic-fail! "Var " (:sym p) " is dynamic")

              (and (fn? vv) (-> v meta :pathedfn))
              (throw-illegal "Cannot use pathed fn '" (:sym p) "' where navigator expected")

              (valid-navigator? vv)
              vv

              :else
              (magic-fail! "Var " (:sym p) " is not a navigator")
              ))

      (instance? SpecialFormUse p)
      (if (->> p :code first (contains? #{'fn* 'fn}))
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
                vv (:val op)]
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

                (and (fn? vv) (-> v meta :pathedfn))
                ;;TODO: update this to ignore args that aren't symbols or have :nopath
                ;;metadata on them (in the arglist)
                (let [arglists (-> v meta :arglists)
                      al (arglist-for-params-count arglists (count ps) (:code p))
                      subpath (vec
                                (map
                                  (fn [pdecl p]
                                   (if (and (symbol? pdecl)
                                            (-> pdecl meta :notpath not))
                                     (magic-precompilation* p params-atom failed-atom)

                                     (cond (and (instance? VarUse p)
                                                (-> p :var meta :dynamic not))
                                           (:val p)

                                           (and (not (instance? LocalSym p))
                                                (not (instance? VarUse p))
                                                (not (instance? SpecialFormUse p))
                                                (not (instance? FnInvocation p))
                                                (not (coll? p)))
                                           p

                                           :else
                                           (magic-fail! "Could not factor static param "
                                            "of pathedfn because it's not a static var "
                                            " or non-collection value: "
                                            (extract-original-code p))
                                       )))
                                   al
                                   ps))]
                  (if @failed-atom
                    nil
                    (apply vv subpath)
                    ))

                (and (fn? vv) (-> vv meta :layerednav))
                (do
                  (swap! params-atom conj (:code p))
                  rich-compiled-path-proxy
                  )

                :else
                (magic-fail! "Var " (:sym op) " must be either a parameterized "
                  "navigator, a higher order pathed constructor function, "
                  "or a nav constructor")
                )))
          (magic-fail! "Code at " (extract-original-code p) " is in "
            "function invocation position and must be either a parameterized "
            "navigator, a higher order pathed constructor function, or a "
            "nav constructor."
            )
          ))

      :else
      (cond (set? p)
            (do (swap! params-atom conj p)
                pred*)

            (keyword? p)
            p

            ;; in case anyone extends String for their own use case
            (and (string? p) (valid-navigator? p))
            p

            :else
            (magic-fail! "Code " p " is not a valid navigator or can't be factored")
            )
      )))


;; This is needed when aset is used on primitive values in mk-params-maker
;; to avoid reflection
#+clj
(defn aset-object [^objects a i ^Object v]
  (aset a i v))

#+clj
(defn mk-params-maker [ns-str params-code possible-params-code used-locals]
  (let [ns (find-ns (symbol ns-str))
        array-sym (gensym "array")]
    (binding [*ns* ns]
      (eval
        `(fn [~@used-locals]
           (let [~array-sym (fast-object-array ~(count params-code))]
             ~@(map-indexed
                 (fn [i c]
                  `(aset-object ~array-sym ~i ~c))
                 params-code
                 )
             ~array-sym
             ))))))

#+cljs
(defn mk-params-maker [ns-str params-code possible-params-code used-locals]
  (let [indexed (->> possible-params-code
                     (map-indexed (comp vec reverse vector))
                     (into {}))]
    ;;TODO: may be more efficient as an array
    (mapv (fn [c] (get indexed c)) params-code)))

;; possible-params-code is for cljs impl that can't use eval
(defn magic-precompilation [prepared-path ns-str used-locals possible-params-code]
  (let [params-atom (atom [])
        failed-atom (atom false)
        path (magic-precompilation* prepared-path params-atom failed-atom)
        ]
    (if @failed-atom
      (if (get-cell MUST-CACHE-PATHS)
        (throw-illegal "Failed to cache path")
        (->CachedPathInfo nil nil))
      (let [precompiled (comp-paths* path)
            params-code (mapv extract-original-code @params-atom)
            params-maker (if-not (empty? params-code)
                           (mk-params-maker ns-str params-code possible-params-code used-locals))
            ]
        ;; TODO: error if precompiled is compiledpath and there are params or
        ;; precompiled is paramsneededpath and there are no params...
        (->CachedPathInfo precompiled params-maker)
        ))
    ))



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
