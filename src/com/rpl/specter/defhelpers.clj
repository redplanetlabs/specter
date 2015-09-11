(ns com.rpl.specter.defhelpers)

(defn gensyms [amt]
  (vec (repeatedly amt gensym)))

(defmacro define-ParamsNeededPath [fn-type]
  (let [a (with-meta (gensym "array") {:tag 'objects})
        impls (for [i (range 21)
                    :let [args (vec (gensyms i))
                          setters (for [j (range i)] `(aset ~a ~j ~(get args j)))]]
                `(~'invoke [this# ~@args]
                  (let [~a (object-array ~i)]
                    ~@setters
                    (com.rpl.specter.impl/bind-params* this# ~a 0)
                    )))]
    `(defrecord ~'ParamsNeededPath [~'transform-fns ~'num-needed-params]
       ~fn-type
       ~@impls
       (~'applyTo [this# args#]
         (let [a# (object-array args#)]
           (com.rpl.specter.impl/bind-params* this# a# 0))))))
