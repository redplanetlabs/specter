(ns com.rpl.specter.impl
  #?(:cljs (:require-macros
            [com.rpl.specter.util-macros :refer [doseqres]]))

  (:use [com.rpl.specter.protocols :only
          [select* transform* collect-val Rich Navigator]]
        #?(:clj [com.rpl.specter.util-macros :only [doseqres]]))

  (:require [com.rpl.specter.protocols :as p]
            [clojure.string :as s]
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


(defn benchmark [iters afn]
  (time
   (dotimes [_ iters]
     (afn))))

#?(
   :clj
   (defmacro exec-select* [this & args]
     (let [hinted (with-meta this {:tag 'com.rpl.specter.protocols.Navigator})]
       `(.select* ~hinted ~@args)))


   :cljs
   (defn exec-select* [this vals structure next-fn]
     (p/select* ^not-native this vals structure next-fn)))


#?(
   :clj
   (defmacro exec-transform* [this & args]
     (let [hinted (with-meta this {:tag 'com.rpl.specter.protocols.Navigator})]
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


(defn- seq-contains? [aseq val]
  (->> aseq
       (filter (partial = val))
       empty?
       not))

(defn- coerce-object [this]
  (cond (satisfies? p/ImplicitNav this) (p/implicit-nav this)
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
      (exec-select* curr vals structure
        (fn [vals-next structure-next]
          (exec-select* next vals-next structure-next next-fn))))
    (transform* [this vals structure next-fn]
      (exec-transform* curr vals structure
        (fn [vals-next structure-next]
          (exec-rich-transform* next vals-next structure-next next-fn))))))

(extend-protocol PathComposer
  nil
  (do-comp-paths [o]
    (coerce-path o))
  #?(:clj Object :cljs default)
  (do-comp-paths [o]
    (coerce-path o))
  #?(:clj java.util.List :cljs cljs.core/PersistentVector)
  (do-comp-paths [navigators]
    (reduce combine-two-navs navigators)))


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
  (exec-select* path [] structure result-fn))




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


;;TODO: could inline cache the transform-fn, or even have a different one
;;if know there are no vals at the end
(defn compiled-transform* [path transform-fn structure]
  (exec-transform* nav [] structure
    (fn [vals structure]
      (if (identical? vals [])
        (transform-fn vals)
        (apply transform-fn (conj vals structure))))))

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

(defrecord CachedPathInfo
  [path-fn])


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

(defn pred* [afn]
  (reify RichNavigator
    (select* [this vals structure next-fn]
      (if (afn structure)
        (next-fn vals structure)
        NONE))
    (transform* [this vals structure next-fn]
      (if (afn structure)
        (next-fn vals structure)
        structure))))


(defn collected?* [afn]
  (reify RichNavigator
    (select* [this vals structure next-fn]
      (if (afn vals)
        (next-fn vals structure)
        NONE))
    (transform* [this vals structure next-fn]
      (if (afn vals)
        (next-fn vals structure)
        structure))))

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

;;TODO: all needs to change
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
