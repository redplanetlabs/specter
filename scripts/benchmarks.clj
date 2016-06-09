(ns com.rpl.specter.benchmarks
  (:use [com.rpl.specter]
        [com.rpl.specter macros]
        [com.rpl.specter.transients]
        [com.rpl.specter.impl :only [benchmark]])
  (:require [clojure.walk :as walk]))

;; run via `lein repl` with `(load-file "scripts/benchmarks.clj")`

(defn pretty-float5 [anum]
  (format "%.5g" anum))

(defn pretty-float3 [anum]
  (format "%.3g" anum))

(defn time-ms [amt afn]
  (let [start (System/nanoTime)
        _ (dotimes [_ amt] (afn))
        end (System/nanoTime)]
  (/ (- end start) 1000000.0)
  ))

(defn avg [numbers]
  (/ (reduce + numbers)
     (count numbers)
     1.0))

(defn average-time-ms [iters amt-per-iter afn]
  (avg
    ;; treat 1st run as warmup 
    (next
      (for [i (range (inc iters))]
        (time-ms amt-per-iter afn)))))

(defn compare-benchmark [amt-per-iter afn-map]
  (let [results (transform MAP-VALS
                  (fn [afn]
                    (average-time-ms 8 amt-per-iter afn))
                  afn-map)
        [[_ best-time] & _ :as sorted] (sort-by last results)
        ]
    (println "\nAvg(ms)\t\tvs best\t\tCode")
    (doseq [[k t] sorted]
      (println (pretty-float5 t) "\t\t" (pretty-float3 (/ t best-time 1.0)) "\t\t" k)
      )))

(defmacro run-benchmark [name amt-per-iter & exprs]
  (let [afn-map (->> exprs shuffle (map (fn [e] [`(quote ~e) `(fn [] ~e)])) (into {}))]
    `(do
       (println "Benchmark:" ~name)
       (compare-benchmark ~amt-per-iter ~afn-map)
       (println "\n********************************\n")
       )))

(let [data {:a {:b {:c 1}}}
      p (comp-paths :a :b :c)]
  (run-benchmark "get value in nested map" 5000000
    (select-any [:a :b :c] data)
    (select-one [:a :b :c] data)
    (select-first [:a :b :c] data)
    (select-one! [:a :b :c] data)
    (compiled-select-any p data)
    (get-in data [:a :b :c])
    (-> data :a :b :c)
    (select-any [(keypath :a) (keypath :b) (keypath :c)] data)
    ))


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
  (run-benchmark "update value in nested map" 500000
    (update-in data [:a :b :c] inc)
    (transform [:a :b :c] inc data)
    (manual-transform data inc)
    ))

(let [data [1 2 3 4 5]]
  (run-benchmark "map a function over a vector" 1000000
    (vec (map inc data))
    (mapv inc data)
    (transform ALL inc data)
    ))

(let [data [1 2 3 4 5 6 7 8 9 10]]
  (run-benchmark "filter a sequence" 1000000
    (doall (filter even? data))
    (filterv even? data)
    (select [ALL even?] data)
    (select-any (filterer even?) data)
    ))

(let [data [{:a 2 :b 2} {:a 1} {:a 4} {:a 6}]]
  (run-benchmark "even :a values from sequence of maps" 1000000
    (select [ALL :a even?] data)
    (->> data (mapv :a) (filter even?) doall)
    ))

(let [v (vec (range 1000))]
  (run-benchmark "END on large vector"
    5000000
    (setval END [1] v)
    (reduce conj v [1])
    (conj v 1)))

(defn- update-pair [[k v]]
  [k (inc v)])

(defn manual-similar-reduce-kv [data]
  (reduce-kv
    (fn [m k v]
      (let [[newk newv] (update-pair [k v])]
        (assoc m newk newv)))
    {}
    data
    ))

(let [data {:a 1 :b 2 :c 3 :d 4}]
  (run-benchmark "transform values of a map" 1000000
    (into {} (for [[k v] data] [k (inc v)]))
    (reduce-kv (fn [m k v] (assoc m k (inc v))) {} data)
    (manual-similar-reduce-kv data)
    (transform [ALL LAST] inc data)
    (transform MAP-VALS inc data)
    ))

(let [data (->> (for [i (range 1000)] [i i]) (into {}))]
  (run-benchmark "transform values of large map" 1000
    (into {} (for [[k v] data] [k (inc v)]))
    (reduce-kv (fn [m k v] (assoc m k (inc v))) {} data)
    (manual-similar-reduce-kv data)
    (transform [ALL LAST] inc data)
    (transform MAP-VALS inc data)
    ))


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
    (afn atree)
    ))

(let [data [1 2 [[3]] [4 6 [7 [8]] 10]]]
  (run-benchmark "update every value in a tree (represented with vectors)"
    50000
    (walk/postwalk (fn [e] (if (and (number? e) (even? e)) (inc e) e)) data)
    (transform [(walker number?) even?] inc data)
    (transform [TreeValues even?] inc data)
    (transform [TreeValuesProt even?] inc data)
    (tree-value-transform (fn [e] (if (even? e) (inc e) e)) data)
    ))

(let [toappend (range 1000)]
  (run-benchmark "transient comparison: building up vectors"
    10000
    (reduce (fn [v i] (conj v i)) [] toappend)
    (reduce (fn [v i] (conj! v i)) (transient []) toappend)
    (setval END toappend [])
    (setval END! toappend (transient []))))

(let [toappend (range 1000)]
  (run-benchmark "transient comparison: building up vectors one at a time"
    10000
    (reduce (fn [v i] (conj v i)) [] toappend)
    (reduce (fn [v i] (conj! v i)) (transient []) toappend)
    (reduce (fn [v i] (setval END [i] v)) [] toappend)
    (reduce (fn [v i] (setval END! [i] v)) (transient []) toappend)
    ))

(let [data (vec (range 1000))
      tdata (transient data)
      tdata2 (transient data)
      idx 600]
  (run-benchmark "transient comparison: assoc'ing in vectors"
    2500000
    (assoc data idx 0)
    (assoc! tdata idx 0)
    (setval (keypath idx) 0 data)
    (setval (keypath! idx) 0 tdata2)))

(let [data (into {} (for [k (range 1000)]
                      [k (rand)]))
      tdata (transient data)
      tdata2 (transient data)
      idx 600]
  (run-benchmark "transient comparison: assoc'ing in maps"
    2500000
    (assoc data idx 0)
    (assoc! tdata idx 0)
    (setval (keypath idx) 0 data)
    (setval (keypath! idx) 0 tdata2)))

(defn modify-submap
  [m]
  (assoc m 0 1 458 89))

(let [data (into {} (for [k (range 1000)]
                      [k (rand)]))
      tdata (transient data)]
  (run-benchmark "transient comparison: submap"
    300000
    (transform (submap [600 700]) modify-submap data)
    (transform (submap! [600 700]) modify-submap tdata)))

(let [data {:x 1}
      meta-map {:my :metadata}]
  (run-benchmark "set metadata"
    2000000
    (with-meta data meta-map)
    (setval META meta-map data)))

(let [data (with-meta {:x 1} {:my :metadata})]
  (run-benchmark "get metadata"
    20000000
    (meta data)
    (select-any META data)))

(let [data (with-meta {:x 1} {:my :metadata})]
  (run-benchmark "vary metadata"
    2000000
    (vary-meta data assoc :y 2)
    (setval [META :y] 2 data)))
