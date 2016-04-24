(ns com.rpl.specter.zipper
  #+cljs (:require-macros
            [com.rpl.specter.macros
              :refer [fixed-pathed-path defpath]])
  (:use
    #+clj [com.rpl.specter.macros :only [fixed-pathed-path defpath]]
    [com.rpl specter])
  (:require [clojure [zip :as zip]]))

(defpath zipper [constructor]
  (select* [this structure next-fn]
    (next-fn (constructor structure)))
  (transform* [this structure next-fn]
    (zip/root (next-fn (constructor structure)))
    ))

(def VECTOR-ZIP (zipper zip/vector-zip))
(def SEQ-ZIP (zipper zip/seq-zip))
(def XML-ZIP (zipper zip/xml-zip))

(def NEXT (view zip/next))
(def RIGHT (view zip/right))
(def RIGHTMOST (view zip/rightmost))
(def LEFT (view zip/left))
(def DOWN (view zip/down))
(def LEFTMOST (view zip/leftmost))
(def UP (view zip/up))

(defn- inner-insert [structure next-fn inserter mover backer]
  (let [to-insert (next-fn [])
        inserts (reduce
                  (fn [z e] (-> z (inserter e) mover))
                  structure
                  to-insert
                  )]
    (if backer
      (reduce (fn [z _] (backer z)) inserts to-insert)
      inserts)
    ))

(defpath INNER-RIGHT []
  (select* [this structure next-fn]
    (next-fn []))
  (transform* [this structure next-fn]
    (inner-insert structure next-fn zip/insert-right zip/right zip/left)
    ))

(defpath INNER-LEFT []
  (select* [this structure next-fn]
    (next-fn []))
  (transform* [this structure next-fn]
    (inner-insert structure next-fn zip/insert-left identity nil)
    ))

(defpath NODE []
  (select* [this structure next-fn]
    (next-fn (zip/node structure))
    )
  (transform* [this structure next-fn]
    (zip/edit structure next-fn)
    ))
