(ns com.rpl.specter.impl
  #?(:cljs (:require-macros
            [com.rpl.specter.defhelpers :refer [define-ParamsNeededPath]]
            [com.rpl.specter.util-macros :refer [doseqres definterface+]]))

  (:use [com.rpl.specter.protocols :only
          [select* transform* collect-val Navigator]]
        #?(:clj [com.rpl.specter.util-macros :only [doseqres definterface+]]))

  (:require [com.rpl.specter.protocols :as p]
            [clojure.string :as s]
            [clojure.walk :as walk]
            #?(:clj [com.rpl.specter.defhelpers :as dh])
            #?(:clj [riddley.walk :as riddley]))

  #?(:clj (:import [com.rpl.specter Util MutableCell])))


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

(defn object-aget [^objects a i]
  (aget a i))

(defn fast-constantly [v]
  (fn ([] v)
      ([a1] v)
      ([a1 a2] v)
      ([a1 a2 a3] v)
      ([a1 a2 a3 a4] v)
      ([a1 a2 a3 a4 a5] v)
      ([a1 a2 a3 a4 a5 a6] v)
      ([a1 a2 a3 a4 a5 a6 a7] v)
      ([a1 a2 a3 a4 a5 a6 a7 a8] v)
      ([a1 a2 a3 a4 a5 a6 a7 a8 a9] v)
      ([a1 a2 a3 a4 a5 a6 a7 a8 a9 a10] v)
      ([a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 & r] v)))


#?(:clj
   (defmacro throw* [etype & args]
     `(throw (new ~etype (smart-str ~@args)))))

#?(
   :clj
   (defmacro throw-illegal [& args]
     `(throw* IllegalArgumentException ~@args))


   :cljs
   (defn throw-illegal [& args]
     (throw (js/Error. (apply str args)))))


;; need to get the expansion function like this so that
;; this code compiles in a clojure environment where cljs.analyzer
;; namespace does not exist
#?(
   :clj
   (defn cljs-analyzer-macroexpand-1 []
     (eval 'cljs.analyzer/macroexpand-1))

;; this version is for bootstrap cljs
   :cljs
   (defn cljs-analyzer-macroexpand-1 []
     ^:cljs.analyzer/no-resolve cljs.analyzer/macroexpand-1))


#?(
   :clj
   (defn clj-macroexpand-all [form]
     (riddley/macroexpand-all form))

   :cljs
   (defn clj-macroexpand-all [form]
     (throw-illegal "not implemented")))


#?(
   :clj
   (defn intern* [ns name val] (intern ns name val))

   :cljs
   (defn intern* [ns name val]
     (throw-illegal "intern not supported in ClojureScript")))


(defn benchmark [iters afn]
  (time
   (dotimes [_ iters]
     (afn))))

(deftype ExecutorFunctions [traverse-executor transform-executor])

(deftype ParameterizedRichNav [rich-nav params ^long params-idx])

(definterface+ RichNavigator
  (rich_select [this params ^long params-idx vals structure next-fn])
  (rich_transform [this params ^long params-idx vals structure next-fn]))


