(ns com.rpl.specter.navs
  #?(:cljs (:require-macros
            [com.rpl.specter
              :refer
              [defnav defrichnav]]
            [com.rpl.specter.util-macros :refer
              [doseqres]]))
  (:use #?(:clj [com.rpl.specter.macros :only [defnav defrichnav]])
        #?(:clj [com.rpl.specter.util-macros :only [doseqres]]))
  (:require [com.rpl.specter.impl :as i]
            [clojure.walk :as walk]
            #?(:clj [clojure.core.reducers :as r])))


(defn not-selected?*
  [compiled-path structure]
  (->> structure
       (i/compiled-select-any* compiled-path)
       (identical? i/NONE)))

(defn selected?*
  [compiled-path structure]
  (not (not-selected?* compiled-path structure)))

(defn walk-select [pred continue-fn structure]
  (let [ret (i/mutable-cell i/NONE)
        walker (fn this [structure]
                 (if (pred structure)
                   (let [r (continue-fn structure)]
                     (if-not (identical? r i/NONE)
                       (i/set-cell! ret r))
                     r)

                   (walk/walk this identity structure)))]

    (walker structure)
    (i/get-cell ret)))


(defn key-select [akey structure next-fn]
  (next-fn (get structure akey)))

(defn key-transform [akey structure next-fn]
  (assoc structure akey (next-fn (get structure akey))))


(defn all-select [structure next-fn]
  (doseqres i/NONE [e structure]
    (next-fn e)))

#?(
   :clj
   (defn queue? [coll]
     (instance? clojure.lang.PersistentQueue coll))

   :cljs
   (defn queue? [coll]
     (= (type coll) (type #queue []))))


(defprotocol AllTransformProtocol
  (all-transform [structure next-fn]))

(defn- non-transient-map-all-transform [structure next-fn empty-map]
  (reduce-kv
    (fn [m k v]
      (let [[newk newv] (next-fn [k v])]
        (assoc m newk newv)))

    empty-map
    structure))


(extend-protocol AllTransformProtocol
  nil
  (all-transform [structure next-fn]
    nil)


  ;; in cljs they're PersistentVector so don't need a special case
  #?(:clj clojure.lang.MapEntry)
  #?(:clj
     (all-transform [structure next-fn]
       (let [newk (next-fn (key structure))
             newv (next-fn (val structure))]
         (clojure.lang.MapEntry. newk newv))))


  #?(:clj clojure.lang.PersistentVector :cljs cljs.core/PersistentVector)
  (all-transform [structure next-fn]
    (mapv next-fn structure))

  #?(:clj clojure.lang.PersistentArrayMap)
  #?(:clj
     (all-transform [structure next-fn]
       (let [k-it (.keyIterator structure)
             v-it (.valIterator structure)
             array (i/fast-object-array (* 2 (.count structure)))]
         (loop [i 0]
           (if (.hasNext k-it)
             (let [k (.next k-it)
                   v (.next v-it)
                   [newk newv] (next-fn [k v])]
               (aset array i newk)
               (aset array (inc i) newv)
               (recur (+ i 2)))))
         (clojure.lang.PersistentArrayMap/createAsIfByAssoc array))))


  #?(:cljs cljs.core/PersistentArrayMap)
  #?(:cljs
     (all-transform [structure next-fn]
       (non-transient-map-all-transform structure next-fn {})))


  #?(:clj clojure.lang.PersistentTreeMap :cljs cljs.core/PersistentTreeMap)
  (all-transform [structure next-fn]
    (non-transient-map-all-transform structure next-fn (empty structure)))


  #?(:clj clojure.lang.PersistentHashMap :cljs cljs.core/PersistentHashMap)
  (all-transform [structure next-fn]
    (persistent!
      (reduce-kv
        (fn [m k v]
          (let [[newk newv] (next-fn [k v])]
            (assoc! m newk newv)))

        (transient
          #?(:clj clojure.lang.PersistentHashMap/EMPTY :cljs cljs.core.PersistentHashMap.EMPTY))

        structure)))



  #?(:clj Object)
  #?(:clj
     (all-transform [structure next-fn]
       (let [empty-structure (empty structure)]
         (cond (and (list? empty-structure) (not (queue? empty-structure)))
            ;; this is done to maintain order, otherwise lists get reversed
               (doall (map next-fn structure))

               (map? structure)
            ;; reduce-kv is much faster than doing r/map through call to (into ...)
               (reduce-kv
                 (fn [m k v]
                   (let [[newk newv] (next-fn [k v])]
                     (assoc m newk newv)))

                 empty-structure
                 structure)


               :else
               (->> structure (r/map next-fn) (into empty-structure))))))


  #?(:cljs default)
  #?(:cljs
     (all-transform [structure next-fn]
       (let [empty-structure (empty structure)]
         (if (and (list? empty-structure) (not (queue? empty-structure)))
        ;; this is done to maintain order, otherwise lists get reversed
           (doall (map next-fn structure))
           (into empty-structure (map #(next-fn %)) structure))))))



(defprotocol MapValsTransformProtocol
  (map-vals-transform [structure next-fn]))

(defn map-vals-non-transient-transform [structure empty-map next-fn]
  (reduce-kv
    (fn [m k v]
      (assoc m k (next-fn v)))
    empty-map
    structure))

(extend-protocol MapValsTransformProtocol
  nil
  (map-vals-transform [structure next-fn]
    nil)


  #?(:clj clojure.lang.PersistentArrayMap)
  #?(:clj
     (map-vals-transform [structure next-fn]
       (let [k-it (.keyIterator structure)
             v-it (.valIterator structure)
             array (i/fast-object-array (* 2 (.count structure)))]
         (loop [i 0]
           (if (.hasNext k-it)
             (let [k (.next k-it)
                   v (.next v-it)
                   newv (next-fn v)]
               (aset array i k)
               (aset array (inc i) newv)
               (recur (+ i 2)))))
         (clojure.lang.PersistentArrayMap. array))))


  #?(:cljs cljs.core/PersistentArrayMap)
  #?(:cljs
     (map-vals-transform [structure next-fn]
       (map-vals-non-transient-transform structure {} next-fn)))


  #?(:clj clojure.lang.PersistentTreeMap :cljs cljs.core/PersistentTreeMap)
  (map-vals-transform [structure next-fn]
    (map-vals-non-transient-transform structure (empty structure) next-fn))


  #?(:clj clojure.lang.PersistentHashMap :cljs cljs.core/PersistentHashMap)
  (map-vals-transform [structure next-fn]
    (persistent!
      (reduce-kv
        (fn [m k v]
          (assoc! m k (next-fn v)))
        (transient
          #?(:clj clojure.lang.PersistentHashMap/EMPTY :cljs cljs.core.PersistentHashMap.EMPTY))

        structure)))


  #?(:clj Object :cljs default)
  (map-vals-transform [structure next-fn]
    (reduce-kv
      (fn [m k v]
        (assoc m k (next-fn v)))
      (empty structure)
      structure)))


(defn srange-select [structure start end next-fn]
  (next-fn (-> structure vec (subvec start end))))

(def srange-transform i/srange-transform*)


(defn extract-basic-filter-fn [path]
  (cond (fn? path)
        path

        (and (coll? path)
             (every? fn? path))
        (reduce
          (fn [combined afn]
            (fn [structure]
              (and (combined structure) (afn structure))))

          path)))




(defn if-select [vals structure next-fn then-tester then-nav else-nav]
  (i/exec-select*
    (if (then-tester structure) then-nav else-nav)
    vals
    structure
    next-fn))



(defn if-transform [vals structure next-fn then-tester then-nav else-nav]
  (i/exec-transform*
    (if (then-tester structure) then-nav else-nav)
    vals
    structure
    next-fn))




(defprotocol AddExtremes
  (append-all [structure elements])
  (prepend-all [structure elements]))

(extend-protocol AddExtremes
  nil
  (append-all [_ elements]
    elements)
  (prepend-all [_ elements]
    elements)

  #?(:clj clojure.lang.PersistentVector :cljs cljs.core/PersistentVector)
  (append-all [structure elements]
    (reduce conj structure elements))
  (prepend-all [structure elements]
    (let [ret (transient [])]
      (as-> ret <>
            (reduce conj! <> elements)
            (reduce conj! <> structure)
            (persistent! <>))))


  #?(:clj Object :cljs default)
  (append-all [structure elements]
    (concat structure elements))
  (prepend-all [structure elements]
    (concat elements structure)))



(defprotocol UpdateExtremes
  (update-first [s afn])
  (update-last [s afn]))

(defprotocol GetExtremes
  (get-first [s])
  (get-last [s]))

(defprotocol FastEmpty
  (fast-empty? [s]))

(defnav PosNavigator [getter updater]
  (select* [this structure next-fn]
    (if-not (fast-empty? structure)
      (next-fn (getter structure))
      i/NONE))
  (transform* [this structure next-fn]
    (if (fast-empty? structure)
      structure
      (updater structure next-fn))))

(defn- update-first-list [l afn]
  (cons (afn (first l)) (rest l)))

(defn- update-last-list [l afn]
  (concat (butlast l) [(afn (last l))]))

#?(
   :clj
   (defn vec-count [^clojure.lang.IPersistentVector v]
     (.length v))

   :cljs
   (defn vec-count [v]
     (count v)))


#?(
   :clj
   (defn transient-vec-count [^clojure.lang.ITransientVector v]
     (.count v))

   :cljs
   (defn transient-vec-count [v]
     (count v)))


(extend-protocol UpdateExtremes
  #?(:clj clojure.lang.PersistentVector :cljs cljs.core/PersistentVector)
  (update-first [v afn]
    (let [val (nth v 0)]
      (assoc v 0 (afn val))))

  (update-last [v afn]
    ;; type-hinting vec-count to ^int caused weird errors with case
    (let [c (int (vec-count v))]
      (case c
        1 (let [[e] v] [(afn e)])
        2 (let [[e1 e2] v] [e1 (afn e2)])
        (let [i (dec c)]
          (assoc v i (afn (nth v i)))))))

  #?(:clj Object :cljs default)
  (update-first [l val]
    (update-first-list l val))
  (update-last [l val]
    (update-last-list l val)))


(extend-protocol GetExtremes
  #?(:clj clojure.lang.IPersistentVector :cljs cljs.core/PersistentVector)
  (get-first [v]
    (nth v 0))
  (get-last [v]
    (peek v))
  #?(:clj Object :cljs default)
  (get-first [s]
    (first s))
  (get-last [s]
    (last s)))



(extend-protocol FastEmpty
  nil
  (fast-empty? [_] true)

  #?(:clj clojure.lang.IPersistentVector :cljs cljs.core/PersistentVector)
  (fast-empty? [v]
    (= 0 (vec-count v)))
  #?(:clj clojure.lang.ITransientVector :cljs cljs.core/TransientVector)
  (fast-empty? [v]
    (= 0 (transient-vec-count v)))
  #?(:clj Object :cljs default)
  (fast-empty? [s]
    (empty? s)))


(defn walk-until [pred on-match-fn structure]
  (if (pred structure)
    (on-match-fn structure)
    (walk/walk (partial walk-until pred on-match-fn) identity structure)))


(defrichnav
  ^{:doc "Navigates to the specified key, navigating to nil if it does not exist."}
  keypath*
  [key]
  (select* [this vals structure next-fn]
    (next-fn vals (get structure key)))
  (transform* [this vals structure next-fn]
    (assoc structure key (next-fn vals (get structure key)))))


(defrichnav
  ^{:doc "Navigates to the key only if it exists in the map."}
  must*
  [k]
  (select* [this vals structure next-fn]
    (if (contains? structure k)
      (next-fn vals (get structure k))
      i/NONE))
  (transform* [this vals structure next-fn]
   (if (contains? structure k)
     (assoc structure k (next-fn vals (get structure k)))
     structure)))
