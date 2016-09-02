(ns com.rpl.specter.util-macros)

(defmacro doseqres [backup-res [n aseq] & body]
  `(reduce
     (fn [curr# ~n]
       (let [ret# (do ~@body)]
         (if (identical? ret# ~backup-res)
           curr#
           ret#)))

     ~backup-res
     ~aseq))

(defn- gensyms [amt]
  (vec (repeatedly amt gensym)))

(defmacro mk-comp-navs []
  (let [impls (for [i (range 3 20)]
                (let [[fsym & rsyms :as syms] (gensyms i)]
                  `([~@syms] (~'comp-navs ~fsym (~'comp-navs ~@rsyms)))))
        last-syms (gensyms 19)]
    `(defn ~'comp-navs
       ([] ~'com.rpl.specter.impl/STAY*)
       ([nav1#] nav1#)
       ([nav1# nav2#] (~'com.rpl.specter.impl/combine-two-navs nav1# nav2#))
       ~@impls
       ([~@last-syms ~'& rest#]
        (~'comp-navs
          (~'comp-navs ~@last-syms)
          (reduce ~'comp-navs rest#))))))
