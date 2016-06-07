(ns com.rpl.specter.util-macros)

(defmacro doseqres [backup-res-kw [n aseq] & body]
  (let [platform (if (contains? &env :locals) :cljs :clj)
        idfn (if (= platform :clj) 'identical? 'keyword-identical?)]
    `(reduce
       (fn [curr# ~n]
         (let [ret# (do ~@body)]
           (if (~idfn ret# ~backup-res-kw)
             curr#
             ret#
             )))
       ~backup-res-kw
       ~aseq
       )))
