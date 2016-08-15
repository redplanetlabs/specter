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


(defmacro definterface+ [name & methods]
  (let [platform (if (contains? &env :locals) :cljs :clj)]
    (if (= platform :cljs)
      `(defprotocol ~name ~@methods)
      (let [methods (for [[n p & body] methods]
                      (concat [n (-> p next vec)] body))]
        `(definterface ~name ~@methods)))))
