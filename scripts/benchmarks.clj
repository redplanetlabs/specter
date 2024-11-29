(ns com.rpl.specter.benchmarks
  (:use [com.rpl.specter]
        [com.rpl.specter.transients])
  (:require [clojure.walk :as walk]
            [com.rpl.specter.impl :as i]
            [criterium.core :as bench]))

(defn pretty-float3 [anum]
  (format "%.3g" anum))

(defn mean [a-fn]
  (-> a-fn (bench/benchmark* {}) :mean first (* 1000000)))

(defn compare-benchmark [afn-map]
  (let [results (transform MAP-VALS mean afn-map)
        [[_ best-time] & _ :as sorted] (sort-by last results)]
    (println "\nMean(us)\tvs best\t\tCode")
    (doseq [[k t] sorted]
      (println (pretty-float3 t) "\t\t" (pretty-float3 (/ t best-time 1.0)) "\t\t" k))))

(defmacro run-benchmark [name & exprs]
  (let [only-benchmarks (set (filter some? *command-line-args*))
        all-benchmarks? (empty? only-benchmarks)]
    (if (or all-benchmarks? (contains? only-benchmarks name))
      (let [afn-map (->> exprs shuffle (map (fn [e] [`(quote ~e) `(fn [] ~e)])) (into {}))]
        `(do
           (println "Benchmark:" ~name)
           (compare-benchmark ~afn-map)
           (println "\n********************************\n"))))))

(defn specter-dynamic-nested-get [data a b c]
  (select-any (keypath a b c) data))


(defn get-k [k] (fn [m next] (next (get m k))))

(def get-a-b-c
  (reduce
    (fn [curr afn]
      (fn [structure]
        (afn structure curr)))
    [identity (get-k :c) (get-k :b) (get-k :a)]))

(let [data {:a {:b {:c 1}}}
      p (comp-paths :a :b :c)]
  (run-benchmark "get value in nested map"
    (select-any [:a :b :c] data)
    (select-any (keypath :a :b :c) data)
    (select-one [:a :b :c] data)
    (select-first [:a :b :c] data)
    (select-one! [:a :b :c] data)
    (compiled-select-any p data)
    (specter-dynamic-nested-get data :a :b :c)
    (get-in data [:a :b :c])
    (get-a-b-c data)
    (-> data :a :b :c identity)
    (-> data (get :a) (get :b) (get :c))
    (-> data :a :b :c)
    (select-any [(keypath :a) (keypath :b) (keypath :c)] data)))


(let [data {:a {:b {:c 1}}}]
  (run-benchmark "set value in nested map"
    (assoc-in data [:a :b :c] 1)
    (setval [:a :b :c] 1 data)))


;; because below 1.7 there is no update function
(defn- my-update [m k afn]
  (assoc m k (afn (get m k))))

(defn manual-transform [m afn]
  (my-update m :a
    (fn [m2]
      (my-update m2 :b
        (fn [m3]
          (my-update m3 :c afn))))))

(let [data {:a {:b {:c 1}}}]
  (run-benchmark "update value in nested map"
    (update-in data [:a :b :c] inc)
    (transform [:a :b :c] inc data)
    (manual-transform data inc)))


(defn map-vals-map-iterable [^clojure.lang.IMapIterable m afn]
  (let [k-it (.keyIterator m)
        v-it (.valIterator m)]
    (loop [ret {}]
      (if (.hasNext k-it)
        (let [k (.next k-it)
              v (.next v-it)]
          (recur (assoc ret k (afn v))))

        ret))))


(defn map-vals-map-iterable-transient [^clojure.lang.IMapIterable m afn]
  (persistent!
    (let [k-it (.keyIterator m)
          v-it (.valIterator m)]
      (loop [ret (transient {})]
        (if (.hasNext k-it)
          (let [k (.next k-it)
                v (.next v-it)]
            (recur (assoc! ret k (afn v))))

          ret)))))


(let [data '(1 2 3 4 5)]
  (run-benchmark "transform values of a list"
    (transform ALL inc data)
    (doall (sequence (map inc) data))
    (reverse (into '() (map inc) data))))


(let [data {:a 1 :b 2 :c 3 :d 4}]
  (run-benchmark "transform values of a small map"
    (into {} (for [[k v] data] [k (inc v)]))
    (reduce-kv (fn [m k v] (assoc m k (inc v))) {} data)
    (persistent! (reduce-kv (fn [m k v] (assoc! m k (inc v))) (transient {}) data))
    (reduce-kv (fn [m k v] (assoc m k (inc v))) (empty data) data)
    (transform [ALL LAST] inc data)
    (transform MAP-VALS inc data)
    (zipmap (keys data) (map inc (vals data)))
    (into {} (map (fn [e] [(key e) (inc (val e))]) data))
    (into {} (map (fn [e] [(key e) (inc (val e))])) data)
    (map-vals-map-iterable data inc)
    (map-vals-map-iterable-transient data inc)))



(let [data (->> (for [i (range 1000)] [i i]) (into {}))]
  (run-benchmark "transform values of large map"
    (into {} (for [[k v] data] [k (inc v)]))
    (reduce-kv (fn [m k v] (assoc m k (inc v))) {} data)
    (persistent! (reduce-kv (fn [m k v] (assoc! m k (inc v))) (transient {}) data))
    (persistent! (reduce-kv (fn [m k v] (assoc! m k (inc v))) (transient clojure.lang.PersistentHashMap/EMPTY) data))
    (reduce-kv (fn [m k v] (assoc m k (inc v))) (empty data) data)
    (transform [ALL LAST] inc data)
    (transform MAP-VALS inc data)
    (zipmap (keys data) (map inc (vals data)))
    (into {} (map (fn [e] [(key e) (inc (val e))]) data))
    (into {} (map (fn [e] [(key e) (inc (val e))])) data)
    (map-vals-map-iterable data inc)
    (map-vals-map-iterable-transient data inc)))


(let [data [1 2 3 4 5 6 7 8 9 10]]
  (run-benchmark "first value of a size 10 vector"
    (first data)
    (select-any ALL data)
    (select-any FIRST data)
    (select-first ALL data)))


(let [data [1 2 3 4 5]]
  (run-benchmark "map a function over a vector"
    (vec (map inc data))
    (mapv inc data)
    (transform ALL inc data)
    (into [] (map inc) data)))


(let [data [1 2 3 4 5 6 7 8 9 10]]
  (run-benchmark "filter a sequence"
    (doall (filter even? data))
    (filterv even? data)
    (select [ALL even?] data)
    (select-any (filterer even?) data)
    (into [] (filter even?) data)))


(let [data [{:a 2 :b 2} {:a 1} {:a 4} {:a 6}]
      xf (comp (map :a) (filter even?))]
  (run-benchmark "even :a values from sequence of maps"
    (select [ALL :a even?] data)
    (->> data (mapv :a) (filter even?) doall)
    (into [] (comp (map :a) (filter even?)) data)
    (into [] xf data)))


(let [v (vec (range 1000))]
  (run-benchmark "Append to a large vector"
    (setval END [1] v)
    (setval AFTER-ELEM 1 v)
    (reduce conj v [1])
    (conj v 1)))

(let [data [1 2 3 4 5 6 7 8 9 10]]
  (run-benchmark "prepend to a vector"
    (vec (cons 0 data))
    (setval BEFORE-ELEM 0 data)
    (into [0] data)))


(declarepath TreeValues)

(providepath TreeValues
  (if-path vector?
    [ALL TreeValues]
    STAY))

(defprotocolpath TreeValuesProt)

(extend-protocolpath TreeValuesProt
  clojure.lang.PersistentVector [ALL TreeValuesProt]
  Object STAY)


(defn tree-value-transform [afn atree]
  (if (vector? atree)
    (mapv #(tree-value-transform afn %) atree)
    (afn atree)))


(let [data [1 2 [[3]] [4 6 [7 [8]] 10]]]
  (run-benchmark "update every value in a tree (represented with vectors)"
    (walk/postwalk (fn [e] (if (and (number? e) (even? e)) (inc e) e)) data)
    (transform [(walker number?) even?] inc data)
    (transform [TreeValues even?] inc data)
    (transform [TreeValuesProt even?] inc data)
    (tree-value-transform (fn [e] (if (even? e) (inc e) e)) data)))


(let [toappend (range 1000)]
  (run-benchmark "transient comparison: building up vectors"
    (reduce (fn [v i] (conj v i)) [] toappend)
    (reduce (fn [v i] (conj! v i)) (transient []) toappend)
    (setval END toappend [])
    (setval END! toappend (transient []))))

(let [toappend (range 1000)]
  (run-benchmark "transient comparison: building up vectors one at a time"
    (reduce (fn [v i] (conj v i)) [] toappend)
    (reduce (fn [v i] (conj! v i)) (transient []) toappend)
    (reduce (fn [v i] (setval END [i] v)) [] toappend)
    (reduce (fn [v i] (setval END! [i] v)) (transient []) toappend)))


(let [data (vec (range 1000))
      tdata (transient data)
      tdata2 (transient data)]
  (run-benchmark "transient comparison: assoc'ing in vectors"
    (assoc data 600 0)
    (assoc! tdata 600 0)
    (setval (keypath 600) 0 data)
    (setval (keypath! 600) 0 tdata2)))

(let [data (into {} (for [k (range 1000)]
                      [k (rand)]))
      tdata (transient data)
      tdata2 (transient data)]
  (run-benchmark "transient comparison: assoc'ing in maps"
    (assoc data 600 0)
    (assoc! tdata 600 0)
    (setval (keypath 600) 0 data)
    (setval (keypath! 600) 0 tdata2)))

(defn modify-submap
  [m]
  (assoc m 0 1 458 89))

(let [data (into {} (for [k (range 1000)]
                      [k (rand)]))
      tdata (transient data)]
  (run-benchmark "transient comparison: submap"
    (transform (submap [600 700]) modify-submap data)
    (transform (submap! [600 700]) modify-submap tdata)))

(let [data {:x 1}
      meta-map {:my :metadata}]
  (run-benchmark "set metadata"
    (with-meta data meta-map)
    (setval META meta-map data)))

(let [data (with-meta {:x 1} {:my :metadata})]
  (run-benchmark "get metadata"
    (meta data)
    (select-any META data)))

(let [data (with-meta {:x 1} {:my :metadata})]
  (run-benchmark "vary metadata"
    (vary-meta data assoc :y 2)
    (setval [META :y] 2 data)))

(let [data (range 1000)]
  (run-benchmark "Traverse into a set"
    (set data)
    (set (select ALL data))
    (into #{} (traverse ALL data))
    (persistent!
      (reduce conj! (transient #{}) (traverse ALL data)))
    (reduce conj #{} (traverse ALL data))))


(defn mult-10 [v] (* 10 v))

(let [data [1 2 3 4 5 6 7 8 9]]
  (run-benchmark "multi-transform vs. consecutive transforms, one shared nav"
    (->> data (transform [ALL even?] mult-10) (transform [ALL odd?] dec))
    (multi-transform [ALL (multi-path [even? (terminal mult-10)] [odd? (terminal dec)])] data)))


(let [data [[1 2 3 4 :a] [5] [6 7 :b 8 9] [10 11 12 13]]]
  (run-benchmark "multi-transform vs. consecutive transforms, three shared navs"
    (->> data (transform [ALL ALL number? even?] mult-10) (transform [ALL ALL number? odd?] dec))
    (multi-transform [ALL ALL number? (multi-path [even? (terminal mult-10)] [odd? (terminal dec)])] data)))

(let [data {:a 1 :b 2 :c 3 :d 4}]
  (run-benchmark "namespace qualify keys of a small map"
    (into {}
      (map (fn [[k v]] [(keyword (str *ns*) (name k)) v]))
      data)
    (reduce-kv (fn [m k v] (assoc m (keyword (str *ns*) (name k)) v)) {} data)
    (setval [MAP-KEYS NAMESPACE] (str *ns*) data)))



(let [data (->> (for [i (range 1000)] [(keyword (str i)) i]) (into {}))]
  (run-benchmark "namespace qualify keys of a large map"
    (into {}
      (map (fn [[k v]] [(keyword (str *ns*) (name k)) v]))
      data)
    (reduce-kv (fn [m k v] (assoc m (keyword (str *ns*) (name k)) v)) {} data)
    (setval [MAP-KEYS NAMESPACE] (str *ns*) data)))


(defnav walker-old [afn]
  (select* [this structure next-fn]
    (i/walk-select afn next-fn structure))
  (transform* [this structure next-fn]
    (i/walk-until afn next-fn structure)))

(let [data {:a [1 2 {:c '(3 4) :d {:e [1 2 3] 7 8 9 10}}]}]
  (run-benchmark "walker vs. clojure.walk version"
    (transform (walker number?) inc data)
    (transform (walker-old number?) inc data)))


(let [size 1000
      middle-idx (/ size 2)
      v -1
      rng (range size)
      data-vec (vec rng)
      data-lst (apply list rng)]
  (run-benchmark "before-index vs. srange in middle (vector)"
    (setval (before-index middle-idx) v data-vec)
    (setval (srange middle-idx middle-idx) [v] data-vec))
  (run-benchmark "before-index vs. srange in middle (list)"
    (setval (before-index middle-idx) v data-lst)
    (setval (srange middle-idx middle-idx) [v] data-lst))
  (run-benchmark "before-index at 0 vs. srange vs. cons (list)"
    (setval (before-index 0) v data-lst)
    (setval (srange 0 0) [v] data-lst)
    (cons v data-lst))
  (run-benchmark "set keypath and nthpath at index to NONE versus srange in middle (vector)"
    (setval (nthpath middle-idx) NONE data-vec)
    (setval (keypath middle-idx) NONE data-vec)
    (setval (srange middle-idx (inc middle-idx)) [] data-vec))
  (run-benchmark "set keypath and nthpath at index to NONE versus srange in middle (list)"
    ;; this case still needs to be optimized in nthpath*
    (setval (nthpath middle-idx) NONE data-lst)
    (setval (keypath middle-idx) NONE data-lst)
    (setval (srange middle-idx (inc middle-idx)) [] data-lst))
  (run-benchmark "set keypath and nthpath at beginning to NONE versus srange and subvec (vector)"
    (setval (nthpath 0) NONE data-vec)
    (setval (keypath 0) NONE data-vec)
    (setval (srange 0 1) [] data-vec)
    (subvec data-vec 1))
  (run-benchmark "set keypath and nthpath at beginning to NONE versus srange and rest (list)"
    ;; this case still needs to be optimized in nthpath*
    (setval (nthpath 0) NONE data-lst)
    (setval (keypath 0) NONE data-lst)
    (setval (srange 0 1) [] data-lst)
    (rest data-lst))
  (run-benchmark "set keypath and nthpath at end to NONE versus srange and subvec (vector)"
    (setval (nthpath (dec size)) NONE data-vec)
    (setval (keypath (dec size)) NONE data-vec)
    (setval (srange (dec size) size) [] data-vec)
    (subvec data-vec 0 (dec size))))
