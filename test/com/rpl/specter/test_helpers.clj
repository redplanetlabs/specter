(ns com.rpl.specter.test-helpers
  (:use [clojure.test]
        [clojure.test.check.clojure-test])
  (:require [clojure.test.check             
             [generators :as gen]
             [properties :as prop]]
            [clojure.test.check :as qc]
            [clojure.pprint :as pp]))


;; it seems like gen/bind and gen/return are a monad (hence the names)
(defmacro for-all+ [bindings & body]
  (let [parts (partition 2 bindings)
        vars (vec (map first parts))
        genned (reduce
                (fn [curr [v code]]
                  `(gen/bind ~code (fn [~v] ~curr)))
                `(gen/return ~vars)
                (reverse parts))]
    `(prop/for-all [~vars ~genned]
                   ~@body )))

(defn limit-size [n {gen :gen}]
  (gen/->Generator
   (fn [rnd _size]
     (gen rnd (if (< _size n) _size n)))))

