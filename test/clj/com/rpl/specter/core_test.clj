(ns com.rpl.specter.core-test
  (:refer-clojure :exclude [update])
  (:use [clojure.test]
        [clojure.test.check.clojure-test]
        [com.rpl specter]
        [com.rpl.specter protocols]
        [com.rpl.specter test-helpers])
  (:require [clojure.test.check
             [generators :as gen]
             [properties :as prop]]
            [clojure.test.check :as qc]))

;;TODO:
;; test walk, codewalk
;; test keypath
;; test comp-structure-paths

(defn gen-map-with-keys [key-gen val-gen & keys]
  (gen/bind (gen/map key-gen val-gen)
            (fn [m]
              (gen/bind
               (apply gen/hash-map (mapcat (fn [k] [k val-gen]) keys))
               (fn [m2]
                 (gen/return (merge m m2)))))))

(defspec select-all-keyword-filter
  (for-all+
    [kw gen/keyword
     v (gen/vector (max-size 5
                     (gen-map-with-keys gen/keyword gen/int kw)))
     pred (gen/elements [odd? even?])]
    (= (select [ALL kw pred] v)
       (->> v (map kw) (filter pred))
       )))

(defspec select-pos-extreme-pred
  (for-all+
   [v (gen/vector gen/int)
    pred (gen/elements [odd? even?])
    pos (gen/elements [[FIRST first] [LAST last]])]
   (= (select-one [(filterer pred) (first pos)] v)
      (->> v (filter pred) ((last pos)))
      )))

(defspec select-all-on-map
  (for-all+
    [m (max-size 5 (gen/map gen/keyword gen/int))]
    (= (select [ALL LAST] m)
       (for [[k v] m] v))
    ))

(deftest select-one-test
   (is (thrown? Exception (select-one [ALL even?] [1 2 3 4])))
   (is (= 1 (select-one [ALL odd?] [2 4 1 6])))
   )

