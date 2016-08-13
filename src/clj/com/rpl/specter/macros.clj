(ns com.rpl.specter.macros
  (:use [com.rpl.specter.protocols :only [Navigator]]
        [com.rpl.specter.impl :only [RichNavigator]])
  (:require [com.rpl.specter.impl :as i]
            [clojure.walk :as cljwalk]
            [com.rpl.specter.defnavhelpers :as dnh]))


(defn ^:no-doc gensyms [amt]
  (vec (repeatedly amt gensym)))

(defn ^:no-doc determine-params-impls [impls]
  (let [grouped (->> impls (map (fn [[n & body]] [n body])) (into {}))]
    (if-not (= #{'select* 'transform*} (-> grouped keys set))
      (i/throw-illegal "defnav must implement select* and transform*, instead got "
                       (keys grouped)))
    grouped))


(defmacro richnav
  "Defines a navigator with full access to collected vals, the parameters array,
  and the parameters array index. `next-fn` expects to receive the params array,
  a params index, the collected vals, and finally the next structure.
  `next-fn` will automatically skip ahead in params array by `num-params`, so the
  index passed to it is ignored.
  This is the lowest level way of making navigators."
  [num-params & impls]
  (let [{[s-params & s-body] 'select*
         [t-params & t-body] 'transform*} (determine-params-impls impls)
        s-next-fn-sym (last s-params)
        s-pidx-sym (nth s-params 2)
        t-next-fn-sym (last t-params)
        t-pidx-sym (nth t-params 2)]

    `(let [num-params# ~num-params
           nav# (reify RichNavigator
                  (~'rich-select* ~s-params
                    (let [~s-next-fn-sym (i/mk-jump-next-fn ~s-next-fn-sym ~s-pidx-sym num-params#)]
                      ~@s-body))
                  (~'rich-transform* ~t-params
                    (let [~t-next-fn-sym (i/mk-jump-next-fn ~t-next-fn-sym ~t-pidx-sym num-params#)]
                      ~@t-body)))]

       (if (zero? num-params#)
         (i/no-params-rich-compiled-path nav#)
         (i/->ParamsNeededPath nav# num-params#)))))


(defmacro ^:no-doc lean-nav* [& impls]
  `(reify Navigator ~@impls))

(defn ^:no-doc operation-with-bindings [bindings params-sym params-idx-sym op-maker]
  (let [bindings (partition 2 bindings)
        binding-fn-syms (gensyms (count bindings))
        binding-syms (map first bindings)
        fn-exprs (map second bindings)
        binding-fn-declarations (vec (mapcat vector binding-fn-syms fn-exprs))
        binding-declarations (vec (mapcat (fn [s f] [s `(~f ~params-sym ~params-idx-sym)])
                                          binding-syms
                                          binding-fn-syms))
        body (op-maker binding-declarations)]
    `(let [~@binding-fn-declarations]
       ~body)))


(defn- rich-nav-with-bindings-not-inlined [num-params-code bindings impls]
  (let [{[[_ s-structure-sym s-next-fn-sym] & s-body] 'select*
         [[_ t-structure-sym t-next-fn-sym] & t-body] 'transform*}
        (determine-params-impls impls)
        params-sym (gensym "params")
        params-idx-sym (gensym "params-idx")]
    (operation-with-bindings
      bindings
      params-sym
      params-idx-sym
      (fn [binding-declarations]
        `(reify RichNavigator
          (~'rich-select* [this# ~params-sym ~params-idx-sym vals# ~s-structure-sym next-fn#]
            (let [~@binding-declarations
                  next-params-idx# (+ ~params-idx-sym ~num-params-code)
                  ~s-next-fn-sym (fn [structure#]
                                   (next-fn# ~params-sym
                                             next-params-idx#
                                             vals#
                                             structure#))]
              ~@s-body))

          (~'rich-transform* [this# ~params-sym ~params-idx-sym vals# ~t-structure-sym next-fn#]
            (let [~@binding-declarations
                  next-params-idx# (+ ~params-idx-sym ~num-params-code)
                  ~t-next-fn-sym (fn [structure#]
                                   (next-fn# ~params-sym
                                             next-params-idx#
                                             vals#
                                             structure#))]
              ~@t-body)))))))

(defn inline-next-fn [body next-fn-sym extra-params]
  (i/codewalk-until
    #(and (i/fn-invocation? %) (= next-fn-sym (first %)))
    (fn [code]
      (let [code (map #(inline-next-fn % next-fn-sym extra-params) code)]
        (concat [next-fn-sym] extra-params (rest code))))
    body))

(defn- rich-nav-with-bindings-inlined [num-params-code bindings impls]
  (let [{[[_ s-structure-sym s-next-fn-sym] & s-body] 'select*
         [[_ t-structure-sym t-next-fn-sym] & t-body] 'transform*}
        (determine-params-impls impls)
        params-sym (gensym "params")
        params-idx-sym (gensym "params-idx")
        vals-sym (gensym "vals")
        next-params-idx-sym (gensym "next-params-idx")
        s-body (inline-next-fn s-body s-next-fn-sym [params-sym next-params-idx-sym vals-sym])
        t-body (inline-next-fn t-body t-next-fn-sym [params-sym next-params-idx-sym vals-sym])]
    (operation-with-bindings
      bindings
      params-sym
      params-idx-sym
      (fn [binding-declarations]
        `(reify RichNavigator
          (~'rich-select* [this# ~params-sym ~params-idx-sym ~vals-sym ~s-structure-sym ~s-next-fn-sym]
            (let [~@binding-declarations
                  ~next-params-idx-sym (+ ~params-idx-sym ~num-params-code)]
              ~@s-body))

          (~'rich-transform* [this# ~params-sym ~params-idx-sym ~vals-sym ~t-structure-sym ~t-next-fn-sym]
            (let [~@binding-declarations
                  ~next-params-idx-sym (+ ~params-idx-sym ~num-params-code)]
              ~@t-body)))))))

(defmacro ^:no-doc rich-nav-with-bindings [opts num-params-code bindings & impls]
  (if (:inline-next-fn opts)
    (rich-nav-with-bindings-inlined num-params-code bindings impls)
    (rich-nav-with-bindings-not-inlined num-params-code bindings impls)))


(defmacro ^:no-doc collector-with-bindings [num-params-code bindings impl]
  (let [[_ [_ structure-sym] & body] impl
        params-sym (gensym "params")
        params-idx-sym (gensym "params")]
    (operation-with-bindings
      bindings
      params-sym
      params-idx-sym
      (fn [binding-declarations]
        `(let [num-params# ~num-params-code
               cfn# (fn [~params-sym ~params-idx-sym vals# ~structure-sym next-fn#]
                      (let [~@binding-declarations]
                        (next-fn# ~params-sym (+ ~params-idx-sym num-params#) (conj vals# (do ~@body)) ~structure-sym)))]

          (reify RichNavigator
            (~'rich-select* [this# params# params-idx# vals# structure# next-fn#]
              (cfn# params# params-idx# vals# structure# next-fn#))
            (~'rich-transform* [this# params# params-idx# vals# structure# next-fn#]
              (cfn# params# params-idx# vals# structure# next-fn#))))))))


(defn- delta-param-bindings [params]
  (->> params
       (map-indexed (fn [i p] [p `(dnh/param-delta ~i)]))
       (apply concat)
       vec))


(defmacro nav
  "Defines a navigator with late bound parameters. This navigator can be precompiled
  with other navigators without knowing the parameters. When precompiled with other
  navigators, the resulting path takes in parameters for all navigators in the path
  that needed parameters (in the order in which they were declared)."
  [& impl]
  (let [[opts params & impls] (if (map? (first impl))
                                impl
                                (cons {} impl))]
    (if (empty? params)
      `(i/lean-compiled-path (lean-nav* ~@impls))
      `(vary-meta
        (fn ~params (i/lean-compiled-path (lean-nav* ~@impls)))
        assoc
        :highernav
        {:type :lean
         :params-needed-path
         (i/->ParamsNeededPath
          (rich-nav-with-bindings ~opts
                                  ~(count params)
                                  ~(delta-param-bindings params)
                                  ~@impls)

          ~(count params))}))))


(defmacro collector
  "Defines a Collector with late bound parameters. This collector can be precompiled
  with other selectors without knowing the parameters. When precompiled with other
  selectors, the resulting selector takes in parameters for all selectors in the path
  that needed parameters (in the order in which they were declared).
  "
  [params body]
  `(let [rich-nav# (collector-with-bindings ~(count params)
                                           ~(delta-param-bindings params)
                                           ~body)]

     (if ~(empty? params)
       (i/no-params-rich-compiled-path rich-nav#)
       (vary-meta
         (fn ~params
           (i/no-params-rich-compiled-path
             (collector-with-bindings 0 []
                ~body)))
         assoc
         :highernav
         {:type :rich
          :params-needed-path
          (i/->ParamsNeededPath
           rich-nav#
           ~(count params))}))))



(defn ^:no-doc fixed-pathed-operation [bindings op-maker]
  (let [bindings (partition 2 bindings)
        late-path-syms (map first bindings)
        paths-code (vec (map second bindings))
        delta-syms (vec (gensyms (count bindings)))
        compiled-syms (vec (gensyms (count bindings)))
        runtime-bindings (vec (mapcat
                               (fn [l c d]
                                 `[~l (dnh/bound-params ~c ~d)])

                               late-path-syms
                               compiled-syms
                               delta-syms))
        total-params-sym (gensym "total-params")
        body (op-maker runtime-bindings compiled-syms total-params-sym)]
    `(let [compiled# (doall (map i/comp-paths* ~paths-code))
           ~compiled-syms compiled#
           deltas# (cons 0 (reductions + (map i/num-needed-params compiled#)))
           ~delta-syms deltas#
           ~total-params-sym (last deltas#)]

       ~body)))


(defmacro fixed-pathed-nav
  "This helper is used to define navigators that take in a fixed number of other
  paths as input. Those paths may require late-bound params, so this helper
  will create a parameterized navigator if that is the case. If no late-bound params
  are required, then the result is executable."
  [bindings & impls]
  (fixed-pathed-operation bindings
    (fn [runtime-bindings compiled-syms total-params-sym]
      (let [late-syms (map first (partition 2 bindings))
            lean-bindings (mapcat vector late-syms compiled-syms)]
        `(if (zero? ~total-params-sym)
           (let [~@lean-bindings]
             (i/lean-compiled-path (lean-nav* ~@impls)))

           (i/->ParamsNeededPath
            (rich-nav-with-bindings {}
                                    ~total-params-sym
                                    ~runtime-bindings
                                    ~@impls)

            ~total-params-sym))))))




(defmacro fixed-pathed-collector
  "This helper is used to define collectors that take in a fixed number of
  paths as input. That path may require late-bound params, so this helper
  will create a parameterized navigator if that is the case. If no late-bound params
  are required, then the result is executable."
  [bindings & body]
  (fixed-pathed-operation bindings
    (fn [runtime-bindings compiled-syms total-params-sym]
      (let [late-syms (map first (partition 2 bindings))
            lean-bindings (mapcat vector late-syms compiled-syms)]
        `(if (zero? ~total-params-sym)
           (let [~@lean-bindings]
             (i/no-params-rich-compiled-path
               (collector-with-bindings 0 [] ~@body)))
           (i/->ParamsNeededPath
            (collector-with-bindings ~total-params-sym
                                     ~runtime-bindings
                                     ~@body)

            ~total-params-sym))))))


(defmacro paramsfn [params [structure-sym] & impl]
  `(nav ~params
    (~'select* [this# structure# next-fn#]
               (let [afn# (fn [~structure-sym] ~@impl)]
                 (i/filter-select afn# structure# next-fn#)))

    (~'transform* [this# structure# next-fn#]
                  (let [afn# (fn [~structure-sym] ~@impl)]
                    (i/filter-transform afn# structure# next-fn#)))))


(defmacro defnav [name & body]
  `(def ~name (nav ~@body)))

(defmacro defcollector [name & body]
  `(def ~name (collector ~@body)))


(defn- protpath-sym [name]
  (-> name (str "-prot") symbol))


(defmacro defprotocolpath
  "Defines a navigator that chooses the path to take based on the type
  of the value at the current point. May be specified with parameters to
  specify that all extensions must require that number of parameters.

  Currently not available for ClojureScript.

  Example of usage:
  (defrecord SingleAccount [funds])
  (defrecord FamilyAccount [single-accounts])

  (defprotocolpath FundsPath)
  (extend-protocolpath FundsPath
    SingleAccount :funds
    FamilyAccount [ALL FundsPath]
    )
"
  ([name]
   `(defprotocolpath ~name []))
  ([name params]
   (let [prot-name (protpath-sym name)
         m (-> name (str "-retrieve") symbol)
         num-params (count params)
         ssym (gensym "structure")
         rargs [(gensym "params") (gensym "pidx") (gensym "vals") ssym (gensym "next-fn")]
         retrieve `(~m ~ssym)]

     `(do
        (defprotocol ~prot-name (~m [structure#]))
        (let [nav# (reify RichNavigator
                     (~'rich-select* [this# ~@rargs]
                       (let [inav# ~retrieve]
                         (i/exec-rich-select* inav# ~@rargs)))

                     (~'rich-transform* [this# ~@rargs]
                       (let [inav# ~retrieve]
                         (i/exec-rich-transform* inav# ~@rargs))))]

          (def ~name
            (if (= ~num-params 0)
              (i/no-params-rich-compiled-path nav#)
              (i/->ParamsNeededPath nav# ~num-params))))))))





(defn ^:no-doc declared-name [name]
  (vary-meta (symbol (str name "-declared"))
             assoc :no-doc true))


(defmacro declarepath
  ([name]
   `(declarepath ~name []))
  ([name params]
   (let [platform (if (contains? &env :locals) :cljs :clj)
         select-exec (if (= platform :clj)
                       `i/exec-rich-select*
                       `i/rich-select*)
         transform-exec (if (= platform :clj)
                          `i/exec-rich-transform*
                          `i/rich-transform*)
         num-params (count params)
         declared (declared-name name)
         rargs [(gensym "params") (gensym "pidx") (gensym "vals")
                (gensym "structure") (gensym "next-fn")]]
     `(do
        (declare ~declared)
        (def ~name
          (let [nav# (reify RichNavigator
                       (~'rich-select* [this# ~@rargs]
                         (~select-exec ~declared ~@rargs))
                       (~'rich-transform* [this# ~@rargs]
                         (~transform-exec ~declared ~@rargs)))]

            (if (= ~num-params 0)
              (i/no-params-rich-compiled-path nav#)
              (i/->ParamsNeededPath nav# ~num-params))))))))


(defmacro providepath [name apath]
  `(let [comped# (i/comp-paths-internalized ~apath)
         expected-params# (i/num-needed-params ~name)
         needed-params# (i/num-needed-params comped#)]
     (if-not (= needed-params# expected-params#)
       (i/throw-illegal "Invalid number of params in provided path, expected "
                        expected-params# " but got " needed-params#))
     (def ~(declared-name name)
       (i/extract-rich-nav (i/coerce-compiled->rich-nav comped#)))))


(defmacro extend-protocolpath
  "Used in conjunction with `defprotocolpath`. See [[defprotocolpath]]."
  [protpath & extensions]
  `(i/extend-protocolpath* ~protpath ~(protpath-sym protpath) ~(vec extensions)))

;; copied from tools.macro to avoid the dependency
(defn ^:no-doc name-with-attributes
  "To be used in macro definitions.
  Handles optional docstrings and attribute maps for a name to be defined
  in a list of macro arguments. If the first macro argument is a string,
  it is added as a docstring to name and removed from the macro argument
  list. If afterwards the first macro argument is a map, its entries are
  added to the name's metadata map and the map is removed from the
  macro argument list. The return value is a vector containing the name
  with its extended metadata map and the list of unprocessed macro
  arguments."
  [name macro-args]
  (let [[docstring macro-args] (if (string? (first macro-args))
                                 [(first macro-args) (next macro-args)]
                                 [nil macro-args])
        [attr macro-args]          (if (map? (first macro-args))
                                     [(first macro-args) (next macro-args)]
                                     [{} macro-args])
        attr                       (if docstring
                                     (assoc attr :doc docstring)
                                     attr)
        attr                       (if (meta name)
                                     (conj (meta name) attr)
                                     attr)]
    [(with-meta name attr) macro-args]))

(defmacro defpathedfn
  "Defines a higher order navigator that itself takes in one or more paths
  as input. This macro is generally used in conjunction with [[fixed-pathed-nav]]
  or [[variable-pathed-nav]]. When inline factoring is applied to a path containing
  one of these higher order navigators, it will automatically interepret all
  arguments as paths, factor them accordingly, and set up the callsite to
  provide the parameters dynamically. Use ^:notpath metadata on arguments
  to indicate non-path arguments that should not be factored – note that in order
  to be inline factorable, these arguments must be statically resolvable (e.g. a
    top level var). See `transformed` for an example."
  [name & args]
  (let [[name args] (name-with-attributes name args)
        name (vary-meta name assoc :pathedfn true)]
    `(defn ~name ~@args)))

(defmacro defnavconstructor [name & args]
  (let [[name [[csym anav] & body-or-bodies]] (name-with-attributes name args)
        bodies (if (-> body-or-bodies first vector?) [body-or-bodies] body-or-bodies)

        checked-code
        (doall
         (for [[args & body] bodies]
           `(~args
             (let [ret# (do ~@body)]
               (if (i/layered-nav? ret#)
                 (i/layered-nav-underlying ret#)
                 (i/throw-illegal "Expected result navigator '" (quote ~anav)
                                  "' from nav constructor '" (quote ~name) "'"
                                  " constructed with the provided constructor '" (quote ~csym)
                                  "'"))))))]

    `(def ~name
       (vary-meta
        (let [~csym (i/layered-wrapper ~anav)]
          (fn ~@checked-code))
        assoc :layerednav (or (-> ~anav meta :highernav :type) :rich)))))




(defn ^:no-doc ic-prepare-path [locals-set path]
  (cond
    (vector? path)
    (mapv #(ic-prepare-path locals-set %) path)

    (symbol? path)
    (if (contains? locals-set path)
      `(com.rpl.specter.impl/->LocalSym ~path (quote ~path))
      ;; var-get doesn't work in cljs, so capture the val in the macro instead
      `(com.rpl.specter.impl/->VarUse ~path (var ~path) (quote ~path)))


    (i/fn-invocation? path)
    (let [[op & params] path]
      ;; need special case for 'fn since macroexpand does NOT
      ;; expand fn when run on cljs code, but it's also not considered a special symbol
      (if (or (= 'fn op) (special-symbol? op))
        `(com.rpl.specter.impl/->SpecialFormUse ~path (quote ~path))
        `(com.rpl.specter.impl/->FnInvocation
          ~(ic-prepare-path locals-set op)
          ~(mapv #(ic-prepare-path locals-set %) params)
          (quote ~path))))


    :else
    `(quote ~path)))


(defn ^:no-doc ic-possible-params [path]
  (do
    (mapcat
     (fn [e]
       (cond (or (set? e)
                 (map? e) ; in case inline maps are ever extended
                 (and (i/fn-invocation? e) (contains? #{'fn* 'fn} (first e))))
             [e]

             (i/fn-invocation? e)
             ;; the [e] here handles nav constructors
             (concat [e] (rest e) (ic-possible-params e))

             (vector? e)
             (ic-possible-params e)))

     path)))


(defn cljs-macroexpand [env form]
  (let [expand-fn (i/cljs-analyzer-macroexpand-1)
        mform (expand-fn env form)]
    (cond (identical? form mform) mform
          (and (seq? mform) (#{'js*} (first mform))) form
          :else (cljs-macroexpand env mform))))

(defn cljs-macroexpand-all* [env form]
  (if (and (seq? form)
           (#{'fn 'fn* 'cljs.core/fn} (first form)))
    form
    (let [expanded (if (seq? form) (cljs-macroexpand env form) form)]
      (cljwalk/walk #(cljs-macroexpand-all* env %) identity expanded))))


(defn cljs-macroexpand-all [env form]
  (let [ret (cljs-macroexpand-all* env form)]
    ret))


;; still possible to mess this up with alter-var-root
(defmacro path
  "Same as calling comp-paths, except it caches the composition of the static part
  of the path for later re-use (when possible). For almost all idiomatic uses
  of Specter provides huge speedup. This macro is automatically used by the
  select/transform/setval/replace-in/etc. macros."
  [& path]
  (let [;;this is a hack, but the composition of &env is considered stable for cljs
        platform (if (contains? &env :locals) :cljs :clj)
        local-syms (if (= platform :cljs)
                     (-> &env :locals keys set) ;cljs
                     (-> &env keys set)) ;clj

        used-locals-cell (i/mutable-cell [])
        _ (cljwalk/postwalk
           (fn [e]
             (if (local-syms e)
               (i/update-cell! used-locals-cell #(conj % e))
               e))

           path)
        used-locals (i/get-cell used-locals-cell)

        ;; note: very important to use riddley's macroexpand-all here, so that
        ;; &env is preserved in any potential nested calls to select (like via
        ;; a view function)
        expanded (if (= platform :clj)
                   (i/clj-macroexpand-all (vec path))
                   (cljs-macroexpand-all &env (vec path)))

        prepared-path (ic-prepare-path local-syms expanded)
        possible-params (vec (ic-possible-params expanded))

        ;; - with invokedynamic here, could go directly to the code
        ;; to invoke and/or parameterize the precompiled path without
        ;; a bunch of checks beforehand
        cache-sym (vary-meta
                   (gensym "pathcache")
                   assoc :cljs.analyzer/no-resolve true)

        info-sym (gensym "info")

        get-cache-code (if (= platform :clj)
                         `(try (i/get-cell ~cache-sym)
                               (catch ClassCastException e#
                                 (if (bound? (var ~cache-sym))
                                   (throw e#)
                                   (do
                                     (alter-var-root
                                      (var ~cache-sym)
                                      (fn [_#] (i/mutable-cell)))
                                     nil))))

                         cache-sym)

        add-cache-code (if (= platform :clj)
                         `(i/set-cell! ~cache-sym ~info-sym)
                         `(def ~cache-sym ~info-sym))


        precompiled-sym (gensym "precompiled")
        params-maker-sym (gensym "params-maker")

        handle-params-code
        (if (= platform :clj)
          `(i/bind-params* ~precompiled-sym (~params-maker-sym ~@used-locals) 0)
          `(i/handle-params
            ~precompiled-sym
            ~params-maker-sym
            ~(mapv (fn [p] `(fn [] ~p)) possible-params)))]


    (if (= platform :clj)
      (i/intern* *ns* cache-sym (i/mutable-cell)))
    `(let [info# ~get-cache-code

           ^com.rpl.specter.impl.CachedPathInfo info#
           (if (nil? info#)
             (let [~info-sym (i/magic-precompilation
                              ~prepared-path
                              ~(str *ns*)
                              (quote ~used-locals)
                              (quote ~possible-params))]

               ~add-cache-code
               ~info-sym)

             info#)


           ~precompiled-sym (.-precompiled info#)
           ~params-maker-sym (.-params-maker info#)]
       (if (nil? ~precompiled-sym)
         (i/comp-paths* ~(if (= (count path) 1) (first path) (vec path)))
         (if (nil? ~params-maker-sym)
           ~precompiled-sym
           ~handle-params-code)))))




(defmacro select
  "Navigates to and returns a sequence of all the elements specified by the path.
  This macro will attempt to do inline factoring and caching of the path, falling
  back to compiling the path on every invocation if it's not possible to
  factor/cache the path."
  [apath structure]
  `(i/compiled-select* (path ~apath) ~structure))

(defmacro select-one!
  "Returns exactly one element, throws exception if zero or multiple elements found.
  This macro will attempt to do inline factoring and caching of the path, falling
  back to compiling the path on every invocation if it's not possible to
  factor/cache the path."
  [apath structure]
  `(i/compiled-select-one!* (path ~apath) ~structure))

(defmacro select-one
  "Like select, but returns either one element or nil. Throws exception if multiple elements found.
  This macro will attempt to do inline factoring and caching of the path, falling
  back to compiling the path on every invocation if it's not possible to
  factor/cache the path."
  [apath structure]
  `(i/compiled-select-one* (path ~apath) ~structure))

(defmacro select-first
  "Returns first element found.
  This macro will attempt to do inline factoring and caching of the path, falling
  back to compiling the path on every invocation if it's not possible to
  factor/cache the path."
  [apath structure]
  `(i/compiled-select-first* (path ~apath) ~structure))

(defmacro select-any
  "Returns any element found or [[NONE]] if nothing selected. This is the most
  efficient of the various selection operations.
  This macro will attempt to do inline factoring and caching of the path, falling
  back to compiling the path on every invocation if it's not possible to
  factor/cache the path."
  [apath structure]
  `(i/compiled-select-any* (path ~apath) ~structure))

(defmacro selected-any?
  "Returns true if any element was selected, false otherwise.
  This macro will attempt to do inline factoring and caching of the path, falling
  back to compiling the path on every invocation if it's not possible to
  factor/cache the path."
  [apath structure]
  `(i/compiled-selected-any?* (path ~apath) ~structure))

(defmacro transform
  "Navigates to each value specified by the path and replaces it by the result of running
  the transform-fn on it.
  This macro will attempt to do inline factoring and caching of the path, falling
  back to compiling the path on every invocation if it's not possible to
  factor/cache the path."
  [apath transform-fn structure]
  `(i/compiled-transform* (path ~apath) ~transform-fn ~structure))

(defmacro multi-transform
  "Just like `transform` but expects transform functions to be specified
  inline in the path using `terminal`. Error is thrown if navigation finishes
  at a non-`terminal` navigator. `terminal-val` is a wrapper around `terminal` and is
  the `multi-transform` equivalent of `setval`.
  This macro will attempt to do inline factoring and caching of the path, falling
  back to compiling the path on every invocation if it's not possible to
  factor/cache the path."
  [apath structure]
  `(i/compiled-multi-transform* (path ~apath) ~structure))


(defmacro setval
  "Navigates to each value specified by the path and replaces it by `aval`.
  This macro will attempt to do inline factoring and caching of the path, falling
  back to compiling the path on every invocation if it's not possible to
  factor/cache the path."
  [apath aval structure]
  `(i/compiled-setval* (path ~apath) ~aval ~structure))

(defmacro traverse
  "Return a reducible object that traverses over `structure` to every element
  specified by the path.
  This macro will attempt to do inline factoring and caching of the path, falling
  back to compiling the path on every invocation if it's not possible to
  factor/cache the path."
  [apath structure]
  `(i/do-compiled-traverse (path ~apath) ~structure))

(defmacro replace-in
  "Similar to transform, except returns a pair of [transformed-structure sequence-of-user-ret].
  The transform-fn in this case is expected to return [ret user-ret]. ret is
  what's used to transform the data structure, while user-ret will be added to the user-ret sequence
  in the final return. replace-in is useful for situations where you need to know the specific values
  of what was transformed in the data structure.
  This macro will attempt to do inline factoring and caching of the path, falling
  back to compiling the path on every invocation if it's not possible to
  factor/cache the path."
  [apath transform-fn structure & args]
  `(i/compiled-replace-in* (path ~apath) ~transform-fn ~structure ~@args))

(defmacro collected?
  "Creates a filter function navigator that takes in all the collected values
  as input. For arguments, can use `(collected? [a b] ...)` syntax to look
  at each collected value as individual arguments, or `(collected? v ...)` syntax
  to capture all the collected values as a single vector."
  [params & body]
  (let [platform (if (contains? &env :locals) :cljs :clj)]
    `(i/collected?* (~'fn [~params] ~@body))))
