(ns com.rpl.specter.macros
  (:require [com.rpl.specter.impl :as i]
            [clojure.walk :as cljwalk])
  )

(defn ^:no-doc gensyms [amt]
  (vec (repeatedly amt gensym)))

(defn ^:no-doc determine-params-impls [[name1 & impl1] [name2 & impl2]]
  (if-not (= #{name1 name2} #{'select* 'transform*})
    (i/throw-illegal "defpath must implement select* and transform*, instead got "
      name1 " and " name2))
  (if (= name1 'select*)
    [impl1 impl2]
    [impl2 impl1]))


(def ^:no-doc PARAMS-SYM (vary-meta (gensym "params") assoc :tag 'objects))
(def ^:no-doc PARAMS-IDX-SYM (gensym "params-idx"))

(defn ^:no-doc paramsnav* [bindings num-params [impl1 impl2]]
  (let [[[[_ s-structure-sym s-next-fn-sym] & select-body]
         [[_ t-structure-sym t-next-fn-sym] & transform-body]]
         (determine-params-impls impl1 impl2)]
    (if (= 0 num-params)
      `(i/no-params-compiled-path
         (i/->TransformFunctions
           i/LeanPathExecutor
           (fn [~s-structure-sym ~s-next-fn-sym]
             ~@select-body)
           (fn [~t-structure-sym ~t-next-fn-sym]
             ~@transform-body)
           ))
      `(i/->ParamsNeededPath
         (i/->TransformFunctions
           i/RichPathExecutor
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
         ))))

(defn ^:no-doc paramscollector* [post-bindings num-params [_ [_ structure-sym] & body]]
  `(let [collector# (fn [~PARAMS-SYM ~PARAMS-IDX-SYM vals# ~structure-sym next-fn#]
                      (let [~@post-bindings ~@[] ; to avoid syntax highlighting issues
                            c# (do ~@body)]
                        (next-fn#                                    
                          ~PARAMS-SYM
                          (+ ~PARAMS-IDX-SYM ~num-params)
                          (conj vals# c#)
                          ~structure-sym)                     
                        ))]
     (i/->ParamsNeededPath
       (i/->TransformFunctions
         i/RichPathExecutor
         collector#
         collector# )
       ~num-params
       )))

(defn ^:no-doc pathed-nav* [builder paths-seq latefns-sym pre-bindings post-bindings impls]
  (let [num-params-sym (gensym "num-params")]
    `(let [paths# (map i/comp-paths* ~paths-seq)
           needed-params# (map i/num-needed-params paths#)
           offsets# (cons 0 (reductions + needed-params#))
           any-params-needed?# (->> paths#
                                    (filter i/params-needed-path?)
                                    empty?
                                    not)
           ~num-params-sym (last offsets#)
           ~latefns-sym (map
                          (fn [o# p#]
                            (if (i/compiled-path? p#)
                              (fn [params# params-idx#]
                                p# )
                              (fn [params# params-idx#]
                                (i/bind-params* p# params# (+ params-idx# o#))
                                )))
                          offsets#
                          paths#)
           ~@pre-bindings
           ret# ~(builder post-bindings num-params-sym impls)
           ]
    (if (not any-params-needed?#)
      (i/bind-params* ret# nil 0)
      ret#
      ))))

(defn ^:no-doc make-param-retrievers [params]
  (->> params
       (map-indexed
         (fn [i p]
           [p `(aget ~PARAMS-SYM
                     (+ ~PARAMS-IDX-SYM ~i))]
           ))
       (apply concat)))


(defmacro nav
  "Defines a navigator with late bound parameters. This navigator can be precompiled
  with other navigators without knowing the parameters. When precompiled with other
  navigators, the resulting path takes in parameters for all navigators in the path
  that needed parameters (in the order in which they were declared)."
  [params impl1 impl2]
  (let [num-params (count params)
        retrieve-params (make-param-retrievers params)]
    (paramsnav* retrieve-params num-params [impl1 impl2])
    ))

(defmacro paramsfn [params [structure-sym] & impl]
  `(nav ~params
     (~'select* [this# structure# next-fn#]
       (let [afn# (fn [~structure-sym] ~@impl)]
         (i/filter-select afn# structure# next-fn#)
         ))
     (~'transform* [this# structure# next-fn#]
       (let [afn# (fn [~structure-sym] ~@impl)]
         (i/filter-transform afn# structure# next-fn#)
         ))))

(defmacro paramscollector
  "Defines a Collector with late bound parameters. This collector can be precompiled
  with other selectors without knowing the parameters. When precompiled with other
  selectors, the resulting selector takes in parameters for all selectors in the path
  that needed parameters (in the order in which they were declared).
   "
  [params impl]
  (let [num-params (count params)
        retrieve-params (make-param-retrievers params)]
    (paramscollector* retrieve-params num-params impl)
    ))

(defmacro defnav [name & body]
  `(def ~name (nav ~@body)))

(defmacro defcollector [name & body]
  `(def ~name (paramscollector ~@body)))

(defmacro fixed-pathed-nav
  "This helper is used to define navigators that take in a fixed number of other
   paths as input. Those paths may require late-bound params, so this helper
   will create a parameterized navigator if that is the case. If no late-bound params
   are required, then the result is executable."
  [bindings impl1 impl2]
  (let [bindings (partition 2 bindings)
        paths (mapv second bindings)
        names (mapv first bindings)
        latefns-sym (gensym "latefns")
        latefn-syms (vec (gensyms (count paths)))]
    (pathed-nav*
      paramsnav*
      paths
      latefns-sym
      [latefn-syms latefns-sym]
      (mapcat (fn [n l] [n `(~l ~PARAMS-SYM ~PARAMS-IDX-SYM)]) names latefn-syms)
      [impl1 impl2])))

(defmacro variable-pathed-nav
  "This helper is used to define navigators that take in a variable number of other
   paths as input. Those paths may require late-bound params, so this helper
   will create a parameterized navigator if that is the case. If no late-bound params
   are required, then the result is executable."
  [[latepaths-seq-sym paths-seq] impl1 impl2]
  (let [latefns-sym (gensym "latefns")]
    (pathed-nav*
      paramsnav*
      paths-seq
      latefns-sym
      []
      [latepaths-seq-sym `(map (fn [l#] (l# ~PARAMS-SYM ~PARAMS-IDX-SYM))
                               ~latefns-sym)]
      [impl1 impl2]
      )))

(defmacro pathed-collector
  "This helper is used to define collectors that take in a single selector
   paths as input. That path may require late-bound params, so this helper
   will create a parameterized selector if that is the case. If no late-bound params
   are required, then the result is executable."
  [[name path] impl]
  (let [latefns-sym (gensym "latefns")
        latefn (gensym "latefn")]
    (pathed-nav*
      paramscollector*
      [path]
      latefns-sym
      [[latefn] latefns-sym]
      [name `(~latefn ~PARAMS-SYM ~PARAMS-IDX-SYM)]
      impl
      )
    ))

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
          retrieve `(~m ~ssym)
          ]
      `(do
          (defprotocol ~prot-name (~m [structure#]))
          (def ~name
            (if (= ~num-params 0)
              (i/no-params-compiled-path
                (i/->TransformFunctions
                  i/RichPathExecutor
                  (fn ~rargs
                    (let [path# ~retrieve
                          selector# (i/compiled-selector path#)]
                      (selector# ~@rargs)
                      ))
                  (fn ~rargs
                    (let [path# ~retrieve
                          transformer# (i/compiled-transformer path#)]
                      (transformer# ~@rargs)
                      ))))
              (i/->ParamsNeededPath
                (i/->TransformFunctions
                  i/RichPathExecutor
                  (fn ~rargs
                    (let [path# ~retrieve
                          selector# (i/params-needed-selector path#)]
                      (selector# ~@rargs)
                      ))
                  (fn ~rargs
                    (let [path# ~retrieve
                          transformer# (i/params-needed-transformer path#)]
                      (transformer# ~@rargs)
                      )))
                ~num-params
                )
              ))))))


(defn ^:no-doc declared-name [name]
  (vary-meta (symbol (str name "-declared"))
             assoc :no-doc true))

(defmacro declarepath
  ([name]
    `(declarepath ~name []))
  ([name params]
    (let [num-params (count params)
          declared (declared-name name)
          rargs [(gensym "params") (gensym "pidx") (gensym "vals")
                 (gensym "structure") (gensym "next-fn")]]
      `(do
         (declare ~declared)
         (def ~name
           (if (= ~num-params 0)
             (i/no-params-compiled-path
               (i/->TransformFunctions
                i/RichPathExecutor
                (fn ~rargs
                  (let [selector# (i/compiled-selector ~declared)]
                    (selector# ~@rargs)
                    ))
                (fn ~rargs
                  (let [transformer# (i/compiled-transformer ~declared)]
                    (transformer# ~@rargs)
                    ))))
             (i/->ParamsNeededPath
               (i/->TransformFunctions
                 i/RichPathExecutor
                 (fn ~rargs
                   (let [selector# (i/params-needed-selector ~declared)]
                     (selector# ~@rargs)
                     ))
                 (fn ~rargs
                   (let [transformer# (i/params-needed-transformer ~declared)]
                     (transformer# ~@rargs)
                     )))
               ~num-params
               )
           ))))))

(defmacro providepath [name apath]
  `(let [comped# (i/comp-paths* ~apath)
         expected-params# (i/num-needed-params ~name)
         needed-params# (i/num-needed-params comped#)]
     (if-not (= needed-params# expected-params#)
       (i/throw-illegal "Invalid number of params in provided path, expected "
           expected-params# " but got " needed-params#))
     (def ~(declared-name name)
       (update-in comped#
                  [:transform-fns]
                  i/coerce-tfns-rich)
       )))

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
                      "'"))
                 ))))]
    `(def ~name
       (vary-meta
         (let [~csym (i/layered-wrapper ~anav)]
           (fn ~@checked-code))
         assoc :layerednav true))
     ))
  

(defn ^:no-doc ic-prepare-path [locals-set path]
  (cond
    (vector? path)
    (mapv #(ic-prepare-path locals-set %) path)

    (symbol? path)
    (if (contains? locals-set path)
      `(com.rpl.specter.impl/->LocalSym ~path (quote ~path))
      ;; var-get doesn't work in cljs, so capture the val in the macro instead
      `(com.rpl.specter.impl/->VarUse ~path (var ~path) (quote ~path))
      )

    (i/fn-invocation? path)
    (let [[op & params] path]
      ;; need special case for 'fn since macroexpand does NOT 
      ;; expand fn when run on cljs code, but it's also not considered a special symbol
      (if (or (= 'fn op) (special-symbol? op))
        `(com.rpl.specter.impl/->SpecialFormUse ~path (quote ~path))
        `(com.rpl.specter.impl/->FnInvocation
           ~(ic-prepare-path locals-set op)
           ~(mapv #(ic-prepare-path locals-set %) params)
           (quote ~path)))
      )

    :else
    `(quote ~path)
    ))

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
              (ic-possible-params e)
              ))
      path
      )))

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
                    (-> &env keys set) ;clj
                    )
        used-locals-cell (i/mutable-cell [])
        _ (cljwalk/postwalk
            (fn [e]
              (if (local-syms e)
                (i/update-cell! used-locals-cell #(conj % e))
                e
                ))
            path)
        used-locals (i/get-cell used-locals-cell)

        ;; note: very important to use riddley's macroexpand-all here, so that
        ;; &env is preserved in any potential nested calls to select (like via
        ;; a view function)
        expanded (i/do-macroexpand-all (vec path))
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
                         `(i/get-cell ~cache-sym)
                         cache-sym
                         )
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
             ~(mapv (fn [p] `(fn [] ~p)) possible-params)
             ))
        ]
    (if (= platform :clj)
      (i/intern* *ns* cache-sym (i/mutable-cell)))
    `(let [info# ~get-cache-code
           
           ^com.rpl.specter.impl.CachedPathInfo info#
            (if (nil? info#)
              (let [~info-sym (i/magic-precompilation
                               ~prepared-path
                               ~(str *ns*)
                               (quote ~used-locals)
                               (quote ~possible-params)
                               )]
                ~add-cache-code
                ~info-sym
                )
              info#
              )

           ~precompiled-sym (.-precompiled info#)
           ~params-maker-sym (.-params-maker info#)]
       (if (nil? ~precompiled-sym)
         (i/comp-paths* ~(if (= (count path) 1) (first path) (vec path)))
         (if (nil? ~params-maker-sym)
           ~precompiled-sym
           ~handle-params-code
           )
         ))
  ))

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

