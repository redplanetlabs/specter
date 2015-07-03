(ns com.rpl.specter.prot-opt-invoke)

(defmacro mk-optimized-invocation [protocol obj method num-args]
  (let [args (take num-args (repeatedly gensym))]
    `(if (~'implements? ~protocol ~obj)
       (fn [^not-native o# ~@args]
         (~method o# ~@args)
         )
       ~method
       )))