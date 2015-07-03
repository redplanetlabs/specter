(ns com.rpl.specter.prot-opt-invoke)

(defmacro mk-optimized-invocation [protocol obj method num-args]
  (let [args (take num-args (repeatedly gensym))
  	    o (-> (gensym) (with-meta {:tag 'not-native}))]
    `(if (~'implements? ~protocol ~obj)
       (fn [~o ~@args]
         (~method ~o ~@args)
         )
       ~method
       )))