#?(
   :clj
   (defmacro exec-rich_select [this & args]
     (let [hinted (with-meta this {:tag 'com.rpl.specter.impl.RichNavigator})]
       `(.rich_select ~hinted ~@args)))


   :cljs
   (defn exec-rich_select [this params params-idx vals structure next-fn]
     (rich_select ^not-native this params params-idx vals structure next-fn)))


#?(
   :clj
   (defmacro exec-rich_transform [this & args]
     (let [hinted (with-meta this {:tag 'com.rpl.specter.impl.RichNavigator})]
       `(.rich_transform ~hinted ~@args)))


   :cljs
   (defn exec-rich_transform [this params params-idx vals structure next-fn]
     (rich_transform ^not-native this params params-idx vals structure next-fn)))


#?(
   :clj
   (defmacro exec-select* [this & args]
     (let [hinted (with-meta this {:tag 'com.rpl.specter.protocols.Navigator})]
       `(.select* ~hinted ~@args)))


   :cljs
   (defn exec-select* [this structure next-fn]
     (p/select* ^not-native this structure next-fn)))


#?(
   :clj
   (defmacro exec-transform* [this & args]
     (let [hinted (with-meta this {:tag 'com.rpl.specter.protocols.Navigator})]
       `(.transform* ~hinted ~@args)))


   :cljs
   (defn exec-transform* [this structure next-fn]
     (p/transform* ^not-native this structure next-fn)))


(def RichPathExecutor
  (->ExecutorFunctions
    (fn [^ParameterizedRichNav richnavp result-fn structure]
      (exec-rich_select (.-rich-nav richnavp)
        (.-params richnavp) (.-params-idx richnavp)
        [] structure
        (fn [_ _ vals structure]
          (result-fn
            (if (identical? vals [])
              structure
              (conj vals structure))))))
    (fn [^ParameterizedRichNav richnavp transform-fn structure]
      (exec-rich_transform (.-rich-nav richnavp)
        (.-params richnavp) (.-params-idx richnavp)
        [] structure
        (fn [_ _ vals structure]
          (if (identical? [] vals)
            (transform-fn structure)
            (apply transform-fn (conj vals structure))))))))


(def LeanPathExecutor
  (->ExecutorFunctions
    (fn [nav result-fn structure]
      (exec-select* nav structure result-fn))
    (fn [nav transform-fn structure]
      (exec-transform* nav structure transform-fn))))


(defrecord CompiledPath [executors nav])

(defn compiled-path? [o]
  (instance? CompiledPath o))

(defn no-params-rich-compiled-path [rich-nav]
  (->CompiledPath
    RichPathExecutor
    (->ParameterizedRichNav
      rich-nav
      nil
      0)))


(defn lean-compiled-path [nav]
  (->CompiledPath LeanPathExecutor nav))


(declare bind-params*)

#?(
   :clj
   (defmacro fast-object-array [i]
     `(com.rpl.specter.Util/makeObjectArray ~i))

   :cljs
   (defn fast-object-array [i]
     (object-array i)))


#?(
   :clj
   (dh/define-ParamsNeededPath
     true
     clojure.lang.IFn
     invoke
     (applyTo [this args]
       (let [a (object-array args)]
         (com.rpl.specter.impl/bind-params* this a 0))))

   :cljs
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
         (com.rpl.specter.impl/bind-params* this a 0)))))



(defn params-needed-path? [o]
  (instance? ParamsNeededPath o))

(defn extract-nav [p]
  (if (params-needed-path? p)
    (.-rich-nav ^ParamsNeededPath p)
    (let [n (.-nav ^CompiledPath p)]
      (if (instance? ParameterizedRichNav n)
        (.-rich-nav ^ParameterizedRichNav n)
        n))))



(defn bind-params* [^ParamsNeededPath params-needed-path params idx]
  (->CompiledPath
    RichPathExecutor
    (->ParameterizedRichNav
      (.-rich-nav params-needed-path)
      params
      idx)))


(defprotocol PathComposer
  (do-comp-paths [paths]))

(defn comp-paths* [p]
  (if (compiled-path? p) p (do-comp-paths p)))


(defn- seq-contains? [aseq val]
  (->> aseq
       (filter (partial = val))
       empty?
       not))

(defn root-params-nav? [o]
  (and (fn? o) (-> o meta :highernav)))

(defn- coerce-object [this]
  (cond (root-params-nav? this) (-> this meta :highernav :params-needed-path)
        (satisfies? p/ImplicitNav this) (p/implicit-nav this)
        :else (throw-illegal "Not a navigator: " this)))


(defprotocol CoercePath
  (coerce-path [this]))

(extend-protocol CoercePath
  nil ; needs its own coercer because it doesn't count as an Object
  (coerce-path [this]
    (coerce-object this))

  CompiledPath
  (coerce-path [this]
    this)

  ParamsNeededPath
  (coerce-path [this]
    this)

  #?(:clj java.util.List :cljs cljs.core/PersistentVector)
  (coerce-path [this]
    (do-comp-paths this))

  #?(:cljs cljs.core/IndexedSeq)
  #?(:cljs (coerce-path [this]
            (coerce-path (vec this))))
  #?(:cljs cljs.core/EmptyList)
  #?(:cljs (coerce-path [this]
            (coerce-path (vec this))))
  #?(:cljs cljs.core/List)
  #?(:cljs (coerce-path [this]
            (coerce-path (vec this))))
  #?(:cljs cljs.core/LazySeq)
  #?(:cljs (coerce-path [this]
            (coerce-path (vec this))))

  #?(:clj Object :cljs default)
  (coerce-path [this]
    (coerce-object this)))

#?(:clj
   (defn rich-nav? [o]
     (instance? RichNavigator o))

   :cljs
   (defn rich-nav? [o]
     (satisfies? RichNavigator o)))


(defn- combine-same-types [[n & _ :as all]]
  (let [combiner
        (if (rich-nav? n)
          (fn [curr next]
            (reify RichNavigator
              (rich_select [this params params-idx vals structure next-fn]
                (exec-rich_select curr params params-idx vals structure
                  (fn [params-next params-idx-next vals-next structure-next]
                    (exec-rich_select next params-next params-idx-next
                      vals-next structure-next next-fn))))

              (rich_transform [this params params-idx vals structure next-fn]
                (exec-rich_transform curr params params-idx vals structure
                  (fn [params-next params-idx-next vals-next structure-next]
                    (exec-rich_transform next params-next params-idx-next
                      vals-next structure-next next-fn))))))

          (fn [curr next]
            (reify Navigator
              (select* [this structure next-fn]
                (exec-select* curr structure
                  (fn [structure-next]
                    (exec-select* next structure-next next-fn))))
              (transform* [this structure next-fn]
                (exec-transform* curr structure
                  (fn [structure-next]
                    (exec-transform* next structure-next next-fn)))))))]
    (reduce combiner all)))

(defn coerce-rich-navigator [nav]
  (if (rich-nav? nav)
    nav
    (reify RichNavigator
      (rich_select [this params params-idx vals structure next-fn]
        (exec-select* nav structure (fn [structure] (next-fn params params-idx vals structure))))

      (rich_transform [this params params-idx vals structure next-fn]
        (exec-transform* nav structure (fn [structure] (next-fn params params-idx vals structure)))))))


(defn extract-rich-nav [p]
  (coerce-rich-navigator (extract-nav p)))

(defn capture-params-internally [path]
  (cond
    (not (instance? CompiledPath path))
    path

    (satisfies? Navigator (:nav path))
    path

    :else
    (let [^ParameterizedRichNav prich-nav (:nav path)
          rich-nav (.-rich-nav prich-nav)
          params (.-params prich-nav)
          params-idx (.-params-idx prich-nav)]
      (if (empty? params)
        path
        (no-params-rich-compiled-path
          (reify RichNavigator
            (rich_select [this params2 params-idx2 vals structure next-fn]
              (exec-rich_select rich-nav params params-idx vals structure
                (fn [_ _ vals-next structure-next]
                  (next-fn params2 params-idx2 vals-next structure-next))))

            (rich_transform [this params2 params-idx2 vals structure next-fn]
              (exec-rich_transform rich-nav params params-idx vals structure
                (fn [_ _ vals-next structure-next]
                  (next-fn params2 params-idx2 vals-next structure-next))))))))))


(defn comp-paths-internalized [path]
  (capture-params-internally (comp-paths* path)))

(defn nav-type [n]
  (if (rich-nav? n)
    :rich
    :lean))

(extend-protocol PathComposer
  nil
  (do-comp-paths [o]
    (coerce-path o))
  #?(:clj Object :cljs default)
  (do-comp-paths [o]
    (coerce-path o))
  #?(:clj java.util.List :cljs cljs.core/PersistentVector)
  (do-comp-paths [navigators]
    (if (empty? navigators)
      (coerce-path nil)
      (let [coerced (->> navigators
                         (map coerce-path)
                         (map capture-params-internally))
            combined (->> coerced
                          (map extract-nav)
                          (partition-by nav-type)
                          (map combine-same-types))

            result-nav (if (= 1 (count combined))
                         (first combined)
                         (->> combined
                              (map coerce-rich-navigator)
                              combine-same-types))

            needs-params-paths (filter #(instance? ParamsNeededPath %) coerced)]
        (if (empty? needs-params-paths)
          (if (satisfies? Navigator result-nav)
            (lean-compiled-path result-nav)
            (no-params-rich-compiled-path result-nav))
          (->ParamsNeededPath
            (coerce-rich-navigator result-nav)
            (->> needs-params-paths
                 (map :num-needed-params)
                 (reduce +))))))))




(defn num-needed-params [path]
  (if (instance? CompiledPath path)
    0
    (.-num-needed-params ^ParamsNeededPath path)))


;; cell implementation idea taken from prismatic schema library
#?(:cljs
   (defprotocol PMutableCell
     (set_cell [cell x])))


#?(:cljs
   (deftype MutableCell [^:volatile-mutable q]
     PMutableCell
     (set_cell [this x] (set! q x))))


#?(
   :clj
   (defn mutable-cell
     ([] (mutable-cell nil))
     ([v] (MutableCell. v)))

   :cljs
   (defn mutable-cell
     ([] (mutable-cell nil))
     ([init] (MutableCell. init))))


#?(
   :clj
   (defn set-cell! [^MutableCell c v]
     (.set c v))

   :cljs
   (defn set-cell! [cell val]
     (set_cell cell val)))


#?(
   :clj
   (defn get-cell [^MutableCell c]
     (.get c))


   :cljs
   (defn get-cell [cell]
     (.-q cell)))




(defn update-cell! [cell afn]
  (let [ret (afn (get-cell cell))]
    (set-cell! cell ret)
    ret))



(defn compiled-nav-field [^CompiledPath p]
  (.-nav p))

(defn compiled-executors-field [^CompiledPath p]
  (.-executors p))


(defn traverse-executor-field [^ExecutorFunctions ex]
  (.-traverse-executor ex))


;; amazingly doing this as a macro shows a big effect in the
;; benchmark for getting a value out of a nested map
#?(
   :clj
   (defmacro compiled-traverse* [path result-fn structure]
     `(let [nav# (compiled-nav-field ~path)
            ex# (compiled-executors-field ~path)]
       ((traverse-executor-field ex#)
        nav#
         ~result-fn
         ~structure)))


   :cljs
   (defn compiled-traverse* [path result-fn structure]
     (let [nav (compiled-nav-field path)
           ex (compiled-executors-field path)]
       ((traverse-executor-field ex)
        nav
         result-fn
         structure))))



(defn do-compiled-traverse [apath structure]
  (reify #?(:clj clojure.lang.IReduce :cljs cljs.core/IReduce)
    (#?(:clj reduce :cljs -reduce)
      [this afn]
      (#?(:clj .reduce :cljs -reduce) this afn (afn)))
    (#?(:clj reduce :cljs -reduce)
      [this afn start]
      (let [cell (mutable-cell start)]
        (compiled-traverse*
          apath
          (fn [elem]
            (let [curr (get-cell cell)]
              (set-cell! cell (afn curr elem))))

          structure)

        (get-cell cell)))))


(defn compiled-select* [path structure]
  (let [res (mutable-cell (transient []))
        result-fn (fn [structure]
                    (let [curr (get-cell res)]
                      (set-cell! res (conj! curr structure))))]

    (compiled-traverse* path result-fn structure)
    (persistent! (get-cell res))))


(defn compiled-select-one* [path structure]
  (let [res (mutable-cell NONE)
        result-fn (fn [structure]
                    (let [curr (get-cell res)]
                      (if (identical? curr NONE)
                        (set-cell! res structure)
                        (throw-illegal "More than one element found in structure: " structure))))]

    (compiled-traverse* path result-fn structure)
    (let [ret (get-cell res)]
      (if (identical? ret NONE)
        nil
        ret))))


(defn compiled-select-one!* [path structure]
  (let [res (mutable-cell NONE)
        result-fn (fn [structure]
                    (let [curr (get-cell res)]
                      (if (identical? curr NONE)
                        (set-cell! res structure)
                        (throw-illegal "More than one element found in structure: " structure))))]

    (compiled-traverse* path result-fn structure)
    (let [ret (get-cell res)]
      (if (identical? NONE ret)
        (throw-illegal "Found no elements for select-one! on " structure))
      ret)))


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
        ret))))


(defn compiled-select-any* [path structure]
  (compiled-traverse* path identity structure))

(defn compiled-selected-any?* [path structure]
  (not= NONE (compiled-select-any* path structure)))

(defn compiled-transform*
  [^com.rpl.specter.impl.CompiledPath path transform-fn structure]
  (let [nav (.-nav path)
        ^com.rpl.specter.impl.ExecutorFunctions ex (.-executors path)]
    ((.-transform-executor ex) nav transform-fn structure)))



(defn params-needed-nav
  ^com.rpl.specter.impl.RichNavigator
  [^com.rpl.specter.impl.ParamsNeededPath path]
  (.-rich-nav path))

(defn compiled-path-rich-nav
  ^com.rpl.specter.impl.RichNavigator
  [^com.rpl.specter.impl.CompiledPath path]
  (let [^com.rpl.specter.impl.ParameterizedRichNav pr (.-nav path)]
    (.-rich-nav pr)))


(defn coerce-compiled->rich-nav [path]
  (if (instance? ParamsNeededPath path)
    path
    (let [nav (.-nav ^CompiledPath path)]
      (if (satisfies? Navigator nav)
        (no-params-rich-compiled-path (coerce-rich-navigator nav))
        path))))


(defn fn-invocation? [f]
  (or #?(:clj  (instance? clojure.lang.Cons f))
      #?(:clj  (instance? clojure.lang.LazySeq f))
      #?(:cljs (instance? cljs.core.LazySeq f))
      (list? f)))

(defn codewalk-until [pred on-match-fn structure]
  (if (pred structure)
    (on-match-fn structure)
    (let [ret (walk/walk (partial codewalk-until pred on-match-fn) identity structure)]
      (if (and (fn-invocation? structure) (fn-invocation? ret))
        (with-meta ret (meta structure))
        ret))))

(defrecord LayeredNav [underlying])

(defn layered-nav? [o] (instance? LayeredNav o))

(defn layered-nav-underlying [^LayeredNav ln]
  (.-underlying ln))

(defn verify-layerable! [anav]
  (if-not
    (or (root-params-nav? anav)
        (and (instance? ParamsNeededPath anav)
             (> (:num-needed-params anav) 0)))
    (throw-illegal "defnavconstructor must be used on a navigator defined with
      defnav with at least one parameter")))


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
       (->LayeredNav (apply anav a1 a2 a3 a4 a5 a6 a7 a8 a9 a10 args)))))


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
   params-maker]) ; can be null


(def MUST-CACHE-PATHS (mutable-cell false))

(defn must-cache-paths!
  ([] (must-cache-paths! true))
  ([v] (set-cell! MUST-CACHE-PATHS v)))

(defn constant-node? [node]
  (cond (and (instance? VarUse node)
             (-> node :var meta :dynamic not)) true
        (record? node) false
        (number? node) true
        (keyword? node) true
        (string? node) true
        (vector? node) (every? constant-node? node)
        (set? node) (every? constant-node? node)
        (map? node) (and (every? constant-node? (vals node))
                         (every? constant-node? (keys node)))
        :else false))

(defn extract-constant [node]
  (cond (some #(% node) [number? keyword? string?]) node
        (instance? VarUse node) (:val node)
        (vector? node) (vec (map extract-constant node))
        (set? node) (set (map extract-constant node))
        (map? node) (->> node
                         (map (fn [[k v]] [(extract-constant k) (extract-constant v)]))
                         (into {}))
        :else (throw-illegal "Unknown node " node)))


(defn- extract-original-code [p]
  (cond
    (instance? LocalSym p) (:sym p)
    (instance? VarUse p) (:sym p)
    (instance? SpecialFormUse p) (:code p)
    (instance? FnInvocation p) (:code p)
    :else p))


(defn- valid-navigator? [v]
  (or (satisfies? p/ImplicitNav v)
      (instance? CompiledPath v)))

#?(:cljs
   (defn handle-params [precompiled params-maker possible-params]
     (let [params (fast-object-array (count params-maker))]
       (dotimes [i (count params-maker)]
         (aset params i ((get possible-params (get params-maker i)))))
       (bind-params* precompiled params 0))))


(defn filter-select [afn structure next-fn]
  (if (afn structure)
    (next-fn structure)
    NONE))

(defn filter-transform [afn structure next-fn]
  (if (afn structure)
    (next-fn structure)
    structure))

(def pred*
  (->ParamsNeededPath
    (reify RichNavigator
      (rich_select [this params params-idx vals structure next-fn]
        (let [afn (aget ^objects params params-idx)]
          (if (afn structure)
            (next-fn params (inc params-idx) vals structure)
            NONE)))

      (rich_transform [this params params-idx vals structure next-fn]
        (let [afn (aget ^objects params params-idx)]
          (if (afn structure)
            (next-fn params (inc params-idx) vals structure)
            structure))))

    1))


(def collected?*
  (->ParamsNeededPath
    (reify RichNavigator
      (rich_select [this params params-idx vals structure next-fn]
        (let [afn (aget ^objects params params-idx)]
          (if (afn vals)
            (next-fn params (inc params-idx) vals structure)
            NONE)))

      (rich_transform [this params params-idx vals structure next-fn]
        (let [afn (aget ^objects params params-idx)]
          (if (afn vals)
            (next-fn params (inc params-idx) vals structure)
            structure))))

    1))


(def rich-compiled-path-proxy
  (->ParamsNeededPath
    (reify RichNavigator
      (rich_select [this params params-idx vals structure next-fn]
        (let [apath ^CompiledPath (aget ^objects params params-idx)
              pnav ^ParameterizedRichNav (.-nav apath)
              nav (.-rich-nav pnav)]
          (exec-rich_select
            nav
            (.-params pnav)
            (.-params-idx pnav)
            vals
            structure
            (fn [_ _ vals-next structure-next]
              (next-fn params params-idx vals-next structure-next)))))

      (rich_transform [this params params-idx vals structure next-fn]
        (let [apath ^CompiledPath (aget ^objects params params-idx)
              pnav ^ParameterizedRichNav (.-nav apath)
              nav (.-rich-nav pnav)]
          (exec-rich_transform
            nav
            (.-params pnav)
            (.-params-idx pnav)
            vals
            structure
            (fn [_ _ vals-next structure-next]
              (next-fn params params-idx vals-next structure-next))))))

    1))


(def lean-compiled-path-proxy
  (->ParamsNeededPath
    (reify RichNavigator
      (rich_select [this params params-idx vals structure next-fn]
        (let [^CompiledPath apath (aget ^objects params params-idx)
              ^Navigator nav (.-nav apath)]
          (exec-select*
            nav
            structure
            (fn [structure-next]
              (next-fn params params-idx vals structure-next)))))

      (rich_transform [this params params-idx vals structure next-fn]
        (let [^CompiledPath apath (aget ^objects params params-idx)
              ^Navigator nav (.-nav apath)]
          (exec-transform*
            nav
            structure
            (fn [structure-next]
              (next-fn params params-idx vals structure-next))))))

    1))


(defn srange-transform* [structure start end next-fn]
  (let [structurev (vec structure)
        newpart (next-fn (-> structurev (subvec start end)))
        res (concat (subvec structurev 0 start)
                    newpart
                    (subvec structurev end (count structure)))]
    (if (vector? structure)
      (vec res)
      res)))


(defn- variadic-arglist? [al]
  (contains? (set al) '&))

(defn- arglist-for-params-count [arglists c code]
  (let [ret (->> arglists
                 (filter
                   (fn [al]
                     (or (= (count al) c)
                         (variadic-arglist? al))))

                 first)
        len (count ret)]
    (when-not ret
      (throw-illegal "Invalid # arguments at " code))
    (if (variadic-arglist? ret)
      (srange-transform* ret (- len 2) len
        (fn [_] (repeatedly (- c (- len 2)) gensym)))
      ret)))


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
              (magic-fail! "Var " (:sym p) " is not a navigator")))


      (instance? SpecialFormUse p)
      (if (->> p :code first (contains? #{'fn* 'fn}))
        (do
          (swap! params-atom conj (:code p))
          pred*)

        (magic-fail! "Special form " (:code p) " where navigator expected"))


      (instance? FnInvocation p)
      (let [op (:op p)
            ps (:params p)]
        (if (instance? VarUse op)
          (let [v (:var op)
                vv (:val op)]
            (if (-> v meta :dynamic)
              (magic-fail! "Var " (:sym op) " is dynamic")
              (cond
                (or (root-params-nav? vv) (instance? ParamsNeededPath vv))
                (if (every? constant-node? ps)
                  (apply vv (map extract-constant ps))
                  (do
                    (swap! params-atom #(vec (concat % ps)))
                    (coerce-path vv)))


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
                                            (extract-original-code p)))))

                                  al
                                   ps))]
                  (if @failed-atom
                    nil
                    (apply vv subpath)))


                (and (fn? vv) (-> vv meta :layerednav))
                (if (every? constant-node? ps)
                  (apply vv (map extract-constant ps))
                  (do
                    (swap! params-atom conj (:code p))
                    (if (= (-> vv meta :layerednav) :lean)
                      lean-compiled-path-proxy
                      rich-compiled-path-proxy)))


                :else
                (magic-fail! "Var " (:sym op) " must be either a parameterized "
                  "navigator, a higher order pathed constructor function, "
                  "or a nav constructor"))))

          (magic-fail! "Code at " (extract-original-code p) " is in "
            "function invocation position and must be either a parameterized "
            "navigator, a higher order pathed constructor function, or a "
            "nav constructor.")))



      :else
      (cond (set? p)
            (if (constant-node? p)
              (extract-constant p)
              (do (swap! params-atom conj p)
                pred*))

            (keyword? p)
            p

            ;; in case anyone extends String for their own use case
            (and (string? p) (valid-navigator? p))
            p

            :else
            (magic-fail! "Code " p " is not a valid navigator or can't be factored")))))




