(ns com.rpl.specter.util-macros)

(defmacro doseqres [backup-res [n aseq] & body]
  `(reduce
     (fn [curr# ~n]
       (let [ret# (do ~@body)]
         (if (identical? ret# ~backup-res)
           curr#
           (if (reduced? ret#) (reduced ret#) ret#))))

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



 ;;TODO: move these definitions somewhere else
(defn late-fn-record-name [i]
  (symbol (str "LateFn" i)))

(defn late-fn-record-constructor-name [i]
  (symbol (str "->LateFn" i)))

(defn- mk-late-fn-record [i]
  (let [fields (concat ['fn] (for [j (range i)] (symbol (str "arg" j))))
        dparams (gensym "dynamic-params")
        resolvers (for [f fields]
                    `(~'late-resolve ~f ~dparams))]
   `(defrecord ~(late-fn-record-name i) [~@fields]
      ~'LateResolve
      (~'late-resolve [this# ~dparams]
        (~@resolvers)))))


(defmacro mk-late-fn-records []
  (let [impls (for [i (range 20)] (mk-late-fn-record i))]
    `(do ~@impls)))

(defmacro mk-late-fn []
  (let [f (gensym "afn")
        args (gensym "args")
        cases (for [i (range 19)]
                [i
                 (let [gets (for [j (range i)] `(nth ~args ~j))]
                  `(~(late-fn-record-constructor-name i)
                    ~f
                    ~@gets))])]
    `(defn ~'late-fn [~f ~args]
       (case (count ~args)
         ~@(apply concat cases)
         (throw (ex-info "Cannot have late function with more than 18 args" {}))))))
