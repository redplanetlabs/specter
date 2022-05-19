(ns com.rpl.specter
  #?(:cljs (:require-macros
            [com.rpl.specter
              :refer
              [late-bound-nav
               late-bound-richnav
               late-bound-collector
               defcollector
               defnav
               defdynamicnav
               dynamicnav
               richnav
               defrichnav
               recursive-path
               select
               transform
               setval
               select-any]]

            [com.rpl.specter.util-macros :refer
              [doseqres]]))
  ;; workaround for cljs bug that emits warnings for vars named the same as a
  ;; private var in cljs.core (in this case `NONE`, added as private var to
  ;; cljs.core with 1.9.562)
  #?(:cljs (:refer-clojure :exclude [NONE]))

  (:use [com.rpl.specter.protocols :only [ImplicitNav RichNavigator]]
    #?(:clj [com.rpl.specter.util-macros :only [doseqres]]))
  (:require [com.rpl.specter.impl :as i]
            [com.rpl.specter.navs :as n]
            #?(:clj [clojure.walk :as cljwalk])
            #?(:clj [com.rpl.specter.macros :as macros])
            [clojure.set :as set]))

(defn- static-path? [path]
  (if (sequential? path)
   (every? static-path? path)
   (-> path i/dynamic-param? not)
   ))

(defn wrap-dynamic-nav [f]
  (fn [& args]
    (let [ret (apply f args)]
      (cond (and (sequential? ret) (static-path? ret))
            (i/comp-paths* ret)

            (and (sequential? ret) (= 1 (count ret)))
            (first ret)

            :else
            ret
            ))))