;; This is needed when aset is used on primitive values in mk-params-maker
;; to avoid reflection
#?(:clj
   (defn aset-object [^objects a i ^Object v]
     (aset a i v)))

#?(
   :clj
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
                    params-code)

                ~array-sym))))))


   :cljs
   (defn mk-params-maker [ns-str params-code possible-params-code used-locals]
     (let [indexed (->> possible-params-code
                        (map-indexed (comp vec reverse vector))
                        (into {}))]
    ;;TODO: may be more efficient as an array
       (mapv (fn [c] (get indexed c)) params-code))))


;; possible-params-code is for cljs impl that can't use eval
(defn magic-precompilation [prepared-path ns-str used-locals possible-params-code]
  (let [params-atom (atom [])
        failed-atom (atom false)
        path (magic-precompilation* prepared-path params-atom failed-atom)]

    (if @failed-atom
      (if (get-cell MUST-CACHE-PATHS)
        (throw-illegal "Failed to cache path")
        (->CachedPathInfo nil nil))
      (let [precompiled (comp-paths* path)
            params-code (mapv extract-original-code @params-atom)
            params-maker (if-not (empty? params-code)
                           (mk-params-maker ns-str params-code possible-params-code used-locals))]

        ;; TODO: error if precompiled is compiledpath and there are params or
        ;; precompiled is paramsneededpath and there are no params...
        (->CachedPathInfo precompiled params-maker)))))





