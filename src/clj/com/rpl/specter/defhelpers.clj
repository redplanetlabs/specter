(ns com.rpl.specter.defhelpers)

(defn gensyms [amt]
  (vec (repeatedly amt gensym)))

(defmacro define-ParamsNeededPath [clj? fn-type invoke-name var-arity-impl]
  (let [a (with-meta (gensym "array") {:tag 'objects})
        impls (for [i (range 21)
                    :let [args (vec (gensyms i))
                          setters (for [j (range i)] `(aset ~a ~j ~(get args j)))]]
                `(~invoke-name [this# ~@args]
                  (let [~a (~(if clj? 'com.rpl.specter.impl/fast-object-array 'object-array) ~i)]
                    ~@setters
                    (com.rpl.specter.impl/bind-params* this# ~a 0)
                    )))]
    `(defrecord ~'ParamsNeededPath [~'rich-nav ~'num-needed-params]
       ~fn-type
       ~@impls
       ~var-arity-impl
       )))
