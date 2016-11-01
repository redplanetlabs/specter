(ns com.rpl.specter.macros
  (:use [com.rpl.specter.protocols :only [RichNavigator]])
  (:require [com.rpl.specter.impl :as i]))


(defn- determine-params-impls [impls]
 (let [grouped (->> impls (map (fn [[n & body]] [n body])) (into {}))]
   (if-not (= #{'select* 'transform*} (-> grouped keys set))
     (i/throw-illegal "defnav must implement select* and transform*, instead got "
                      (keys grouped)))
   grouped))


(defmacro richnav [params & impls]
 (if (empty? params)
   `(reify RichNavigator ~@impls)
   `(i/direct-nav-obj
      (fn ~params
        (reify RichNavigator
          ~@impls)))))


(defmacro nav [params & impls]
 (let [{[[_ s-structure-sym s-next-fn-sym] & s-body] 'select*
        [[_ t-structure-sym t-next-fn-sym] & t-body] 'transform*} (determine-params-impls impls)]
   `(richnav ~params
      (~'select* [this# vals# ~s-structure-sym next-fn#]
        (let [~s-next-fn-sym (fn [s#] (next-fn# vals# s#))]
          ~@s-body))
      (~'transform* [this# vals# ~t-structure-sym next-fn#]
        (let [~t-next-fn-sym (fn [s#] (next-fn# vals# s#))]
          ~@t-body)))))

(defn- helper-name [name method-name]
 (with-meta (symbol (str name "-" method-name)) {:no-doc true}))

(defmacro defnav [name params & impls]
 ;; remove the "this" param for the helper
 (let [helpers (for [[mname [_ & mparams] & mbody] impls]
                 `(defn ~(helper-name name mname) [~@params ~@mparams] ~@mbody))
       decls (for [[mname & _] impls]
               `(declare ~(helper-name name mname)))]
   `(do
      ~@decls
      ~@helpers
      (def ~name (nav ~params ~@impls)))))

(defmacro defrichnav [name params & impls]
  `(def ~name (richnav ~params ~@impls)))
