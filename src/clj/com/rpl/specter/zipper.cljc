(ns com.rpl.specter.zipper
  #?(:cljs (:require-macros
            [com.rpl.specter
              :refer [defnav nav declarepath providepath recursive-path]]))
  #?(:clj
     (:use
       [com.rpl.specter :only [defnav nav declarepath providepath
                                      recursive-path]]))
  (:require [com.rpl.specter :as s]
            [clojure.zip :as zip]))

(defnav zipper [constructor]
  (select* [this structure next-fn]
    (next-fn (constructor structure)))
  (transform* [this structure next-fn]
    (zip/root (next-fn (constructor structure)))))


(def VECTOR-ZIP (zipper zip/vector-zip))
(def SEQ-ZIP (zipper zip/seq-zip))
(def XML-ZIP (zipper zip/xml-zip))


(def ^{:doc "Navigate to the next element in the structure.
             If no next element, works like STOP."}
  NEXT
  (s/comp-paths
    (s/view zip/next)
    (s/if-path zip/end?
      s/STOP
      s/STAY)))


(defn- mk-zip-nav [znav]
  (nav []
    (select* [this structure next-fn]
      (let [ret (znav structure)]
        (if ret (next-fn ret))))

    (transform* [this structure next-fn]
      (let [ret (znav structure)]
        (if ret (next-fn ret) structure)))))


;; (multi-path RIGHT LEFT) will not navigate to the right and left
;; of the currently navigated element because locations aren't stable
;; like they are for maps/graphs. The path following RIGHT could
;; insert lots of elements all over the sequence, and there's no
;; way to determine how to get "back".
(def ^{:doc "Navigate to the element to the right.
             If no element there, works like STOP."}
  RIGHT (mk-zip-nav zip/right))

(def ^{:doc "Navigate to the element to the left.
             If no element there, works like STOP."}
  LEFT (mk-zip-nav zip/left))

(def DOWN (mk-zip-nav zip/down))
(def UP (mk-zip-nav zip/up))

(def ^{:doc "Navigate to the previous element.
             If this is the first element, works like STOP."}
  PREV (mk-zip-nav zip/prev))

(def RIGHTMOST (s/view zip/rightmost))
(def LEFTMOST (s/view zip/leftmost))

(defn- inner-insert [structure next-fn inserter mover backer]
  (let [to-insert (next-fn [])
        inserts (reduce
                  (fn [z e] (-> z (inserter e) mover))
                  structure
                  to-insert)]

    (if backer
      (reduce (fn [z _] (backer z)) inserts to-insert)
      inserts)))


(defnav ^{:doc "Navigate to the empty subsequence directly to the
                 right of this element."}
  INNER-RIGHT []
  (select* [this structure next-fn]
    (next-fn []))
  (transform* [this structure next-fn]
    (inner-insert structure next-fn zip/insert-right zip/right zip/left)))


(defnav ^{:doc "Navigate to the empty subsequence directly to the
                 left of this element."}
  INNER-LEFT []
  (select* [this structure next-fn]
    (next-fn []))
  (transform* [this structure next-fn]
    (inner-insert structure next-fn zip/insert-left identity nil)))


(defnav NODE []
  (select* [this structure next-fn]
    (next-fn (zip/node structure)))

  (transform* [this structure next-fn]
    (zip/edit structure next-fn)))


(defnav ^{:doc "Navigate to the subsequence containing only
                 the node currently pointed to. This works just
                 like srange and can be used to remove elements
                 from the structure"}
  NODE-SEQ []
  (select* [this structure next-fn]
    (next-fn [(zip/node structure)]))

  (transform* [this structure next-fn]
    (let [to-insert (next-fn [(zip/node structure)])
          inserted (reduce zip/insert-left structure to-insert)]
      (zip/remove inserted))))


(def ^{:doc "Navigate the zipper to the first element
                     in the structure matching predfn. A linear scan
                     is done using NEXT to find the element."}
  find-first
  (recursive-path [predfn] p
    (s/if-path [NODE (s/pred predfn)]
      s/STAY
      [NEXT p])))



(declarepath ^{:doc "Navigate to every element reachable using calls
                     to NEXT"}
  NEXT-WALK)

(providepath NEXT-WALK
  (s/stay-then-continue
    NEXT
    NEXT-WALK))
