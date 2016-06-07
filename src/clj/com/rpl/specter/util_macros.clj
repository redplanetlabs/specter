(ns com.rpl.specter.util-macros)

(defmacro doseqres [backup-res [n aseq] & body]
  `(reduce
     (fn [curr# ~n]
       (let [ret# (do ~@body)]
         (if (identical? ret# ~backup-res)
           curr#
           ret#
           )))
     ~backup-res
     ~aseq
     ))
