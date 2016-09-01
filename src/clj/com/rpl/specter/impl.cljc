(ns com.rpl.specter.impl
  #?(:cljs (:require-macros
            [com.rpl.specter.util-macros :refer [doseqres]]))

  (:use [com.rpl.specter.protocols :only
          [select* transform* collect-val RichNavigator]]
        #?(:clj [com.rpl.specter.util-macros :only [doseqres]]))

  (:require [com.rpl.specter.protocols :as p]
            [clojure.string :as s]
            [clojure.walk :as walk]
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

#?(
   :clj
   (defmacro fast-object-array [i]
     `(com.rpl.specter.Util/makeObjectArray ~i))

   :cljs
   (defn fast-object-array [i]
     (object-array i)))

(defn benchmark [iters afn]
  (time
   (dotimes [_ iters]
     (afn))))

#?(
   :clj
   (defmacro exec-select* [this & args]
     (let [hinted (with-meta this {:tag 'com.rpl.specter.protocols.RichNavigator})]
       `(.select* ~hinted ~@args)))


   :cljs
   (defn exec-select* [this vals structure next-fn]
     (p/select* ^not-native this vals structure next-fn)))


#?(
   :clj
   (defmacro exec-transform* [this & args]
     (let [hinted (with-meta this {:tag 'com.rpl.specter.protocols.RichNavigator})]
       `(.transform* ~hinted ~@args)))


   :cljs
   (defn exec-transform* [this vals structure next-fn]
     (p/transform* ^not-native this vals structure next-fn)))

(defprotocol PathComposer
  (do-comp-paths [paths]))

(defn rich-nav? [n]
  (instance? com.rpl.specter.protocols.RichNavigator n))

(defn comp-paths* [p]
  (if (rich-nav? p) p (do-comp-paths p)))

(defn- coerce-object [this]
  (cond (satisfies? p/ImplicitNav this) (p/implicit-nav this)
        (rich-nav? this) this
        :else (throw-illegal "Not a navigator: " this)))


(defprotocol CoercePath
  (coerce-path [this]))

(extend-protocol CoercePath
  nil ; needs its own coercer because it doesn't count as an Object
  (coerce-path [this]
    (coerce-object this))

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


(defn combine-two-navs [nav1 nav2]
  (reify RichNavigator
    (select* [this vals structure next-fn]
      (exec-select* nav1 vals structure
        (fn [vals-next structure-next]
          (exec-select* nav2 vals-next structure-next next-fn))))
    (transform* [this vals structure next-fn]
      (exec-transform* nav1 vals structure
        (fn [vals-next structure-next]
          (exec-transform* nav2 vals-next structure-next next-fn))))))

(extend-protocol PathComposer
  nil
  (do-comp-paths [o]
    (coerce-path o))
  #?(:clj Object :cljs default)
  (do-comp-paths [o]
    (coerce-path o))
  #?(:clj java.util.List :cljs cljs.core/PersistentVector)
  (do-comp-paths [navigators]
    (reduce combine-two-navs (map coerce-path navigators))))

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

;; TODO: this used to be a macro for clj... check if that's still important
(defn compiled-traverse* [path result-fn structure]
  (exec-select*
   path
   []
   structure
   (fn [vals structure]
    (if (identical? vals [])
      (result-fn structure)
      (result-fn (conj vals structure))))))




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

(defn terminal* [afn vals structure]
  (if (identical? vals [])
    (afn structure)
    (apply afn (conj vals structure))))

;;TODO: could inline cache the transform-fn, or even have a different one
;;if know there are no vals at the end
(defn compiled-transform* [nav transform-fn structure]
  (exec-transform* nav [] structure
    (fn [vals structure]
      (terminal* transform-fn vals structure))))

(defn fn-invocation? [f]
  (or #?(:clj  (instance? clojure.lang.Cons f))
      #?(:clj  (instance? clojure.lang.LazySeq f))
      #?(:cljs (instance? cljs.core.LazySeq f))
      (list? f)))

(defrecord LocalSym
  [val sym])

(defrecord VarUse
  [val var sym])

(defrecord SpecialFormUse
  [val code])

(defrecord FnInvocation
  ;; op and params elems can be any of the above
  [op params code])

(defrecord DynamicVal
  [code])

(defrecord DynamicPath
  [path])

(defrecord DynamicFunction
  [op params])

(defn dynamic-param? [o]
  (contains? #{DynamicPath DynamicVal DynamicFunction} (class o)))

(defn static-path? [path]
  (if (sequential? path)
    (every? (complement dynamic-param?) path)
    (-> path dynamic-param? not)))

(defn late-path [path]
  (if (static-path? path)
    (comp-paths* path)
    (com.rpl.specter.impl/->DynamicPath path)))



(defrecord CachedPathInfo
  [dynamic? precompiled])


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
      (rich-nav? v)))


(defn filter-select [afn structure next-fn]
  (if (afn structure)
    (next-fn structure)
    NONE))

(defn filter-transform [afn structure next-fn]
  (if (afn structure)
    (next-fn structure)
    structure))

(defn ^:direct-nav pred* [afn]
  (reify RichNavigator
    (select* [this vals structure next-fn]
      (if (afn structure)
        (next-fn vals structure)
        NONE))
    (transform* [this vals structure next-fn]
      (if (afn structure)
        (next-fn vals structure)
        structure))))

(def STAY*
  (reify RichNavigator
    (select* [this vals structure next-fn]
      (next-fn vals structure))
    (transform* [this vals structure next-fn]
      (next-fn vals structure))))

(defn ^:direct-nav collected?* [afn]
  (reify RichNavigator
    (select* [this vals structure next-fn]
      (if (afn vals)
        (next-fn vals structure)
        NONE))
    (transform* [this vals structure next-fn]
      (if (afn vals)
        (next-fn vals structure)
        structure))))

(defn ^:direct-nav cell-nav [cell]
  (reify RichNavigator
    (select* [this vals structure next-fn]
      (exec-select* (get-cell cell) vals structure next-fn))
    (transform* [this vals structure next-fn]
      (exec-transform* (get-cell cell) vals structure next-fn))))

(defn local-declarepath []
  (let [cell (mutable-cell nil)]
    (vary-meta (cell-nav cell) assoc ::cell cell)))

(defn providepath* [declared compiled-path]
  (let [cell (-> declared meta ::cell)]
    (set-cell! cell compiled-path)))



(defn gensyms [amt]
  (vec (repeatedly amt gensym)))

(defmacro mk-comp-navs []
  (let [impls (for [i (range 3 20)]
                (let [[fsym & rsyms :as syms] (gensyms i)]
                  `([~@syms] (~'comp-navs ~fsym (~'comp-navs ~@rsyms)))))
        last-syms (gensyms 19)]
    `(defn comp-navs
       ([] STAY*)
       ([nav1#] nav1#)
       ([nav1# nav2#] (combine-two-navs nav1# nav2#))
       ~@impls
       ([~@last-syms ~'& rest#]
        (~'comp-navs
          (~'comp-navs ~@last-syms)
          (reduce comp-navs rest#))))))

(mk-comp-navs)

(defn srange-transform* [structure start end next-fn]
  (let [structurev (vec structure)
        newpart (next-fn (-> structurev (subvec start end)))
        res (concat (subvec structurev 0 start)
                    newpart
                    (subvec structurev end (count structure)))]
    (if (vector? structure)
      (vec res)
      res)))

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
          [(conj ranges [curr-start (inc curr-last)]) i i]))

      [[] nil nil]
      (concat (matching-indices aseq p) [-1]))))

(defn continuous-subseqs-transform* [pred structure next-fn]
  (reduce
    (fn [structure [s e]]
      (srange-transform* structure s e next-fn))
    structure
    (reverse (matching-ranges structure pred))))

(defn codewalk-until [pred on-match-fn structure]
  (if (pred structure)
    (on-match-fn structure)
    (let [ret (walk/walk (partial codewalk-until pred on-match-fn) identity structure)]
      (if (and (fn-invocation? structure) (fn-invocation? ret))
        (with-meta ret (meta structure))
        ret))))


(def ^:dynamic *tmp-closure*)
(defn closed-code [closure body]
  (let [lv (mapcat #(vector % `(*tmp-closure* '~%))
                   (keys closure))]
    (binding [*tmp-closure* closure]
      (eval `(let [~@lv] ~body)))))

(defn any?
  "Accepts any number of predicates that take in one input and returns a new predicate that returns true if any of them are true"
  [& preds]
  (fn [obj]
    (some #(% obj) preds)))


(let [embeddable? (any? number?
                        symbol?
                        keyword?
                        string?
                        char?
                        list?
                        vector?
                        set?
                        #(and (map? %) (not (record? %)))
                        nil?
                        #(instance? clojure.lang.Cons %)
                        #(instance? clojure.lang.LazySeq %))]
  (defn eval+
    "Automatically extracts non-evalable stuff into a closure and then evals"
    [form]
    (let [replacements (mutable-cell {})
          new-form (codewalk-until
                     #(-> % embeddable? not)
                     (fn [o]
                       (let [s (gensym)]
                         (update-cell! replacements #(assoc % s o))
                         s))
                     form)
          closure (get-cell replacements)]
      (closed-code closure new-form))))

(defn coerce-nav [o]
  (if (instance? com.rpl.specter.protocols.RichNavigator o)
    o
    (p/implicit-nav o)))


(defn dynamic-var? [v]
  (-> v meta :dynamic))

;; original-obj stuff is done to avoid using functions with metadata on them
;; clojure's implementation of function metadata causes the function to do an
;; apply for every invocation
(defn direct-nav-obj [o]
  (vary-meta o merge {:direct-nav true :original-obj o}))

(defn maybe-direct-nav [obj direct-nav?]
  (if direct-nav?
    (direct-nav-obj obj)
    obj))

(defn original-obj [o]
  (let [orig (-> o meta :original-obj)]
    (if orig
      (recur orig)
      o)))

(defn direct-nav? [o]
  (-> o meta :direct-nav))


;; don't do coerce-nav here... save that for resolve-magic-code
(defn- magic-precompilation* [o]
  (cond (sequential? o)
        (flatten (map magic-precompilation* o))

        (instance? VarUse o)
        (if (dynamic-var? (:var o))
          (->DynamicVal (maybe-direct-nav
                         (:sym o)
                         (or (-> o :var direct-nav?)
                             (-> o :sym direct-nav?))))

          (maybe-direct-nav
           (:val o)
           (or (-> o :var direct-nav?)
               (-> o :sym direct-nav?)
               (-> o :val direct-nav?))))

        (instance? LocalSym o)
        (->DynamicVal (:sym o))

        (instance? SpecialFormUse o)
        (let [code (:code o)
              v (->DynamicVal code)]
          (if (= 'fn* (first code))
            (->DynamicFunction pred* [v])))

        (instance? FnInvocation o)
        (let [op (magic-precompilation* (:op o))
              params (map magic-precompilation* (:params o))]
          (if (-> op meta :dynamicnav)
            (apply op params)
            (->DynamicFunction op params)))

        :else
        ;; this handles dynamicval as well
        o))



(declare resolve-magic-code)

(defn all-static? [params]
  (every? (complement dynamic-param?) params))

(defn resolve-dynamic-fn-arg [o]
  (cond (instance? DynamicFunction o)
        (let [op (resolve-dynamic-fn-arg (:op o))
              params (map resolve-dynamic-fn-arg (:params o))]
          (if (all-static? (conj params op))
            (apply op params)
            (->DynamicFunction op params)))

        (instance? DynamicVal o)
        o

        (instance? DynamicPath o)
        (let [res (resolve-magic-code o)]
          (if (rich-nav? res)
            res
            (->DynamicVal res)))

        :else
        o))

(defn resolve-dynamic-fn-arg-code [o]
  (cond (instance? DynamicFunction o)
        (let [op (resolve-dynamic-fn-arg-code (:op o))
              params (map resolve-dynamic-fn-arg-code (:params o))]
          `(~(original-obj op) ~@params))

        (instance? DynamicVal o)
        (:code o)

        :else
        (original-obj o)))