(defn compiled-setval* [path val structure]
  (compiled-transform* path (fast-constantly val) structure))

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
                   (last args))))

             structure)
     (get-cell state)]))


(defn- multi-transform-error-fn [& nav]
  (throw-illegal
    "All navigation in multi-transform must end in 'terminal' "
    "navigators. Instead navigated to: " nav))

(defn compiled-multi-transform* [path structure]
  (compiled-transform* path multi-transform-error-fn structure))

#?(:clj
   (defn extend-protocolpath* [protpath protpath-prot extensions]
     (let [extensions (partition 2 extensions)
           m (-> protpath-prot :sigs keys first)
           expected-params (num-needed-params protpath)]
       (doseq [[atype apath] extensions]
         (let [p (comp-paths-internalized apath)
               needed-params (num-needed-params p)
               rich-nav (extract-rich-nav p)]

           (if-not (= needed-params expected-params)
             (throw-illegal "Invalid number of params in extended protocol path, expected "
                 expected-params " but got " needed-params))
           (extend atype protpath-prot {m (fn [_] rich-nav)}))))))


(defn parameterize-path [apath params params-idx]
  (if (instance? CompiledPath apath)
    apath
    (bind-params* apath params params-idx)))


(defn mk-jump-next-fn [next-fn init-idx total-params]
  (let [jumped (+ init-idx total-params)]
    (fn [params params-idx vals structure]
      (next-fn params jumped vals structure))))
