(ns com.rpl.specter.zipper
  #+cljs (:require-macros
            [com.rpl.specter.macros
              :refer [defpath path]])
  #+clj
  (:use
    [com.rpl.specter.macros :only [defpath path]])
  (:require [com.rpl.specter :as s]
            [clojure.zip :as zip]))

(defpath zipper [constructor]
  (select* [this structure next-fn]
    (next-fn (constructor structure)))
  (transform* [this structure next-fn]
    (zip/root (next-fn (constructor structure)))
    ))

(def VECTOR-ZIP (zipper zip/vector-zip))
(def SEQ-ZIP (zipper zip/seq-zip))
(def XML-ZIP (zipper zip/xml-zip))


(def NEXT
  (s/comp-paths
    (s/view zip/next)
    (s/if-path zip/end?
      s/STOP
      s/STAY)))

(defn- mk-zip-nav [nav]
  (path []
    (select* [this structure next-fn]
      (let [ret (nav structure)]
        (if ret (next-fn ret))
        ))
    (transform* [this structure next-fn]
      (let [ret (nav structure)]
        (if ret (next-fn ret) structure)
        ))))

;; (multi-path RIGHT LEFT) will not navigate to the right and left
;; of the currently navigated element because locations aren't stable
;; like they are for maps/graphs. The path following RIGHT could 
;; insert lots of elements all over the sequence, and there's no
;; way to determine how to get "back". 
(def RIGHT (mk-zip-nav zip/right))
(def LEFT (mk-zip-nav zip/left))
(def DOWN (mk-zip-nav zip/down))
(def UP (mk-zip-nav zip/up))

(def RIGHTMOST (s/view zip/rightmost))
(def LEFTMOST (s/view zip/leftmost))

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