(defn resolve-magic-code [o]
  (cond
    (instance? DynamicPath o)
    (let [path (:path o)]
      (if (sequential? path)
        (if (empty? path)
          STAY*
          (let [resolved (vec (map resolve-magic-code path))
                combined (continuous-subseqs-transform*
                          rich-nav?
                          resolved
                          (fn [s] [(comp-paths* s)]))]
            (if (= 1 (count combined))
              (first combined)
              `(comp-navs ~@combined))))
        (resolve-magic-code path)))

    (instance? DynamicVal o)
    (if (-> o :code direct-nav?)
      (:code o)
     `(coerce-nav ~(:code o)))

    (instance? DynamicFunction o)
    (let [op (resolve-dynamic-fn-arg (:op o))
          params (map resolve-dynamic-fn-arg (:params o))]
      (if (all-static? (conj params op))
        (coerce-nav (apply op params))
        (let [code `(~(-> op resolve-dynamic-fn-arg-code original-obj)
                      ~@(map resolve-dynamic-fn-arg-code params))]
          (if (direct-nav? op) code `(coerce-nav ~code)))))

    :else
    (coerce-nav o)))


(defn magic-precompilation [path ns-str used-locals]
  (let [path (magic-precompilation* path)
;        _ (println "magic-precompilation*" path)
        ns (find-ns (symbol ns-str))
        maker (binding [*ns* ns]
                (eval+
;                 (spy
                  `(fn [~@used-locals]
                     ~(resolve-magic-code (->DynamicPath path)))))]
    (if (static-path? path)
      (->CachedPathInfo false (maker))
      (->CachedPathInfo true maker))))


  ;; TODO: could have a global flag about whether or not to compile and cache static
  ;; portions, or whether to compile everything together on each invocation (so that
  ;; it can be redefined in repl


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

;;TODO: need a way to deal with protocol paths...
;;maybe they get extended with a function and produce a `path`
;;but could be recursive
; #?(:clj
;    (defn extend-protocolpath* [protpath protpath-prot extensions]
;      (let [extensions (partition 2 extensions)
;            m (-> protpath-prot :sigs keys first)
;            expected-params (num-needed-params protpath)]
;        (doseq [[atype apath] extensions]
;          (let [p (comp-paths-internalized apath)
;                needed-params (num-needed-params p)
;                rich-nav (extract-rich-nav p)]
;
;            (if-not (= needed-params expected-params)
;              (throw-illegal "Invalid number of params in extended protocol path, expected "
;                  expected-params " but got " needed-params))
;            (extend atype protpath-prot {m (fn [_] rich-nav)}))))))
