(ns com.rpl.specter.macros
  (:use [com.rpl.specter impl])
  )

(defn gensyms [amt]
  (vec (repeatedly amt gensym)))

(defn determine-params-impls [[name1 & impl1] [name2 & impl2]]
  (if-not (= #{name1 name2} #{'select* 'transform*})
    (throw-illegal "defpath must implement select* and transform*, instead got "
      name1 " and " name2))
  (if (= name1 'select*)
    [impl1 impl2]
    [impl2 impl1]))


(def PARAMS-SYM (vary-meta (gensym "params") assoc :tag 'objects))
(def PARAMS-IDX-SYM (gensym "params-idx"))

(defn paramspath* [bindings num-params [impl1 impl2]]
  (let [[[[_ s-structure-sym s-next-fn-sym] & select-body]
         [[_ t-structure-sym t-next-fn-sym] & transform-body]]
         (determine-params-impls impl1 impl2)]
    (if (= 0 num-params)
      `(no-params-compiled-path
         (->TransformFunctions
           StructurePathExecutor
           (fn [~s-structure-sym ~s-next-fn-sym]
             ~@select-body)
           (fn [~t-structure-sym ~t-next-fn-sym]
             ~@transform-body)
           ))
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
         ))))

(defn paramscollector* [post-bindings num-params [_ [_ structure-sym] & body]]
  `(let [collector# (fn [~PARAMS-SYM ~PARAMS-IDX-SYM vals# ~structure-sym next-fn#]
                      (let [~@post-bindings ~@[] ; to avoid syntax highlighting issues
                            c# (do ~@body)]
                        (next-fn#                                    
                          ~PARAMS-SYM
                          (+ ~PARAMS-IDX-SYM ~num-params)
                          (conj vals# c#)
                          ~structure-sym)                     
                        ))]
     (->ParamsNeededPath
       (->TransformFunctions
         RichPathExecutor
         collector#
         collector# )
       ~num-params
       )))

(defn pathed-path* [builder paths-seq latefns-sym pre-bindings post-bindings impls]
  (let [num-params-sym (gensym "num-params")]
    `(let [paths# (map comp-paths* ~paths-seq)
           needed-params# (map num-needed-params paths#)
           offsets# (cons 0 (reductions + needed-params#))
           any-params-needed?# (->> paths#
                                    (filter params-needed-path?)
                                    empty?
                                    not)
           ~num-params-sym (last offsets#)
           ~latefns-sym (map
                          (fn [o# p#]
                            (if (compiled-path? p#)
                              (fn [params# params-idx#]
                                p# )
                              (fn [params# params-idx#]
                                (bind-params* p# params# (+ params-idx# o#))
                                )))
                          offsets#
                          paths#)
           ~@pre-bindings
           ret# ~(builder post-bindings num-params-sym impls)
           ]
    (if (not any-params-needed?#)
      (bind-params* ret# nil 0)
      ret#
      ))))

(defn make-param-retrievers [params]
  (->> params
       (map-indexed
         (fn [i p]
           [p `(aget ~PARAMS-SYM
                     (+ ~PARAMS-IDX-SYM ~i))]
           ))
       (apply concat)))


(defmacro path
  "Defines a StructurePath with late bound parameters. This path can be precompiled
  with other selectors without knowing the parameters. When precompiled with other
  selectors, the resulting selector takes in parameters for all selectors in the path
  that needed parameters (in the order in which they were declared)."
  [params impl1 impl2]
  (let [num-params (count params)
        retrieve-params (make-param-retrievers params)]
    (paramspath* retrieve-params num-params [impl1 impl2])
    ))

(defmacro paramsfn [params [structure-sym] & impl]
  `(path ~params
     (~'select* [this# structure# next-fn#]
       (let [afn# (fn [~structure-sym] ~@impl)]
         (filter-select afn# structure# next-fn#)
         ))
     (~'transform* [this# structure# next-fn#]
       (let [afn# (fn [~structure-sym] ~@impl)]
         (filter-transform afn# structure# next-fn#)
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

(defmacro defpath [name & body]
  `(def ~name (path ~@body)))

(defmacro defcollector [name & body]
  `(def ~name (paramscollector ~@body)))

(defmacro fixed-pathed-path
  "This helper is used to define selectors that take in a fixed number of other selector
   paths as input. Those selector paths may require late-bound params, so this helper
   will create a parameterized selector if that is the case. If no late-bound params
   are required, then the result is executable."
  [bindings impl1 impl2]
  (let [bindings (partition 2 bindings)
        paths (mapv second bindings)
        names (mapv first bindings)
        latefns-sym (gensym "latefns")
        latefn-syms (vec (gensyms (count paths)))]
    (pathed-path*
      paramspath*
      paths
      latefns-sym
      [latefn-syms latefns-sym]
      (mapcat (fn [n l] [n `(~l ~PARAMS-SYM ~PARAMS-IDX-SYM)]) names latefn-syms)
      [impl1 impl2])))

(defmacro variable-pathed-path
  "This helper is used to define selectors that take in a variable number of other selector
   paths as input. Those selector paths may require late-bound params, so this helper
   will create a parameterized selector if that is the case. If no late-bound params
   are required, then the result is executable."
  [[latepaths-seq-sym paths-seq] impl1 impl2]
  (let [latefns-sym (gensym "latefns")]
    (pathed-path*
      paramspath*
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
    (pathed-path*
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
              (no-params-compiled-path
                (->TransformFunctions
                  RichPathExecutor
                  (fn ~rargs
                    (let [path# ~retrieve
                          selector# (compiled-selector path#)]
                      (selector# ~@rargs)
                      ))
                  (fn ~rargs
                    (let [path# ~retrieve
                          transformer# (compiled-transformer path#)]
                      (transformer# ~@rargs)
                      ))))
              (->ParamsNeededPath
                (->TransformFunctions
                  RichPathExecutor
                  (fn ~rargs
                    (let [path# ~retrieve
                          selector# (params-needed-selector path#)]
                      (selector# ~@rargs)
                      ))
                  (fn ~rargs
                    (let [path# ~retrieve
                          transformer# (params-needed-transformer path#)]
                      (transformer# ~@rargs)
                      )))
                ~num-params
                )
              ))))))


(defn declared-name [name]
  (symbol (str name "-declared")))

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
             (no-params-compiled-path
               (->TransformFunctions
                RichPathExecutor
                (fn ~rargs
                  (let [selector# (compiled-selector ~declared)]
                    (selector# ~@rargs)
                    ))
                (fn ~rargs
                  (let [transformer# (compiled-transformer ~declared)]
                    (transformer# ~@rargs)
                    ))))
             (->ParamsNeededPath
               (->TransformFunctions
                 RichPathExecutor
                 (fn ~rargs
                   (let [selector# (params-needed-selector ~declared)]
                     (selector# ~@rargs)
                     ))
                 (fn ~rargs
                   (let [transformer# (params-needed-transformer ~declared)]
                     (transformer# ~@rargs)
                     )))
               ~num-params
               )
           ))))))

(defmacro providepath [name apath]
  `(let [comped# (comp-paths* ~apath)
         expected-params# (num-needed-params ~name)
         needed-params# (num-needed-params comped#)]
     (if-not (= needed-params# expected-params#)
       (throw-illegal "Invalid number of params in provided path, expected "
           expected-params# " but got " needed-params#))
     (def ~(declared-name name)
       (update-in comped#
                  [:transform-fns]
                  coerce-tfns-rich)
       )))

(defmacro extend-protocolpath [protpath & extensions]
  `(extend-protocolpath* ~protpath ~(protpath-sym protpath) ~(vec extensions)))
