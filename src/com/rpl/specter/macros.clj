(ns com.rpl.specter.macros
  (:use [com.rpl.specter impl])
  )

(defn gensyms [amt]
  (vec (repeatedly amt gensym)))

(defn determine-params-impls [[name1 & impl1] [name2 & impl2]]
  (if (= name1 'select*)
    [impl1 impl2]
    [impl2 impl1]))


(def PARAMS-SYM (vary-meta (gensym "params") assoc :tag 'objects))
(def PARAMS-IDX-SYM (gensym "params-idx"))

(defn paramspath* [bindings num-params [impl1 impl2]]
  (let [[[[_ s-structure-sym s-next-fn-sym] & select-body]
         [[_ t-structure-sym t-next-fn-sym] & transform-body]]
         (determine-params-impls impl1 impl2)]
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
       )))

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
    (if (= 0 ~num-params-sym)
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

