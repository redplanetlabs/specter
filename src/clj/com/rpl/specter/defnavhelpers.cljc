(ns com.rpl.specter.defnavhelpers
  (:require [com.rpl.specter.impl :as i]))

(defn param-delta [^long i]
  (fn [^objects params ^long params-idx]
    (aget params (+ params-idx i))))


(defn bound-params [path ^long start-delta]
  (fn [^objects params ^long params-idx]
    (if (i/params-needed-path? path)
      (i/bind-params* path params (+ params-idx start-delta))
      path)))
