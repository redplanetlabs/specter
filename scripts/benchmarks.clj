(ns com.rpl.specter.benchmarks
  (:use [com.rpl.specter]
        [com.rpl.specter macros]
        [com.rpl.specter.transient]
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
    (for [i (range iters)]
      (time-ms amt-per-iter afn))))

(defn compare-benchmark [iters amt-per-iter afn-map]
  (let [results (transform [ALL LAST]
                  (fn [afn]
                    (average-time-ms iters amt-per-iter afn))
                  afn-map)
        [[_ best-time] & _ :as sorted] (sort-by last results)
        ]
    (println "\nAvg(ms)\t\tvs best\t\tCode")
    (doseq [[k t] sorted]
      (println (pretty-float5 t) "\t\t" (pretty-float3 (/ t best-time 1.0)) "\t\t" k)
      )))

(defmacro run-benchmark [name iters amt-per-iter & exprs]
  (let [afn-map (->> exprs (map (fn [e] [`(quote ~e) `(fn [] ~e)])) (into {}))]
    `(do
       (println "Benchmark:" ~name)
       (compare-benchmark ~iters ~amt-per-iter ~afn-map)
       (println "\n********************************\n")
       )))



(let [data {:a {:b {:c 1}}}
      p (comp-paths :a :b :c)]
  (run-benchmark "get value in nested map" 6 10000000
    (get-in data [:a :b :c])
    (select [:a :b :c] data)
    (compiled-select p data)
    (-> data :a :b :c vector)
    )
  )


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
  (run-benchmark "update value in nested map" 6 1000000
    (update-in data [:a :b :c] inc)
    (transform [:a :b :c] inc data)
    (manual-transform data inc)
    ))

(let [data [1 2 3 4 5]]
  (run-benchmark "map a function over a vector" 6 1000000
    (vec (map inc data))
    (mapv inc data)
    (transform ALL inc data)
    ))


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
  (run-benchmark "transform values of a map" 6 1000000
    (into {} (for [[k v] data] [k (inc v)]))
    (reduce-kv (fn [m k v] (assoc m k (inc v))) {} data)
    (manual-similar-reduce-kv data)
    (transform [ALL LAST] inc data)
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
    6
    100000
    (walk/postwalk (fn [e] (if (and (number? e) (even? e)) (inc e) e)) data)
    (transform [(walker number?) even?] inc data)
    (transform [TreeValues even?] inc data)
    (transform [TreeValuesProt even?] inc data)
    (tree-value-transform (fn [e] (if (even? e) (inc e) e)) data)
    ))

(run-benchmark "transient comparison: building up vectors"
  6
  10000
  (reduce (fn [v i] (conj v i)) [] (range 1000))
  (reduce (fn [v i] (conj! v i)) (transient []) (range 1000))
  ;; uncomment this when END is fast
  #_(reduce (fn [v i] (setval [END] [i] v)) [] (range 1000))
  (setval [END!] (range 1000) (transient []))
  (reduce (fn [v i] (setval [END!] [i] v)) (transient []) (range 1000)))

(let [data (vec (range 1000))
      tdata (transient data)]
  (run-benchmark "transient comparison: assoc'ing in vectors"
    6
    500000
    (assoc data (rand-int 1000) 0)
    (assoc! tdata (rand-int 1000) 0)
    (setval [(keypath (rand-int 1000))] 0 data)
    (setval [(keypath! (rand-int 1000))] 0 tdata)))

(let [data (into {} (for [k (range 1000)]
                      [k (rand)]))
      tdata (transient data)]
  (run-benchmark "transient comparison: assoc'ing in maps"
    6
    500000
    (assoc data (rand-int 1000) 0)
    (assoc! tdata (rand-int 1000) 0)
    (setval [(keypath (rand-int 1000))] 0 data)
    (setval [(keypath! (rand-int 1000))] 0 tdata)))

(defn modify-submap
  [m]
  (assoc m 0 1 458 89))

(let [data (into {} (for [k (range 1000)]
                      [k (rand)]))
      tdata (transient data)]
  (run-benchmark "transient comparison: submap"
    6
    300000
    (transform [(submap [(rand-int 1000)])] modify-submap data)
    (transform [(submap! [(rand-int 1000)])] modify-submap tdata)))
