(ns com.rpl.specter.defnavhelpers
  (:require [com.rpl.specter.impl :as i]))

(defn param-delta [i]
  (fn [^objects params params-idx]
    (aget params (+ params-idx i))
    ))

(defn bound-params [path start-delta]
  (fn [^objects params params-idx]
    (if (i/params-needed-path? path)
      (i/bind-params* path params (+ params-idx start-delta))
      path
      )))