(deftest select-first-test
  (is (= 7 (select-first [(filterer odd?) ALL #(> % 4)] [3 4 2 3 7 5 9 8])))
  (is (nil? (select-first [ALL even?] [1 3 5 9])))
  )

(defspec transform-all-on-map
  (for-all+
    [m (max-size 5 (gen/map gen/keyword gen/int))]
    (= (transform [ALL LAST] inc m)
       (into {} (for [[k v] m] [k (inc v)]))
       )))

(defspec transform-all
  (for-all+
   [v (gen/vector gen/int)]
   (let [v2 (transform [ALL] inc v)]
    (and (vector? v2) (= v2 (map inc v)))
    )))

(defspec transform-all-list
  (for-all+
   [v (gen/list gen/int)]
   (let [v2 (transform [ALL] inc v)]
     (and (seq? v2) (= v2 (map inc v)))
     )))

(defspec transform-all-filter
  (for-all+
   [v (gen/vector gen/int)
    pred (gen/elements [odd? even?])
    action (gen/elements [inc dec])]
   (let [v2 (transform [ALL pred] action v)]
     (= v2 (map (fn [v] (if (pred v) (action v) v)) v))
     )))

(defspec transform-last
  (for-all+
   [v (gen/not-empty (gen/vector gen/int))
    pred (gen/elements [inc dec])]
   (let [v2 (transform [LAST] pred v)]
     (= v2 (concat (butlast v) [(pred (last v))]))
     )))

(defspec transform-first
  (for-all+
   [v (gen/not-empty (gen/vector gen/int))
    pred (gen/elements [inc dec])]
   (let [v2 (transform [FIRST] pred v)]
     (= v2 (concat [(pred (first v))] (rest v) ))
     )))

(defspec transform-filterer-all-equivalency
  (prop/for-all
   [v (gen/vector gen/int)]
   (let [v2 (transform [(filterer odd?) ALL] inc v)
         v3 (transform [ALL odd?] inc v)]
     (= v2 v3))
     ))

(defspec transform-with-context
  (for-all+
    [kw1 gen/keyword
     kw2 gen/keyword
     m (max-size 10 (gen-map-with-keys gen/keyword gen/int kw1 kw2))
     pred (gen/elements [odd? even?])]
    (= (transform [(collect-one kw2) kw1 pred] + m)
       (if (pred (kw1 m))
          (assoc m kw1 (+ (kw1 m) (kw2 m)))
          m
          ))))

(defn differing-elements [v1 v2]
  (->> (map vector v1 v2)
       (map-indexed (fn [i [e1 e2]]
                      (if (not= e1 e2)
                        i)))
       (filter identity)))

(defspec transform-last-compound
  (for-all+
   [v (gen/such-that #(some odd? %) (gen/vector gen/int))]
   (let [v2 (transform [(filterer odd?) LAST] inc v)
         differing-elems (differing-elements v v2)]
     (and (= (count v2) (count v))
          (= (count differing-elems) 1)
          (every? even? (drop (first differing-elems) v2))
          ))))

;; max sizes prevent too much data from being generated and keeps test from taking forever
(defspec transform-keyword
  (for-all+
   [k1 (max-size 3 gen/keyword)
    k2 (max-size 3 gen/keyword)
    m1 (max-size 5
                 (gen-map-with-keys
                  gen/keyword
                  (gen-map-with-keys gen/keyword gen/int k2)
                  k1))
    pred (gen/elements [inc dec])]
   (let [m2 (transform [k1 k2] pred m1)]
     (= (assoc-in m1 [k1 k2] nil) (assoc-in m2 [k1 k2] nil))
     (= (pred (get-in m1 [k1 k2])) (get-in m2 [k1 k2]))
     )))

(defspec replace-in-test
  (for-all+
    [v (gen/vector gen/int)]
    (let [res (->> v (map (fn [v] (if (even? v) (inc v) v))))
          user-ret (->> v
                        (filter even?)
                        (map (fn [v] [v v]))
                        (apply concat))
          user-ret (if (empty? user-ret) nil user-ret)]
      (= (replace-in [ALL even?] (fn [v] [(inc v) [v v]]) v)
         [res user-ret]
         ))))

(defspec replace-in-custom-merge
  (for-all+
    [v (gen/vector gen/int)]
    (let [res (->> v (map (fn [v] (if (even? v) (inc v) v))))
          last-even (->> v (filter even?) last)
          user-ret (if last-even {:a last-even})]
      (= (replace-in [ALL even?] (fn [v] [(inc v) v]) v :merge-fn (fn [curr new]
                                                                      (assoc curr :a new)))
         [res user-ret]
         ))))

(defspec srange-extremes-test
  (for-all+
   [v (gen/vector gen/int)
    v2 (gen/vector gen/int)]
   (let [b (setval START v2 v)
         e (setval END v2 v)]
     (and (= b (concat v2 v))
          (= e (concat v v2)))
     )))

(defspec srange-test
  (for-all+
   [v (gen/vector gen/int)
    b (gen/elements (-> v count inc range))
    e (gen/elements (range b (-> v count inc)))
    ]
   (let [sv (subvec v b e)
         predcount (fn [pred v] (->> v (filter pred) count))
         even-count (partial predcount even?)
         odd-count (partial predcount odd?)
         b (transform (srange b e) (fn [r] (filter odd? r)) v)]
     (and (= (odd-count v) (odd-count b))
          (= (+ (even-count b) (even-count sv))
             (even-count v)))
     )))

(deftest structure-path-directly-test
  (is (= 3 (select-one :b {:a 1 :b 3})))
  (is (= 5 (select-one (comp-paths :a :b) {:a {:b 5}})))
  )

(defspec view-test
  (for-all+
    [i gen/int
     afn (gen/elements [inc dec])]
    (= (first (select (view afn) i))
       (first (select (viewfn [i] (afn i)) i))
       (afn i)
       (transform (view afn) identity i)
       )))

(deftest selected?-test
  (is (= [[1 3 5] [2 :a] [7 11 4 2 :a] [10 1 :a] []]
         (setval [ALL (selected? ALL even?) END]
                 [:a]
                 [[1 3 5] [2] [7 11 4 2] [10 1] []]
                 ))))

(defspec identity-test
  (for-all+
    [i gen/int
     afn (gen/elements [inc dec])]
    (and (= [i] (select nil i))
         (= (afn i) (transform nil afn i)))))

(deftest nil-comp-test
  (is (= [5] (select (comp-paths* nil) 5))))

(defspec putval-test
  (for-all+
   [kw gen/keyword
    m (max-size 10 (gen-map-with-keys gen/keyword gen/int kw))
    c gen/int]
   (= (transform [(putval c) kw] + m)
      (transform [kw (putval c)] + m)
      (assoc m kw (+ c (get m kw)))
      )))

(defspec empty-selector-test
  (for-all+
   [v (gen/vector gen/int)]
   (= [v]
      (select [] v)
      (select nil v)
      (select (comp-paths) v)
      (select (comp-paths nil) v)
      (select [nil nil nil] v)
      )))

(defspec empty-selector-transform-test
  (for-all+
   [kw gen/keyword
    m (max-size 10 (gen-map-with-keys gen/keyword gen/int kw))]
   (and (= m
           (transform nil identity m)
           (transform [] identity m)
           (transform (comp-paths []) identity m)
           (transform (comp-paths nil nil) identity m)
           )
        (= (transform kw inc m)
           (transform [nil kw] inc m)
           (transform (comp-paths kw nil) inc m)
           (transform (comp-paths nil kw nil) inc m)
           ))))

(deftest compose-empty-comp-path-test
  (let [m {:a 1}]
    (is (= [1]
           (select [:a (comp-paths)] m)
           (select [(comp-paths) :a] m)
           ))))

(defspec mixed-selector-test
  (for-all+
   [k1 (max-size 3 gen/keyword)
    k2 (max-size 3 gen/keyword)
    m (max-size 5
                (gen-map-with-keys
                 gen/keyword
                 (gen-map-with-keys gen/keyword gen/int k2)
                 k1))]
   (= [(-> m k1 k2)]
      (select [k1 (comp-paths k2)] m)
      (select [(comp-paths k1) k2] m)
      (select [(comp-paths k1 k2) nil] m)
      (select [(comp-paths) k1 k2] m)
      (select [k1 (comp-paths) k2] m)
      )))

(deftest cond-path-test
  (is (= [4 2 6 8 10]
         (select [ALL (cond-path even? [(view inc) (view inc)]
                                 #(= 3 %) (view dec))]
                 [1 2 3 4 5 6 7 8])))
  (is (empty? (select (if-path odd? (view inc)) 2)))
  (is (= [6 2 10 6 14] 
         (transform [(putval 2)
                  ALL
                  (if-path odd? [(view inc) (view inc)] (view dec))]
                  *
                  [1 2 3 4 5]
                     )))
  (is (= 2
         (transform [(putval 2)
                  (if-path odd? (view inc))]
                  *
                  2)))
  )

(defspec cond-path-selector-test
  (for-all+
   [k1 (max-size 3 gen/keyword)
    k2 (max-size 3 gen/keyword)
    k3 (max-size 3 gen/keyword)
    m (max-size 5
                (gen-map-with-keys
                 gen/keyword
                 gen/int
                 k1
                 k2
                 k3))
    pred (gen/elements [odd? even?])
    ]
   (let [v1 (get m k1)
         k (if (pred v1) k2 k3)]
     (and
       (= (transform (if-path [k1 pred] k2 k3) inc m)
          (transform k inc m)) 
       (= (select (if-path [k1 pred] k2 k3) m)
          (select k m)) 
       ))))
