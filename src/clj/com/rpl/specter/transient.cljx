(ns com.rpl.specter.transient
  #+cljs
  (:require-macros [com.rpl.specter.macros
                    :refer
                    [defnav
                     defpathedfn]])
  (:use #+clj
        [com.rpl.specter.macros :only
         [defnav
          defpathedfn]])
  (:require [com.rpl.specter.impl :as i]
            [com.rpl.specter :refer [subselect]]))

(defnav
  ^{:doc "Navigates to the specified key of a transient collection,
          navigating to nil if it doesn't exist."}
  keypath!
  [key]
  (select* [this structure next-fn]
    (next-fn (get structure key)))
  (transform* [this structure next-fn]
    (assoc! structure key (next-fn (get structure key)))))

(def END!
  "Navigates to an empty (persistent) vector at the end of a transient vector."
  (i/comp-paths* [(i/->TransientEndNavigator)]))

(def FIRST!
  "Navigates to the first element of a transient vector."
  (keypath! 0))

(def LAST!
  "Navigates to the last element of a transient vector."
  (i/comp-paths* [(i/->TransientLastNavigator)]))

(def ALL!
  ;; TODO: only works on transient vectors, not sets / maps; need a
  ;; different name?
  "Navigates to each element of a transient vector."
  (i/comp-paths* [(i/->TransientAllNavigator)]))

(defpathedfn filterer!
  "Navigates to a view of the current transient vector that only
  contains elements that match the given path."
  [& path]
  (subselect ALL! (selected? path)))

(defn- select-keys-from-transient-map
  "Selects keys from transient map, because built-in select-keys uses
  `find` which is unsupported."
  [m m-keys]
  (loop [result {}
         m-keys m-keys]
    (if-not (seq m-keys)
      result
      (let [k (first m-keys)
            ;; support Clojure 1.6 where contains? is broken on transients
            item (get m k ::not-found)]
        (recur (if-not (identical? item ::not-found)
                 (assoc result k item)
                 result)
               (rest m-keys))))))

(defnav
  ^{:doc "Navigates to the specified persistent submap of a transient map."}
  submap!
  [m-keys]
  (select* [this structure next-fn]
    (select-keys-from-transient-map structure m-keys))
  (transform* [this structure next-fn]
    (let [selected (select-keys-from-transient-map structure m-keys)
          res (next-fn selected)]
      (as-> structure %
        (reduce (fn [m k]
                  (dissoc! m k))
                % m-keys)
        (reduce (fn [m [k v]]
                  (assoc! m k v))
                % res)))))
