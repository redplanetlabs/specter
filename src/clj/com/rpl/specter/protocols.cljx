(ns com.rpl.specter.protocols)

(defprotocol Navigator
  (select* [this structure next-fn])
  (transform* [this structure next-fn]))

(defprotocol Collector
  (collect-val [this structure]))
