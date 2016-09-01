(ns com.rpl.specter.macros
  (:use [com.rpl.specter.protocols :only [RichNavigator]])
  (:require [com.rpl.specter.impl :as i]
            [clojure.walk :as cljwalk]))

(defn ^:no-doc determine-params-impls [impls]
  (let [grouped (->> impls (map (fn [[n & body]] [n body])) (into {}))]
    (if-not (= #{'select* 'transform*} (-> grouped keys set))
      (i/throw-illegal "defnav must implement select* and transform*, instead got "
                       (keys grouped)))
    grouped))


(defmacro richnav [params & impls]
  (if (empty? params)
    `(reify RichNavigator ~@impls)
    `(fn ~params
       (reify RichNavigator
         ~@impls))))


(defmacro nav [params & impls]
  (let [{[[_ s-structure-sym s-next-fn-sym] & s-body] 'select*
         [[_ t-structure-sym t-next-fn-sym] & t-body] 'transform*} (determine-params-impls impls)]
    `(richnav ~params
       (~'select* [this# vals# ~s-structure-sym next-fn#]
         (let [~s-next-fn-sym (fn [s#] (next-fn# vals# s#))]
           ~@s-body))
       (~'transform* [this# vals# ~t-structure-sym next-fn#]
         (let [~t-next-fn-sym (fn [s#] (next-fn# vals# s#))]
           ~@t-body)))))

(defmacro collector [params [_ [_ structure-sym] & body]]
  `(richnav ~params
     (~'select* [this# vals# ~structure-sym next-fn#]
      (next-fn# (conj vals# (do ~@body)) ~structure-sym))
     (~'transform* [this# vals# ~structure-sym next-fn#]
       (next-fn# (conj vals# (do ~@body)) ~structure-sym))))

(defn- helper-name [name method-name]
  (symbol (str name "-" method-name)))

(defmacro defnav [name params & impls]
  ;; remove the "this" param for the helper
  (let [helpers (for [[mname [_ & mparams] & mbody] impls]
                  `(defn ~(helper-name name mname) [~@params ~@mparams] ~@mbody))
        decls (for [[mname & _] impls]
                `(declare ~(helper-name name mname)))]
    `(do
       ~@decls
       ~@helpers
       (def ~name (nav ~params ~@impls)))))

(defmacro defrichnav [name params & impls]
  `(def ~name (richnav ~params ~@impls)))

(defmacro defcollector [name & body]
  `(def ~name (collector ~@body)))


(defn- late-bound-operation [bindings builder-op impls]
  (let [bindings (partition 2 bindings)
        params (map first bindings)
        curr-params (map second bindings)]
    `(let [builder# (~builder-op [~@params] ~@impls)
           curr-params# [~@curr-params]]
       (if (every? (complement i/dynamic-param?) curr-params#)
         (apply builder# curr-params#)
         ;;TODO: should tag with metadata that the return is a direct navigator
         (com.rpl.specter.impl/->DynamicFunction builder# curr-params#)))))

(defmacro late-bound-nav [bindings & impls]
  (late-bound-operation bindings `nav impls))

(defmacro late-bound-collector [bindings impl]
  (late-bound-operation bindings `collector [impl]))

(defmacro late-bound-richnav [bindings & impls]
  (late-bound-operation bindings `richnav impls))

(defn- protpath-sym [name]
  (-> name (str "-prot") symbol))


;;TODO: redesign so can still have parameterized protpaths...
;;TODO: mainly need recursion
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
         rargs [(gensym "vals") ssym (gensym "next-fn")]
         retrieve `(~m ~ssym)]

     `(do
        (defprotocol ~prot-name (~m [structure#]))
        (let [nav# (reify RichNavigator
                     (~'select* [this# ~@rargs]
                       (let [inav# ~retrieve]
                         (i/exec-select* inav# ~@rargs)))

                     (~'rich-transform* [this# ~@rargs]
                       (let [inav# ~retrieve]
                         (i/exec-transform* inav# ~@rargs))))]

          (def ~name
            (if (= ~num-params 0)
              (i/no-params-rich-compiled-path nav#)
              (i/->ParamsNeededPath nav# ~num-params))))))))





(defn ^:no-doc declared-name [name]
  (vary-meta (symbol (str name "-declared"))
             assoc :no-doc true))

;;TODO: redesign so can be recursive
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

(defmacro dynamicnav [& args]
  `(vary-meta (fn ~@args) assoc :dynamicnav true))

(defmacro defdynamicnav
  "Defines a higher order navigator that itself takes in one or more paths
  as input. When inline caching is applied to a path containing
  one of these higher order navigators, it will apply inline caching and
  compilation to the subpaths as well. Use ^:notpath metadata on arguments
  to indicate non-path arguments that should not be compiled"
  [name & args]
  (let [[name args] (name-with-attributes name args)]
    `(def ~name (dynamicnav ~@args))))


(defn- used-locals [locals-set form]
  (let [used-locals-cell (i/mutable-cell [])]
    (cljwalk/postwalk
     (fn [e]
       (if (locals-set e)
         (i/update-cell! used-locals-cell #(conj % e))
         e))
     form)
    (i/get-cell used-locals-cell)))

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
    (if (empty? (used-locals locals-set path))
      path
      `(com.rpl.specter.impl/->DynamicVal ~path (quote ~path)))))


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


(defmacro path
  "Same as calling comp-paths, except it caches the composition of the static parts
  of the path for later re-use (when possible). For almost all idiomatic uses
  of Specter provides huge speedup. This macro is automatically used by the
  select/transform/setval/replace-in/etc. macros."
  [& path]
  (let [;;this is a hack, but the composition of &env is considered stable for cljs
        platform (if (contains? &env :locals) :cljs :clj)
        local-syms (if (= platform :cljs)
                     (-> &env :locals keys set) ;cljs
                     (-> &env keys set)) ;clj

        used-locals (used-locals local-syms path)

        ;; note: very important to use riddley's macroexpand-all here, so that
        ;; &env is preserved in any potential nested calls to select (like via
        ;; a view function)
        expanded (if (= platform :clj)
                   (i/clj-macroexpand-all (vec path))
                   (cljs-macroexpand-all &env (vec path)))

        prepared-path (ic-prepare-path local-syms expanded)
        ; possible-params (vec (ic-possible-params expanded))

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

        ;;TODO: redo clojurescript portions
        handle-params-code
        (if (= platform :clj)
          `(~precompiled-sym ~@used-locals))]
          ; `(i/handle-params
          ;   ~precompiled-sym
          ;   ~params-maker-sym
          ;   ~(mapv (fn [p] `(fn [] ~p)) possible-params)))]


    (if (= platform :clj)
      (i/intern* *ns* cache-sym (i/mutable-cell)))
    `(let [info# ~get-cache-code

           ^com.rpl.specter.impl.CachedPathInfo info#
           (if (nil? info#)
             (let [~info-sym (i/magic-precompilation
                              ~prepared-path
                              ~(str *ns*)
                              (quote ~used-locals))]
               ~add-cache-code
               ~info-sym)
             info#)


           ~precompiled-sym (.-precompiled info#)
           dynamic?# (.-dynamic? info#)]
       (if dynamic?#
         ~handle-params-code
         ~precompiled-sym))))




(defmacro select
  "Navigates to and returns a sequence of all the elements specified by the path.
  This macro will do inline caching of the path."
  [apath structure]
  `(i/compiled-select* (path ~apath) ~structure))

(defmacro select-one!
  "Returns exactly one element, throws exception if zero or multiple elements found.
   This macro will do inline caching of the path."
  [apath structure]
  `(i/compiled-select-one!* (path ~apath) ~structure))

(defmacro select-one
  "Like select, but returns either one element or nil. Throws exception if multiple elements found.
   This macro will do inline caching of the path."
  [apath structure]
  `(i/compiled-select-one* (path ~apath) ~structure))

(defmacro select-first
  "Returns first element found.
   This macro will do inline caching of the path."
  [apath structure]
  `(i/compiled-select-first* (path ~apath) ~structure))

(defmacro select-any
  "Returns any element found or [[NONE]] if nothing selected. This is the most
  efficient of the various selection operations.
  This macro will do inline caching of the path."
  [apath structure]
  `(i/compiled-select-any* (path ~apath) ~structure))

(defmacro selected-any?
  "Returns true if any element was selected, false otherwise.
  This macro will do inline caching of the path."
  [apath structure]
  `(i/compiled-selected-any?* (path ~apath) ~structure))

(defmacro transform
  "Navigates to each value specified by the path and replaces it by the result of running
  the transform-fn on it.
  This macro will do inline caching of the path."
  [apath transform-fn structure]
  `(i/compiled-transform* (path ~apath) ~transform-fn ~structure))

(defmacro multi-transform
  "Just like `transform` but expects transform functions to be specified
  inline in the path using `terminal`. Error is thrown if navigation finishes
  at a non-`terminal` navigator. `terminal-val` is a wrapper around `terminal` and is
  the `multi-transform` equivalent of `setval`.
  This macro will do inline caching of the path."
  [apath structure]
  `(i/compiled-multi-transform* (path ~apath) ~structure))


(defmacro setval
  "Navigates to each value specified by the path and replaces it by `aval`.
  This macro will do inline caching of the path."
  [apath aval structure]
  `(i/compiled-setval* (path ~apath) ~aval ~structure))

(defmacro traverse
  "Return a reducible object that traverses over `structure` to every element
  specified by the path.
  This macro will do inline caching of the path."
  [apath structure]
  `(i/do-compiled-traverse (path ~apath) ~structure))

(defmacro replace-in
  "Similar to transform, except returns a pair of [transformed-structure sequence-of-user-ret].
  The transform-fn in this case is expected to return [ret user-ret]. ret is
  what's used to transform the data structure, while user-ret will be added to the user-ret sequence
  in the final return. replace-in is useful for situations where you need to know the specific values
  of what was transformed in the data structure.
  This macro will do inline caching of the path."
  [apath transform-fn structure & args]
  `(i/compiled-replace-in* (path ~apath) ~transform-fn ~structure ~@args))

(defmacro collected?
  "Creates a filter function navigator that takes in all the collected values
  as input. For arguments, can use `(collected? [a b] ...)` syntax to look
  at each collected value as individual arguments, or `(collected? v ...)` syntax
  to capture all the collected values as a single vector."
  [params & body]
  `(i/collected?* (~'fn [~params] ~@body)))