#?(:clj
   (do

     (defmacro defmacroalias [name target]
       `(do
          (def ~name (var ~target))
          (alter-meta! (var ~name) merge {:macro true})))

     (defmacroalias richnav macros/richnav)
     (defmacroalias nav macros/nav)
     (defmacroalias defnav macros/defnav)
     (defmacroalias defrichnav macros/defrichnav)

     (defmacro collector [params [_ [_ structure-sym] & body]]
       `(richnav ~params
          (~'select* [this# vals# ~structure-sym next-fn#]
           (next-fn# (conj vals# (do ~@body)) ~structure-sym))
          (~'transform* [this# vals# ~structure-sym next-fn#]
            (next-fn# (conj vals# (do ~@body)) ~structure-sym))))

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
              (com.rpl.specter.impl/->DynamicFunction builder# curr-params# nil)))))

     (defmacro late-bound-nav [bindings & impls]
       (late-bound-operation bindings `nav impls))

     (defmacro late-bound-collector [bindings impl]
       (late-bound-operation bindings `collector [impl]))

     (defmacro late-bound-richnav [bindings & impls]
       (late-bound-operation bindings `richnav impls))

     (defmacro with-inline-debug [& body]
       `(binding [i/*DEBUG-INLINE-CACHING* true]
          ~@body))

     (defmacro declarepath [name]
       `(def ~name (i/local-declarepath)))

     (defmacro providepath [name apath]
       `(i/providepath* ~name (path ~apath)))

     (defmacro recursive-path [params self-sym path]
       (if (empty? params)
         `(let [~self-sym (i/local-declarepath)]
            (providepath ~self-sym ~path)
            ~self-sym)
         `(i/direct-nav-obj
            (fn ~params
              (let [~self-sym (i/local-declarepath)]
                (providepath ~self-sym ~path)
                ~self-sym)))))

     ;; copied from clojure.core
     (def
       ^{:private true}
       sigs
       (fn [fdecl]
         (let [asig
               (fn [fdecl]
                 (let [arglist (first fdecl)
                       ;; elide implicit macro args
                       arglist (if (= '&form (first arglist))
                                 (subvec arglist 2 (count arglist))
                                 arglist)
                       body (next fdecl)]
                   (if (map? (first body))
                     (if (next body)
                       (with-meta arglist (conj (if (meta arglist) (meta arglist) {}) (first body)))
                       arglist)
                     arglist)))]
           (if (seq? (first fdecl))
             (loop [ret [] fdecls fdecl]
               (if fdecls
                 (recur (conj ret (asig (first fdecls))) (next fdecls))
                 (seq ret)))
             (list (asig fdecl))))))

     ;; partially copied from clojure.core/defn
     (defn- name-with-attributes
       "To be used in macro definitions.
       Handles optional docstrings and attribute maps for a name to be defined
       in a list of macro arguments. If the first macro argument is a string,
       it is added as a docstring to name and removed from the macro argument
       list. If afterwards the first macro argument is a map, its entries are
       added to the name's metadata map and the map is removed from the
       macro argument list. The return value is a vector containing the name
       with its extended metadata map and the list of unprocessed macro
       arguments."
       [name fdecl]
       (let [m (if (string? (first fdecl))
                 {:doc (first fdecl)}
                 {})

             fdecl (if (string? (first fdecl))
                     (next fdecl)
                     fdecl)
             m (if (map? (first fdecl))
                 (conj m (first fdecl))
                 m)
             fdecl (if (map? (first fdecl))
                     (next fdecl)
                     fdecl)
             fdecl (if (vector? (first fdecl))
                     (list fdecl)
                     fdecl)
             m (if (map? (last fdecl))
                 (conj m (last fdecl))
                 m)
             fdecl (if (map? (last fdecl))
                     (butlast fdecl)
                     fdecl)
             m (conj {:arglists (list 'quote (sigs fdecl))} m)]
         [(with-meta name m) fdecl]))

     (defmacro dynamicnav [& args]
       `(vary-meta (wrap-dynamic-nav (fn ~@args)) assoc :dynamicnav true))

     (defmacro defdynamicnav
       "Defines a function that can choose what navigator to use at runtime based on
        the dynamic context. The arguments will either be static values or
        objects satisfying `dynamic-param?`. Use `late-bound-nav` to produce a runtime
        navigator that uses the values of the dynamic params. See `selected?` for
        an illustrative example of dynamic navs."
       [name & args]
       (let [[name args] (name-with-attributes name args)]
         `(def ~name (dynamicnav ~@args))))


     (defn- ic-prepare-path [locals-set path]
       (cond
         (vector? path)
         (mapv #(ic-prepare-path locals-set %) path)

         (symbol? path)
         (if (contains? locals-set path)
           (let [s (get locals-set path)
                 embed (i/maybe-direct-nav path (-> s meta :direct-nav))]
             `(com.rpl.specter.impl/->LocalSym ~path (quote ~embed)))
           ;; var-get doesn't work in cljs, so capture the val in the macro instead
           `(com.rpl.specter.impl/->VarUse
              ~path
              ~(if-not (instance? Class (resolve path)) `(var ~path))
              (quote ~path)))


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
         (if (empty? (i/used-locals locals-set path))
           path
           `(com.rpl.specter.impl/->DynamicVal (quote ~path)))))


     (defn- ic-possible-params [path]
       (do
         (mapcat
          (fn [e]
            (cond (or (set? e)
                      (map? e)
                      (symbol? e)
                      (and (i/fn-invocation? e)
                           (or (contains? #{'fn* 'fn} (first e))
                               (special-symbol? (first e)))))
                  [e]

                  (sequential? e)
                  (concat (if (vector? e) [e]) (ic-possible-params e))))


          path)))


     (defn- cljs-macroexpand [env form]
       (let [expand-fn (i/cljs-analyzer-macroexpand-1)
             mform (expand-fn env form)]
         (cond (identical? form mform) mform
               (and (seq? mform) (#{'js*} (first mform))) form
               :else (cljs-macroexpand env mform))))

     (defn- cljs-macroexpand-all* [env form]
       (if (and (seq? form)
                (#{'fn 'fn* 'cljs.core/fn} (first form)))
         form
         (let [expanded (if (seq? form) (cljs-macroexpand env form) form)]
           (cljwalk/walk #(cljs-macroexpand-all* env %) identity expanded))))


     (defn- cljs-macroexpand-all [env form]
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

             used-locals (i/used-locals local-syms path)

             ;; note: very important to use riddley's macroexpand-all here, so that
             ;; &env is preserved in any potential nested calls to select (like via
             ;; a view function)
             expanded (if (= platform :clj)
                        (i/clj-macroexpand-all (vec path))
                        (cljs-macroexpand-all &env (vec path)))

             prepared-path (ic-prepare-path local-syms expanded)
             possible-params (vec (ic-possible-params expanded))

             cache-sym (vary-meta
                        (gensym "pathcache")
                        merge {:cljs.analyzer/no-resolve true :no-doc true :private true})

             info-sym (gensym "info")

             get-cache-code (if (= platform :clj)
                              `(try (i/get-cell ~cache-sym)
                                    (catch ClassCastException e#
                                      ;; With AOT compilation it's possible for:
                                      ;; Thread 1: unbound, so throw exception
                                      ;; Thread 2: unbound, so throw exception
                                      ;; Thread 1: do alter-var-root
                                      ;; Thread 2: it's bound, so retrieve the current value
                                      (if (bound? (var ~cache-sym))
                                        (i/get-cell ~cache-sym)
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

             handle-params-code
             (if (= platform :clj)
               `(~precompiled-sym ~@used-locals)
               `(~precompiled-sym ~possible-params))]
         (if (= platform :clj)
           (i/intern* *ns* cache-sym (i/mutable-cell)))
         `(let [info# ~get-cache-code

                info#
                (if (nil? info#)
                  (let [~info-sym (i/magic-precompilation
                                   ~prepared-path
                                   ~(str *ns*)
                                   (quote ~used-locals)
                                   (quote ~possible-params))]
                    ~add-cache-code
                    ~info-sym)
                  info#)

                ~precompiled-sym (i/cached-path-info-precompiled info#)
                dynamic?# (i/cached-path-info-dynamic? info#)]
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

     (defmacro vtransform
       "Navigates to each value specified by the path and replaces it by the result of running
       the transform-fn on two arguments: the collected values as a vector, and the navigated value."
       [apath transform-fn structure]
       `(i/compiled-vtransform* (path ~apath) ~transform-fn ~structure))

     (defmacro multi-transform
       "Just like `transform` but expects transform functions to be specified
       inline in the path using `terminal` or `vterminal`. Error is thrown if navigation finishes
       at a non-terminal navigator. `terminal-val` is a wrapper around `terminal` and is
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

     (defmacro traverse-all
       "Returns a transducer that traverses over each element with the given path."
       [apath]
       `(i/compiled-traverse-all* (path ~apath)))

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


     (defn- protpath-sym [name]
       (-> name (str "-prot") symbol))

     (defn- protpath-meth-sym [name]
       (-> name (str "-retrieve") symbol))


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
              m (protpath-meth-sym name)
              num-params (count params)
              ssym (gensym "structure")
              rargs [(gensym "vals") ssym (gensym "next-fn")]
              retrieve `(~m ~ssym ~@params)]
          `(do
             (defprotocol ~prot-name (~m [structure# ~@params]))
             (defrichnav ~name ~params
                (~'select* [this# ~@rargs]
                  (let [inav# ~retrieve]
                    (i/exec-select* inav# ~@rargs)))
                (~'transform* [this# ~@rargs]
                  (let [inav# ~retrieve]
                    (i/exec-transform* inav# ~@rargs))))))))

     (defmacro satisfies-protpath? [protpath o]
       `(satisfies? ~(protpath-sym protpath) ~o))

     (defn extend-protocolpath* [protpath-prot extensions]
       (let [m (-> protpath-prot :sigs keys first)
             params (-> protpath-prot :sigs first last :arglists first)]
         (doseq [[atype path-code] extensions]
            (extend atype protpath-prot
              {m (binding [*compile-files* false]
                   (eval `(fn ~params (path ~path-code))))}))))

     (defmacro extend-protocolpath
       "Used in conjunction with `defprotocolpath`. See [[defprotocolpath]]."
       [protpath & extensions]
       (let [extensions (partition 2 extensions)
             embed (vec (for [[t p] extensions] [t `(quote ~p)]))]
         `(extend-protocolpath*
           ~(protpath-sym protpath)
           ~embed)))

    (defmacro end-fn [& args]
      `(n/->SrangeEndFunction (fn ~@args)))

    (defmacro subseq-pred-fn
      "Used in conjunction with `continuous-subseqs`.  See [[continuous-subseqs]]."
      [get-truthy-fn & args]
      `(i/->SubseqsDynamicPredFn ~get-truthy-fn (i/wrap-pred-with-index (fn ~@args))))

    ))



(defn comp-paths
  "Returns a compiled version of the given path for use with
   compiled-{select/transform/setval/etc.} functions."
  [& apath]
  (i/comp-paths* (vec apath)))

;; Selection functions

(def ^{:doc "Version of select that takes in a path precompiled with comp-paths"}
  compiled-select i/compiled-select*)

(defn select*
  "Navigates to and returns a sequence of all the elements specified by the path."
  [path structure]
  (compiled-select (i/comp-paths* path)
                   structure))

(def ^{:doc "Version of select-one that takes in a path precompiled with comp-paths"}
  compiled-select-one i/compiled-select-one*)

(defn select-one*
  "Like select, but returns either one element or nil. Throws exception if multiple elements found"
  [path structure]
  (compiled-select-one (i/comp-paths* path) structure))

(def ^{:doc "Version of select-one! that takes in a path precompiled with comp-paths"}
  compiled-select-one! i/compiled-select-one!*)

(defn select-one!*
  "Returns exactly one element, throws exception if zero or multiple elements found"
  [path structure]
  (compiled-select-one! (i/comp-paths* path) structure))

(def ^{:doc "Version of select-first that takes in a path precompiled with comp-paths"}
  compiled-select-first i/compiled-select-first*)


(defn select-first*
  "Returns first element found."
  [path structure]
  (compiled-select-first (i/comp-paths* path) structure))

(def ^{:doc "Version of select-any that takes in a path precompiled with comp-paths"}
  compiled-select-any i/compiled-select-any*)

(def ^{:doc "Global value used to indicate no elements selected during
             [[select-any]]."}
  NONE i/NONE)

(defn select-any*
  "Returns any element found or [[NONE]] if nothing selected. This is the most
   efficient of the various selection operations."
  [path structure]
  (compiled-select-any (i/comp-paths* path) structure))

(def ^{:doc "Version of selected-any? that takes in a path precompiled with comp-paths"}
  compiled-selected-any? i/compiled-selected-any?*)

(defn selected-any?*
  "Returns true if any element was selected, false otherwise."
  [path structure]
  (compiled-selected-any? (i/comp-paths* path) structure))

;; Reducible traverse functions

(def ^{:doc "Version of traverse that takes in a path precompiled with comp-paths"}
  compiled-traverse i/do-compiled-traverse)

(defn traverse*
  "Return a reducible object that traverses over `structure` to every element
   specified by the path"
  [apath structure]
  (compiled-traverse (i/comp-paths* apath) structure))

(def ^{:doc "Version of traverse-all that takes in a path precompiled with comp-paths"}
  compiled-traverse-all i/compiled-traverse-all*)

(defn traverse-all*
  "Returns a transducer that traverses over each element with the given path."
  [apath]
  (compiled-traverse-all (i/comp-paths* apath)))

;; Transformation functions

(def ^{:doc "Version of transform that takes in a path precompiled with comp-paths"}
  compiled-transform i/compiled-transform*)

(def ^{:doc "Version of vtransform that takes in a path precompiled with comp-paths"}
  compiled-vtransform i/compiled-vtransform*)


(defn transform*
  "Navigates to each value specified by the path and replaces it by the result of running
  the transform-fn on it"
  [path transform-fn structure]
  (compiled-transform (i/comp-paths* path) transform-fn structure))

(def ^{:doc "Version of `multi-transform` that takes in a path precompiled with `comp-paths`"}
  compiled-multi-transform i/compiled-multi-transform*)


(defn multi-transform*
  "Just like `transform` but expects transform functions to be specified
   inline in the path using `terminal` or `vterminal`. Error is thrown if navigation finishes
   at a non-terminal navigator. `terminal-val` is a wrapper around `terminal` and is
   the `multi-transform` equivalent of `setval`."
  [path structure]
  (compiled-multi-transform (i/comp-paths* path) structure))


(def ^{:doc "Version of setval that takes in a path precompiled with comp-paths"}
  compiled-setval i/compiled-setval*)

(defn setval*
  "Navigates to each value specified by the path and replaces it by val"
  [path val structure]
  (compiled-setval (i/comp-paths* path) val structure))

(def ^{:doc "Version of replace-in that takes in a path precompiled with comp-paths"}
  compiled-replace-in i/compiled-replace-in*)

(defn replace-in*
  "Similar to transform, except returns a pair of [transformed-structure sequence-of-user-ret].
   The transform-fn in this case is expected to return [ret user-ret]. ret is
   what's used to transform the data structure, while user-ret will be added to the user-ret sequence
   in the final return. replace-in is useful for situations where you need to know the specific values
   of what was transformed in the data structure."
  [path transform-fn structure & {:keys [merge-fn] :or {merge-fn concat}}]
  (compiled-replace-in (i/comp-paths* path) transform-fn structure :merge-fn merge-fn))

;; Helper for making late-bound navs

(def late-path i/late-path)
(def dynamic-param? i/dynamic-param?)
(def late-resolved-fn i/late-resolved-fn)


(defdynamicnav
  ^{:doc "Turns a navigator that takes one argument into a navigator that takes
          many arguments and uses the same navigator with each argument. There
          is no performance cost to using this. See implementation of `keypath`"}
  eachnav
  [navfn]
  (let [latenavfn (late-resolved-fn navfn)]
    (dynamicnav [& args]
      (map latenavfn args))))


;; Helpers for making recursive or mutually recursive navs

(def local-declarepath i/local-declarepath)

;; Built-in pathing and context operations

(defnav
  ^{:doc "Stops navigation at this point. For selection returns nothing and for
          transformation returns the structure unchanged"}
  STOP
  []
  (select* [this structure next-fn]
    NONE)
  (transform* [this structure next-fn]
    structure))



(def
  ^{:doc "Stays navigated at the current point. Essentially a no-op navigator."}
  STAY
  i/STAY*)

(def
  ^{:doc "Defines an endpoint in the navigation the transform function run. The transform
          function works just like it does in `transform`, with collected values
          given as the first arguments"}
  terminal
  (richnav [afn]
    (select* [this vals structure next-fn]
       NONE)
    (transform* [this vals structure next-fn]
      (i/terminal* afn vals structure))))

(def
  ^{:doc "Defines an endpoint in the navigation the transform function run.The transform
          function works differently than it does in `transform`. Rather than receive
          collected vals spliced in as the first arguments to the function, this function
          always takes two arguemnts. The first is all collected vals in a vector, and
          the second is the navigated value."}
  vterminal
  (richnav [afn]
    (select* [this vals structure next-fn]
      NONE)
    (transform* [this vals structure next-fn]
      (afn vals structure))))

(defn ^:direct-nav terminal-val
  "Like `terminal` but specifies a val to set at the location regardless of
   the collected values or the value at the location."
  [v]
  (terminal (i/fast-constantly v)))

(defnav
  ^{:doc "Navigate to every element of the collection. For maps navigates to
          a vector of `[key value]`."}
  ALL
  []
  (select* [this structure next-fn]
    (n/all-select structure next-fn))
  (transform* [this structure next-fn]
    (n/all-transform structure next-fn)))

(defnav
  ^{:doc "Same as ALL, except maintains metadata on the structure."}
  ALL-WITH-META
  []
  (select* [this structure next-fn]
    (n/all-select structure next-fn))
  (transform* [this structure next-fn]
    (let [m (meta structure)
          res (n/all-transform structure next-fn)]
      (if (some? res)
        (with-meta res m)
        ))))

(defnav
  ^{:doc "Navigate to each value of the map. This is more efficient than
          navigating via [ALL LAST]"}
  MAP-VALS
  []
  (select* [this structure next-fn]
    (doseqres NONE [v (vals structure)]
      (next-fn v)))
  (transform* [this structure next-fn]
    (n/map-vals-transform structure next-fn)))

(defnav
  ^{:doc "Navigate to each key of the map. This is more efficient than
          navigating via [ALL FIRST]"}
  MAP-KEYS
  []
  (select* [this structure next-fn]
    (doseqres NONE [k (keys structure)]
      (next-fn k)))
  (transform* [this structure next-fn]
    (n/map-keys-transform structure next-fn)))


(defcollector VAL []
  (collect-val [this structure]
    structure))

(def
  ^{:doc "Navigate to the last element of the collection. If the collection is
          empty navigation is stopped at this point."}
  LAST
  (n/PosNavigator n/get-last n/update-last))

(def
  ^{:doc "Navigate to the first element of the collection. If the collection is
          empty navigation is stopped at this point."}
  FIRST
  (n/PosNavigator n/get-first n/update-first))

(defnav
  ^{:doc "Uses start-index-fn and end-index-fn to determine the bounds of the subsequence
          to select when navigating. `start-index-fn` takes in the structure as input. `end-index-fn`
          can be one of two forms. If a regular function (e.g. defined with `fn`), it takes in only the structure as input. If a function defined using special `end-fn` macro, it takes in the structure and the result of `start-index-fn`."}
  srange-dynamic
  [start-index-fn end-index-fn]
  (select* [this structure next-fn]
    (let [s (start-index-fn structure)]
      (n/srange-select structure s (n/invoke-end-fn end-index-fn structure s) next-fn)))
  (transform* [this structure next-fn]
    (let [s (start-index-fn structure)]
      (n/srange-transform structure s (n/invoke-end-fn end-index-fn structure s) next-fn))))


(defnav
  ^{:doc "Navigates to the subsequence bound by the indexes start (inclusive)
          and end (exclusive)"}
  srange
  [start end]
  (select* [this structure next-fn]
    (n/srange-select structure start end next-fn))
  (transform* [this structure next-fn]
    (n/srange-transform structure start end next-fn)))


(defnav
  ^{:doc "Navigates to every continuous subsequence of elements matching `pred`. `pred` can be specified one of two
          forms. If a regular function (e.g. defined with `fn`), it takes in only the current element as input.  If
          defined using the special `subseq-pred-fn` macro, it takes in a `get-truthy-fn` as its first parameter,
          followed by arguments to a predicate function [`elem` `prev`], followed by the predicate function body.  The
          `elem` argument to the predicate function is the current element, and the `pred` argument is the value
          returned by your predicate on the previous element, so it can be in any structure you choose.  `get-truthy-fn`
          is a function that should return true from your predicate's return structure if that element should be
          included in a subsequence."}
  continuous-subseqs
  [pred]
  (select* [this structure next-fn]
    (doseqres NONE [[s e] (i/matching-ranges structure pred)]
      (n/srange-select structure s e next-fn)))
  (transform* [this structure next-fn]
    (i/continuous-subseqs-transform* pred structure next-fn)))


(defnav
  ^{:doc "Navigate to the empty subsequence before the first element of the collection."}
  BEGINNING
  []
  (select* [this structure next-fn]
    (next-fn (if (string? structure) "" [])))
  (transform* [this structure next-fn]
    (if (string? structure)
      (str (next-fn "") structure)
      (let [to-prepend (next-fn [])]
        (n/prepend-all structure to-prepend)))))


(defnav
 ^{:doc "Navigate to the empty subsequence after the last element of the collection."}
  END
  []
  (select* [this structure next-fn]
    (next-fn (if (string? structure) "" [])))
  (transform* [this structure next-fn]
    (if (string? structure)
      (str structure (next-fn ""))
      (let [to-append (next-fn [])]
        (n/append-all structure to-append)))))

(defnav
  ^{:doc "Navigate to 'void' elem in the set.
          For transformations - if result is not `NONE`,
          then add that value to the set."}
  NONE-ELEM
  []
  (select* [this structure next-fn]
    (next-fn NONE))
  (transform* [this structure next-fn]
    (let [newe (next-fn NONE)]
      (if (identical? NONE newe)
        structure
        (if (nil? structure)
          #{newe}
          (conj structure newe)
          )))))

(defnav
  ^{:doc "Navigate to 'void' element before the sequence.
          For transformations – if result is not `NONE`,
          then prepend that value."}
  BEFORE-ELEM
  []
  (select* [this structure next-fn]
    (next-fn NONE))
  (transform* [this structure next-fn]
    (let [newe (next-fn NONE)]
      (if (identical? NONE newe)
        structure
        (n/prepend-one structure newe)
        ))))

(defnav
  ^{:doc "Navigate to 'void' element after the sequence.
          For transformations – if result is not `NONE`,
          then append that value."}
  AFTER-ELEM
  []
  (select* [this structure next-fn]
    (next-fn NONE))
  (transform* [this structure next-fn]
    (let [newe (next-fn NONE)]
      (if (identical? NONE newe)
        structure
        (n/append-one structure newe)
        ))))

(defnav
  ^{:doc "Navigates to the specified subset (by taking an intersection).
          In a transform, that subset in the original set is changed to the
          new value of the subset."}
  subset
  [aset]
  (select* [this structure next-fn]
    (next-fn (set/intersection structure aset)))
  (transform* [this structure next-fn]
    (let [subset (set/intersection structure aset)
          newset (next-fn subset)]
      (-> structure
          (set/difference subset)
          (set/union newset)))))


(defnav
  ^{:doc "Navigates to the specified submap (using select-keys).
          In a transform, that submap in the original map is changed to the new
          value of the submap."}
  submap
  [m-keys]
  (select* [this structure next-fn]
    (next-fn (select-keys structure m-keys)))

  (transform* [this structure next-fn]
    (let [submap (select-keys structure m-keys)
          newmap (next-fn submap)]
      (merge (reduce dissoc structure m-keys)
             newmap))))

(defdynamicnav subselect
  "Navigates to a sequence that contains the results of (select ...),
  but is a view to the original structure that can be transformed.

  Requires that the input navigators will walk the structure's
  children in the same order when executed on \"select\" and then
  \"transform\".

  If transformed sequence is smaller than input sequence, missing entries
  will be filled in with NONE, triggering removal if supported by that navigator.

  Value collection (e.g. collect, collect-one) may not be used in the subpath."
  [& path]
  (late-bound-nav [late (late-path path)]
    (select* [this structure next-fn]
             (next-fn (compiled-select late structure)))
    (transform* [this structure next-fn]
      (let [select-result (compiled-select late structure)
            transformed (next-fn select-result)
            values-to-insert (i/mutable-cell (seq transformed))]
        (compiled-transform late
                            (fn [_] (let [vs (i/get-cell values-to-insert)]
                                      (if vs
                                        (do (i/update-cell! values-to-insert next)
                                            (first vs))
                                        NONE
                                        )))
                            structure)))))

(defrichnav
  ^{:doc "Navigates to the given key in the map (not to the value). Navigates only if the
          key currently exists in the map. Can transform to NONE to remove the key/value
          pair from the map."}
  map-key
  [key]
  (select* [this vals structure next-fn]
    (if (contains? structure key)
      (next-fn vals key)
      NONE
      ))
  (transform* [this vals structure next-fn]
    (if (contains? structure key)
      (let [newkey (next-fn vals key)
            dissoced (dissoc structure key)]
        (if (identical? NONE newkey)
          dissoced
          (assoc dissoced newkey (get structure key))
          ))
      structure
      )))

(defrichnav
  ^{:doc "Navigates to the given element in the set only if it exists in the set.
          Can transform to NONE to remove the element from the set."}
  set-elem
  [elem]
  (select* [this vals structure next-fn]
    (if (contains? structure elem)
      (next-fn vals elem)
      NONE
      ))
  (transform* [this vals structure next-fn]
    (if (contains? structure elem)
      (let [newelem (next-fn vals elem)
            removed (disj structure elem)]
        (if (identical? NONE newelem)
          removed
          (conj removed newelem)
          ))
      structure
      )))

(def ^{:doc "Navigate to the specified keys one after another. If navigate to NONE,
             that element is removed from the map or vector."}
  keypath
  (eachnav n/keypath*))

(def ^{:doc "Navigate to the specified keys one after another, only if they exist
             in the data structure. If navigate to NONE, that element is removed
             from the map or vector."}
  must
  (eachnav n/must*))

(def ^{:doc "Navigate to the specified indices one after another. If navigate to
            NONE, that element is removed from the sequence."}
  nthpath
  (eachnav n/nthpath*))

(defrichnav
  ^{:doc "Navigates to the empty space between the index and the prior index. For select
          navigates to NONE, and transforms to non-NONE insert at that position."}
  before-index
  [index]
  (select* [this vals structure next-fn]
    NONE)
  (transform* [this vals structure next-fn]
    (let [v (next-fn vals NONE)]
      (if
        (identical? NONE v)
          structure
          (n/insert-before-idx structure index v)))))

(defrichnav
  ^{:doc "Navigates to the index of the sequence if within 0 and size. Transforms move element
          at that index to the new index, shifting other elements in the sequence."}
  index-nav
  [i]
  (select* [this vals structure next-fn]
    (if (and (>= i 0) (< i (count structure)))
      (next-fn vals i)
      NONE
      ))
  (transform* [this vals structure next-fn]
    (if (and (>= i 0) (< i (count structure)))
      (let [newi (next-fn vals i)]
        (if (= newi i)
          structure
          (let [v (nth structure i)]
            (if (vector? structure)
              (let [shifted (if (< newi i)
                              (loop [j (dec i)
                                     s structure]
                                (if (< j newi)
                                  s
                                  (recur (dec j) (assoc s (inc j) (nth s j)))
                                  ))
                              (loop [j (inc i)
                                     s structure]
                                (if (> j newi)
                                  s
                                  (recur (inc j) (assoc s (dec j) (nth s j)))
                                  )))]
                (assoc shifted newi v)
                )
                (->> structure
                     (setval (nthpath i) NONE)
                     (setval (before-index newi) v)
                     )))))
      structure
      )))

(defnav
  ^{:doc "Navigate to [index elem] pairs for each element in a sequence. The sequence will be indexed
          starting from `start`. Changing index in transform has same effect as `index-nav`. Indices seen
          during transform take into account any shifting from prior sequence elements changing indices."}
  indexed-vals
  [start]
  (select* [this structure next-fn]
    ;; could be more efficient with a primitive mutable field
    (let [i (i/mutable-cell (dec start))]
      (doseqres NONE [e structure]
        (i/update-cell! i inc)
        (next-fn [(i/get-cell i) e])
        )))
  (transform* [this structure next-fn]
    (let [indices (i/mutable-cell (-> structure count range))]
      (reduce
       (fn [s e]
         (let [curri (first (i/get-cell indices))
               [newi* newe] (next-fn [(+ start curri) e])
               newi (- newi* start)]
           (i/update-cell!
             indices
             (fn [ii]
               (let [ii2 (next ii)]
                 (if (> newi curri)
                   (transform [ALL #(>= % (inc curri)) #(<= % newi)] dec ii2)
                   ii2
                   ))))
           (->> s
                (setval (nthpath curri) newe)
                (setval (index-nav curri) newi)
                )))
       structure
       structure
       ))))

(def
  ^{:doc "`indexed-vals` with a starting index of 0."}
  INDEXED-VALS
  (indexed-vals 0))

(defrichnav
  ^{:doc "Navigates to result of running `afn` on the currently navigated value."}
  view
  [afn]
  (select* [this vals structure next-fn]
    (next-fn vals (afn structure)))
  (transform* [this vals structure next-fn]
    (next-fn vals (afn structure))))


(defnav
  ^{:doc "Navigate to the result of running `parse-fn` on the value. For
          transforms, the transformed value then has `unparse-fn` run on
          it to get the final value at this point."}
  parser
  [parse-fn unparse-fn]
  (select* [this structure next-fn]
    (next-fn (parse-fn structure)))
  (transform* [this structure next-fn]
    (unparse-fn (next-fn (parse-fn structure)))))


(defnav
  ^{:doc "Navigates to atom value."}
  ATOM
  []
  (select* [this structure next-fn]
    (next-fn @structure))
  (transform* [this structure next-fn]
    (do
      (swap! structure next-fn)
      structure)))

(defnav regex-nav [re]
  (select* [this structure next-fn]
    (doseqres NONE [s (re-seq re structure)]
      (next-fn s)))
  (transform* [this structure next-fn]
    (clojure.string/replace structure re next-fn)))

(defdynamicnav selected?
  "Filters the current value based on whether a path finds anything.
  e.g. (selected? :vals ALL even?) keeps the current element only if an
  even number exists for the :vals key."
  [& path]
  (if-let [afn (n/extract-basic-filter-fn path)]
    afn
    (late-bound-richnav [late (late-path path)]
      (select* [this vals structure next-fn]
        (i/filter-select
          #(n/selected?* late vals %)
          vals
          structure
          next-fn))
      (transform* [this vals structure next-fn]
        (i/filter-transform
          #(n/selected?* late vals %)
          vals
          structure
          next-fn)))))

(defdynamicnav not-selected? [& path]
  (if-let [afn (n/extract-basic-filter-fn path)]
    (fn [s] (not (afn s)))
    (late-bound-richnav [late (late-path path)]
      (select* [this vals structure next-fn]
        (i/filter-select
          #(n/not-selected?* late vals %)
          vals
          structure
          next-fn))
      (transform* [this vals structure next-fn]
        (i/filter-transform
          #(n/not-selected?* late vals %)
          vals
          structure
          next-fn)))))

(defdynamicnav filterer
  "Navigates to a view of the current sequence that only contains elements that
  match the given path. An element matches the selector path if calling select
  on that element with the path yields anything other than an empty sequence.

  For transformation: `NONE` entries in the result sequence cause corresponding entries in
  input to be removed. A result sequence smaller than the input sequence is equivalent to
  padding the result sequence with `NONE` at the end until the same size as the input."
  [& path]
  (subselect ALL (selected? path)))

(defdynamicnav transformed
  "Navigates to a view of the current value by transforming it with the
   specified path and update-fn."
  [path update-fn]
  (late-bound-nav [late (late-path path)
                   late-fn update-fn]
    (select* [this structure next-fn]
      (next-fn (compiled-transform late late-fn structure)))
    (transform* [this structure next-fn]
      (next-fn (compiled-transform late late-fn structure)))))

(defdynamicnav traversed
  "Navigates to a view of the current value by transforming with a reduction over
   the specified traversal."
  [path reduce-fn]
  (late-bound-nav [late (late-path path)
                   late-fn reduce-fn]
    (select* [this structure next-fn]
      (next-fn (reduce late-fn (compiled-traverse late structure))))
    (transform* [this structure next-fn]
      (next-fn (reduce late-fn (compiled-traverse late structure)))
      )))

(def
  ^{:doc "Keeps the element only if it matches the supplied predicate. Functions in paths
          implicitly convert to this navigator."
    :direct-nav true}
  pred
  i/pred*)


(defn ^:direct-nav pred= [v] (pred #(= % v)))
(defn ^:direct-nav pred< [v] (pred #(< % v)))
(defn ^:direct-nav pred> [v] (pred #(> % v)))
(defn ^:direct-nav pred<= [v] (pred #(<= % v)))
(defn ^:direct-nav pred>= [v] (pred #(>= % v)))

(extend-type nil
  ImplicitNav
  (implicit-nav [this] STAY))

(extend-type #?(:clj clojure.lang.Keyword :cljs cljs.core/Keyword)
  ImplicitNav
  (implicit-nav [this] (n/keypath* this)))

(extend-type #?(:clj clojure.lang.Symbol :cljs cljs.core/Symbol)
  ImplicitNav
  (implicit-nav [this] (n/keypath* this)))

(extend-type #?(:clj String :cljs string)
  ImplicitNav
  (implicit-nav [this] (n/keypath* this)))

(extend-type #?(:clj Number :cljs number)
  ImplicitNav
  (implicit-nav [this] (n/keypath* this)))

(extend-type #?(:clj Character :cljs char)
  ImplicitNav
  (implicit-nav [this] (n/keypath* this)))

(extend-type #?(:clj Boolean :cljs boolean)
  ImplicitNav
  (implicit-nav [this] (n/keypath* this)))

(extend-type #?(:clj clojure.lang.AFn :cljs function)
  ImplicitNav
  (implicit-nav [this] (pred this)))

(extend-type #?(:clj clojure.lang.PersistentHashSet :cljs cljs.core/PersistentHashSet)
  ImplicitNav
  (implicit-nav [this] (pred this)))

(extend-type #?(:clj java.util.regex.Pattern :cljs js/RegExp)
  ImplicitNav
  (implicit-nav [this] (regex-nav this)))

(defnav
  ^{:doc "Navigates to the provided val if the structure is nil. Otherwise it stays
          navigated at the structure."}
  nil->val
  [v]
  (select* [this structure next-fn]
    (next-fn (if (nil? structure) v structure)))
  (transform* [this structure next-fn]
    (next-fn (if (nil? structure) v structure))))

(def
  ^{:doc "Navigates to #{} if the value is nil. Otherwise it stays
          navigated at the current value."}
  NIL->SET
  (nil->val #{}))

(def
  ^{:doc "Navigates to '() if the value is nil. Otherwise it stays
          navigated at the current value."}
  NIL->LIST
  (nil->val '()))

(def
  ^{:doc "Navigates to [] if the value is nil. Otherwise it stays
          navigated at the current value."}
  NIL->VECTOR
  (nil->val []))

(defnav ^{:doc "Navigates to the metadata of the structure, or nil if
  the structure has no metadata or may not contain metadata."}
  META
  []
  (select* [this structure next-fn]
    (next-fn (meta structure)))
  (transform* [this structure next-fn]
    (with-meta structure (next-fn (meta structure)))))

(defnav ^{:doc "Navigates to the name portion of the keyword or symbol"}
  NAME
  []
  (select* [this structure next-fn]
    (next-fn (name structure)))
  (transform* [this structure next-fn]
    (let [new-name (next-fn (name structure))
          ns (namespace structure)]
      (cond (keyword? structure) (keyword ns new-name)
            (symbol? structure) (symbol ns new-name)
            :else (throw (ex-info "NAME can only be used on symbols or keywords" {:structure structure}))
            ))))

(defnav ^{:doc "Navigates to the namespace portion of the keyword or symbol"}
  NAMESPACE
  []
  (select* [this structure next-fn]
    (next-fn (namespace structure)))
  (transform* [this structure next-fn]
    (let [name (name structure)
          new-ns (next-fn (namespace structure))]
      (cond (keyword? structure) (keyword new-ns name)
            (symbol? structure) (symbol new-ns name)
            :else (throw (ex-info "NAMESPACE can only be used on symbols or keywords"
                                  {:structure structure}))
            ))))

(defdynamicnav
  ^{:doc "Adds the result of running select with the given path on the
          current value to the collected vals."}
  collect
  [& path]
  (late-bound-collector [late (late-path path)]
    (collect-val [this structure]
      (compiled-select late structure))))


(defdynamicnav
  ^{:doc "Adds the result of running select-one with the given path on the
          current value to the collected vals."}
  collect-one
  [& path]
  (late-bound-collector [late (late-path path)]
    (collect-val [this structure]
      (compiled-select-one late structure))))


(defcollector
  ^{:doc
    "Adds an external value to the collected vals. Useful when additional arguments
     are required to the transform function that would otherwise require partial
     application or a wrapper function.

     e.g., incrementing val at path [:a :b] by 3:
     (transform [:a :b (putval 3)] + some-map)"}
  putval
  [val]
  (collect-val [this structure]
    val))

(defdynamicnav
  ^{:doc
    "Continues navigating on the given path with the collected vals reset to []. Once
     navigation leaves the scope of with-fresh-collected, the collected vals revert
     to what they were before."}
  with-fresh-collected
  [& path]
  (late-bound-richnav [late (late-path path)]
    (select* [this vals structure next-fn]
      (i/exec-select* late [] structure (fn [_ structure] (next-fn vals structure)))
      )
    (transform* [this vals structure next-fn]
      (i/exec-transform* late [] structure (fn [_ structure] (next-fn vals structure))))
      ))

(defrichnav
  ^{:doc "Drops all collected values for subsequent navigation."}
  DISPENSE
  []
  (select* [this vals structure next-fn]
    (next-fn [] structure))
  (transform* [this vals structure next-fn]
    (next-fn [] structure)))

(defdynamicnav if-path
  "Like cond-path, but with if semantics."
  ([cond-p then-path]
   (if-path cond-p then-path STOP))
  ([cond-p then-path else-path]
   (if-let [afn (n/extract-basic-filter-fn cond-p)]
    (late-bound-richnav [late-then (late-path then-path)
                         late-else (late-path else-path)]
      (select* [this vals structure next-fn]
        (n/if-select
          vals
          structure
          next-fn
          afn
          late-then
          late-else))
      (transform* [this vals structure next-fn]
        (n/if-transform
          vals
          structure
          next-fn
          afn
          late-then
          late-else)))
    (late-bound-richnav [late-cond (late-path cond-p)
                         late-then (late-path then-path)
                         late-else (late-path else-path)]
      (select* [this vals structure next-fn]
         (n/if-select
          vals
          structure
          next-fn
          #(n/selected?* late-cond vals %)
          late-then
          late-else))
      (transform* [this vals structure next-fn]
         (n/if-transform
          vals
          structure
          next-fn
          #(n/selected?* late-cond vals %)
          late-then
          late-else))))))


(defdynamicnav cond-path
  "Takes in alternating cond-path path cond-path path...
   Tests the structure if selecting with cond-path returns anything.
   If so, it uses the following path for this portion of the navigation.
   Otherwise, it tries the next cond-path. If nothing matches, then the structure
   is not selected."
  [& conds]
  (let [pairs (reverse (partition 2 conds))]
    (reduce
      (fn [p [tester apath]]
        (if-path tester apath p))
      STOP
      pairs)))


(defdynamicnav multi-path
  "A path that branches on multiple paths. For updates,
   applies updates to the paths in order."
  ([] STAY)
  ([path] path)
  ([path1 path2]
   (late-bound-richnav [late1 (late-path path1)
                        late2 (late-path path2)]
     (select* [this vals structure next-fn]
       (let [res1 (i/exec-select* late1 vals structure next-fn)]
         (if (reduced? res1)
           res1
           (let [res2 (i/exec-select* late2 vals structure next-fn)]
             (if (identical? NONE res1)
               res2
               res1
               )))))
     (transform* [this vals structure next-fn]
       (let [s1 (i/exec-transform* late1 vals structure next-fn)]
         (i/exec-transform* late2 vals s1 next-fn)))))
  ([path1 path2 & paths]
   (reduce multi-path (multi-path path1 path2) paths)))


(defdynamicnav stay-then-continue
  "Navigates to the current element and then navigates via the provided path.
   This can be used to implement pre-order traversal."
  [& path]
  (multi-path STAY path))

(defdynamicnav continue-then-stay
  "Navigates to the provided path and then to the current element. This can be used
   to implement post-order traversal."
  [& path]
  (multi-path path STAY))

(def
  ^{:doc "Navigate the data structure until reaching
          a value for which `afn` returns truthy. Has
          same semantics as clojure.walk."}
  walker
  (recursive-path [afn] p
    (cond-path (pred afn) STAY
               coll? [ALL p]
               )))

(def
  ^{:doc "Like `walker` but maintains metadata of any forms traversed."}
  codewalker
  (recursive-path [afn] p
    (cond-path (pred afn) STAY
               coll? [ALL-WITH-META p]
               )))

(let [empty->NONE (if-path empty? (terminal-val NONE))
      compact* (fn [nav] (multi-path nav empty->NONE))]
 (defdynamicnav compact
   "During transforms, after each step of navigation in subpath check if the
    value is empty. If so, remove that value by setting it to NONE."
   [& path]
   (map compact* path)
   ))
