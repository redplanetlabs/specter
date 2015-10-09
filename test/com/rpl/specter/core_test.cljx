(ns com.rpl.specter.core-test
  #+cljs (:require-macros
           [cljs.test :refer [is deftest]]
           [cljs.test.check.cljs-test :refer [defspec]]
           [com.rpl.specter.cljs-test-helpers :refer [for-all+]]
           [com.rpl.specter.macros :refer [paramsfn]])
  (:use
    #+clj [clojure.test :only [deftest is]]
    #+clj [clojure.test.check.clojure-test :only [defspec]]
    #+clj [com.rpl.specter.test-helpers :only [for-all+]]
    #+clj [com.rpl.specter.macros :only [paramsfn]]

    )

  (:require #+clj [clojure.test.check.generators :as gen]
            #+clj [clojure.test.check.properties :as prop]
            #+cljs [cljs.test.check :as tc]
            #+cljs [cljs.test.check.generators :as gen]
            #+cljs [cljs.test.check.properties :as prop :include-macros true]
            [com.rpl.specter :as s]))

;;TODO:
;; test walk, codewalk

(defn limit-size [n {gen :gen}]
  (gen/->Generator
   (fn [rnd _size]
     (gen rnd (if (< _size n) _size n)))))

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
     v (gen/vector (limit-size 5
                     (gen-map-with-keys gen/keyword gen/int kw)))
     pred (gen/elements [odd? even?])]
    (= (s/select [s/ALL kw pred] v)
       (->> v (map kw) (filter pred))
       )))

(defspec select-pos-extreme-pred
  (for-all+
   [v (gen/vector gen/int)
    pred (gen/elements [odd? even?])
    pos (gen/elements [[s/FIRST first] [s/LAST last]])]
   (= (s/select-one [(s/filterer pred) (first pos)] v)
      (->> v (filter pred) ((last pos)))
      )))

(defspec select-all-on-map
  (for-all+
    [m (limit-size 5 (gen/map gen/keyword gen/int))]
    (= (s/select [s/ALL s/LAST] m)
       (for [[k v] m] v))
    ))

(deftest select-one-test
   (is (thrown? #+clj Exception #+cljs js/Error (s/select-one [s/ALL even?] [1 2 3 4])))
   (is (= 1 (s/select-one [s/ALL odd?] [2 4 1 6])))
   )

(deftest select-first-test
  (is (= 7 (s/select-first [(s/filterer odd?) s/ALL #(> % 4)] [3 4 2 3 7 5 9 8])))
  (is (nil? (s/select-first [s/ALL even?] [1 3 5 9])))
  )

(defspec transform-all-on-map
  (for-all+
    [m (limit-size 5 (gen/map gen/keyword gen/int))]
    (= (s/transform [s/ALL s/LAST] inc m)
       (into {} (for [[k v] m] [k (inc v)]))
       )))

(defspec transform-all
  (for-all+
   [v (gen/vector gen/int)]
   (let [v2 (s/transform [s/ALL] inc v)]
    (and (vector? v2) (= v2 (map inc v)))
    )))

(defspec transform-all-list
  (for-all+
   [v (gen/list gen/int)]
   (let [v2 (s/transform [s/ALL] inc v)]
     (and (seq? v2) (= v2 (map inc v)))
     )))

(defspec transform-all-filter
  (for-all+
   [v (gen/vector gen/int)
    pred (gen/elements [odd? even?])
    action (gen/elements [inc dec])]
   (let [v2 (s/transform [s/ALL pred] action v)]
     (= v2 (map (fn [v] (if (pred v) (action v) v)) v))
     )))

(defspec transform-last
  (for-all+
   [v (gen/not-empty (gen/vector gen/int))
    pred (gen/elements [inc dec])]
   (let [v2 (s/transform [s/LAST] pred v)]
     (= v2 (concat (butlast v) [(pred (last v))]))
     )))

(defspec transform-first
  (for-all+
   [v (gen/not-empty (gen/vector gen/int))
    pred (gen/elements [inc dec])]
   (let [v2 (s/transform [s/FIRST] pred v)]
     (= v2 (concat [(pred (first v))] (rest v) ))
     )))

(defspec transform-filterer-all-equivalency
  (prop/for-all
   [v (gen/vector gen/int)
    pred (gen/elements [even? odd?])
    updater (gen/elements [inc dec])]
   (let [v2 (s/transform [(s/filterer pred) s/ALL] updater v)
         v3 (s/transform [s/ALL pred] updater v)]
     (= v2 v3))
     ))

(defspec transform-with-context
  (for-all+
    [kw1 gen/keyword
     kw2 gen/keyword
     m (limit-size 10 (gen-map-with-keys gen/keyword gen/int kw1 kw2))
     pred (gen/elements [odd? even?])]
    (= (s/transform [(s/collect-one kw2) kw1 pred] + m)
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
   [pred (gen/elements [odd? even?])
    v (gen/such-that #(some pred %) (gen/vector gen/int))]
   (let [v2 (s/transform [(s/filterer pred) s/LAST] inc v)
         differing-elems (differing-elements v v2)]
     (and (= (count v2) (count v))
          (= (count differing-elems) 1)
          (every? (complement pred) (drop (first differing-elems) v2))
          ))))

;; max sizes prevent too much data from being generated and keeps test from taking forever
(defspec transform-keyword
  (for-all+
   [k1 (limit-size 3 gen/keyword)
    k2 (limit-size 3 gen/keyword)
    m1 (limit-size 5
                 (gen-map-with-keys
                  gen/keyword
                  (gen-map-with-keys gen/keyword gen/int k2)
                  k1))
    pred (gen/elements [inc dec])]
   (let [m2 (s/transform [k1 k2] pred m1)]
     (and (= (assoc-in m1 [k1 k2] nil) (assoc-in m2 [k1 k2] nil))
          (= (pred (get-in m1 [k1 k2])) (get-in m2 [k1 k2])))
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
      (= (s/replace-in [s/ALL even?] (fn [v] [(inc v) [v v]]) v)
         [res user-ret]
         ))))

(defspec replace-in-custom-merge
  (for-all+
    [v (gen/vector gen/int)]
    (let [res (->> v (map (fn [v] (if (even? v) (inc v) v))))
          last-even (->> v (filter even?) last)
          user-ret (if last-even {:a last-even})]
      (= (s/replace-in [s/ALL even?] (fn [v] [(inc v) v]) v :merge-fn (fn [curr new]
                                                                        (assoc curr :a new)))
         [res user-ret]
         ))))

(defspec srange-extremes-test
  (for-all+
   [v (gen/vector gen/int)
    v2 (gen/vector gen/int)]
   (let [b (s/setval s/BEGINNING v2 v)
         e (s/setval s/END v2 v)]
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
         b (s/transform (s/srange b e) (fn [r] (filter odd? r)) v)]
     (and (= (odd-count v) (odd-count b))
          (= (+ (even-count b) (even-count sv))
             (even-count v)))
     )))

(deftest structure-path-directly-test
  (is (= 3 (s/select-one :b {:a 1 :b 3})))
  (is (= 5 (s/select-one (s/comp-paths :a :b) {:a {:b 5}})))
  )

(defspec view-test
  (for-all+
    [i gen/int
     afn (gen/elements [inc dec])]
    (= (first (s/select (s/view afn) i))
       (afn i)
       (s/transform (s/view afn) identity i)
       )))

(deftest selected?-test
  (is (= [[1 3 5] [2 :a] [7 11 4 2 :a] [10 1 :a] []]
         (s/setval [s/ALL (s/selected? s/ALL even?) s/END]
                 [:a]
                 [[1 3 5] [2] [7 11 4 2] [10 1] []]
                 ))))

(defspec identity-test
  (for-all+
    [i gen/int
     afn (gen/elements [inc dec])]
    (and (= [i] (s/select nil i))
         (= (afn i) (s/transform nil afn i)))))

(deftest nil-comp-test
  (is (= [5] (s/select (com.rpl.specter.impl/comp-paths* nil) 5))))

(defspec putval-test
  (for-all+
   [kw gen/keyword
    m (limit-size 10 (gen-map-with-keys gen/keyword gen/int kw))
    c gen/int]
   (= (s/transform [(s/putval c) kw] + m)
      (s/transform [kw (s/putval c)] + m)
      (assoc m kw (+ c (get m kw)))
      )))

(defspec empty-selector-test
  (for-all+
   [v (gen/vector gen/int)]
   (= [v]
      (s/select [] v)
      (s/select nil v)
      (s/select (s/comp-paths) v)
      (s/select (s/comp-paths nil) v)
      (s/select [nil nil nil] v)
      )))

(defspec empty-selector-transform-test
  (for-all+
   [kw gen/keyword
    m (limit-size 10 (gen-map-with-keys gen/keyword gen/int kw))]
   (and (= m
           (s/transform nil identity m)
           (s/transform [] identity m)
           (s/transform (s/comp-paths []) identity m)
           (s/transform (s/comp-paths nil nil) identity m)
           )
        (= (s/transform kw inc m)
           (s/transform [nil kw] inc m)
           (s/transform (s/comp-paths kw nil) inc m)
           (s/transform (s/comp-paths nil kw nil) inc m)
           ))))

(deftest compose-empty-comp-path-test
  (let [m {:a 1}]
    (is (= [1]
           (s/select [:a (s/comp-paths)] m)
           (s/select [(s/comp-paths) :a] m)
           ))))

(defspec mixed-selector-test
  (for-all+
   [k1 (limit-size 3 gen/keyword)
    k2 (limit-size 3 gen/keyword)
    m (limit-size 5
                (gen-map-with-keys
                 gen/keyword
                 (gen-map-with-keys gen/keyword gen/int k2)
                 k1))]
   (= [(-> m k1 k2)]
      (s/select [k1 (s/comp-paths k2)] m)
      (s/select [(s/comp-paths k1) k2] m)
      (s/select [(s/comp-paths k1 k2) nil] m)
      (s/select [(s/comp-paths) k1 k2] m)
      (s/select [k1 (s/comp-paths) k2] m)
      )))

(deftest cond-path-test
  (is (= [4 2 6 8 10]
         (s/select [s/ALL (s/cond-path even? [(s/view inc) (s/view inc)]
                                 #(= 3 %) (s/view dec))]
                 [1 2 3 4 5 6 7 8])))
  (is (empty? (s/select (s/if-path odd? (s/view inc)) 2)))
  (is (= [6 2 10 6 14]
         (s/transform [(s/putval 2)
                  s/ALL
                  (s/if-path odd? [(s/view inc) (s/view inc)] (s/view dec))]
                  *
                  [1 2 3 4 5]
                     )))
  (is (= 2
         (s/transform [(s/putval 2)
                  (s/if-path odd? (s/view inc))]
                  *
                  2)))
  )

(defspec cond-path-selector-test
  (for-all+
   [k1 (limit-size 3 gen/keyword)
    k2 (limit-size 3 gen/keyword)
    k3 (limit-size 3 gen/keyword)
    m (limit-size 5
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
       (= (s/transform (s/if-path [k1 pred] k2 k3) inc m)
          (s/transform k inc m))
       (= (s/select (s/if-path [k1 pred] k2 k3) m)
          (s/select k m))
       ))))

(defspec multi-path-test
  (for-all+
    [k1 (limit-size 3 gen/keyword)
    k2 (limit-size 3 gen/keyword)
    m (limit-size 5
                (gen-map-with-keys
                 gen/keyword
                 gen/int
                 k1
                 k2))
    ]
    (= (s/transform (s/multi-path k1 k2) inc m)
       (->> m
            (s/transform k1 inc)
            (s/transform k2 inc)))
    ))

(deftest empty-pos-transform
  (is (empty? (s/select s/FIRST [])))
  (is (empty? (s/select s/LAST [])))
  (is (= [] (s/transform s/FIRST inc [])))
  (is (= [] (s/transform s/LAST inc [])))
  )

(defspec set-filter-test
  (for-all+
    [k1 gen/keyword
     k2 (gen/such-that #(not= k1 %) gen/keyword)
     k3 (gen/such-that (complement #{k1 k2}) gen/keyword)
     v (gen/vector (gen/elements [k1 k2 k3]))]
    (= (filter #{k1 k2} v) (s/select [s/ALL #{k1 k2}] v))
    ))

(deftest nil-select-one-test
  (is (= nil (s/select-one! s/ALL [nil])))
  (is (thrown? #+clj Exception #+cljs js/Error (s/select-one! s/ALL [])))
  )


(defspec transformed-test
  (for-all+
    [v (gen/vector gen/int)
     pred (gen/elements [even? odd?])
     op   (gen/elements [inc dec])]
    (= (s/select-one (s/transformed [s/ALL pred] op) v)
       (s/transform [s/ALL pred] op v))
    ))

(defspec basic-parameterized-composition-test
  (for-all+
    [k1 (limit-size 3 gen/keyword)
     k2 (limit-size 3 gen/keyword)
     m1 (limit-size 5
                 (gen-map-with-keys
                  gen/keyword
                  (gen-map-with-keys gen/keyword gen/int k2)
                  k1))
    pred (gen/elements [inc dec])]
    (let [p (s/comp-paths s/keypath s/keypath)]
      (and
        (= (s/compiled-select (p k1 k2) m1) (s/select [k1 k2] m1))
        (= (s/compiled-transform (p k1 k2) pred m1) (s/transform [k1 k2] pred m1))
        ))))

(defspec various-orders-comp-test
  (for-all+
    [k1 (limit-size 3 gen/keyword)
     k2 (limit-size 3 gen/keyword)
     k3 (limit-size 3 gen/keyword)
     m1 (limit-size 5
                 (gen-map-with-keys
                  gen/keyword
                  (gen-map-with-keys
                    gen/keyword
                    (gen-map-with-keys
                      gen/keyword
                      gen/int
                      k3
                      )
                    k2)
                  k1))
    pred (gen/elements [inc dec])]
    (let [paths [((s/comp-paths s/keypath s/keypath k3) k1 k2)
                 (s/comp-paths k1 k2 k3)
                 ((s/comp-paths s/keypath k2 s/keypath) k1 k3)
                 ((s/comp-paths k1 s/keypath k3) k2)
                 (s/comp-paths k1 (s/keypath k2) k3)
                 ((s/comp-paths (s/keypath k1) s/keypath (s/keypath k3)) k2)
                 ((s/comp-paths s/keypath (s/keypath k2) s/keypath) k1 k3)
                 ]
          ]
      (and
        (apply = (for [p paths] (s/compiled-select p m1)))
        (apply = (for [p paths] (s/compiled-transform p pred m1)))
        ))))

(defspec filterer-param-test
  (for-all+
    [k gen/keyword
     k2 gen/keyword
     v (gen/vector
         (limit-size 5
           (gen-map-with-keys
                        gen/keyword
                        gen/int
                        k
                        k2
                        )))
     pred (gen/elements [odd? even?])
     updater (gen/elements [inc dec])]
    (and
      (= (s/compiled-select ((s/filterer s/keypath pred) k) v)
         (s/compiled-select (s/filterer k pred) v))
      (= (s/compiled-transform ((s/comp-paths (s/filterer s/keypath pred) s/ALL k2) k)
           updater
           v)
         (s/compiled-transform (s/comp-paths (s/filterer k pred) s/ALL k2)
           updater
           v))
      )))

(deftest nested-param-paths
  (let [p (s/filterer s/keypath (s/selected? s/ALL s/keypath (s/filterer s/keypath even?) s/ALL))
        p2 (p :a :b :c)
        p3 (s/filterer :a (s/selected? s/ALL :b (s/filterer :c even?) s/ALL))
        data [{:a [{:b [{:c 4 :d 5}]}]}
              {:a [{:c 3}]}
              {:a [{:b [{:c 7}] :e [1]}]}]
        ]
    (is (= (s/select p2 data)
           (s/select p3 data)
           [[{:a [{:b [{:c 4 :d 5}]}]}]]
           ))
    ))

(defspec param-multi-path-test
  (for-all+
    [k1 gen/keyword
     k2 gen/keyword
     k3 gen/keyword
     m (limit-size 5
         (gen-map-with-keys
           gen/keyword
           gen/int
           k1
           k2
           k3
           ))
     pred1 (gen/elements [odd? even?])
     pred2 (gen/elements [odd? even?])
     updater (gen/elements [inc dec])
     ]
     (let [paths [((s/multi-path [s/keypath pred1] [s/keypath pred2] k3) k1 k2)
                  ((s/multi-path [k1 pred1] [s/keypath pred2] s/keypath) k2 k3)
                  ((s/multi-path [s/keypath pred1] [s/keypath pred2] s/keypath) k1 k2 k3)
                  (s/multi-path [k1 pred1] [k2 pred2] k3)
                  ((s/multi-path [k1 pred1] [s/keypath pred2] k3) k2)
                  ]]
      (and
        (apply =
          (for [p paths]
            (s/select p m)
            ))
        (apply =
          (for [p paths]
            (s/transform p updater m)
            ))
        ))))

(defspec paramsfn-test
  (for-all+
    [v (gen/vector (gen/elements (range 10)))
     val (gen/elements (range 10))
     op (gen/elements [inc dec])
     comparator (gen/elements [= > <])]
    (let [path (s/comp-paths s/ALL (paramsfn [p] [v] (comparator v p)))]
      (= (s/transform (path val) op v)
         (s/transform [s/ALL #(comparator % val)] op v)))
      ))

#+clj
(deftest large-params-test
  (let [path (apply s/comp-paths (repeat 25 s/keypath))
        m (reduce
            (fn [m k]
              {k m})
            :a
            (reverse (range 25)))]
    (is (= :a (s/select-one (apply path (range 25)) m)))
    ))
;;TODO: there's a bug in clojurescript that won't allow
;; non function implementations of IFn to have more than 20 arguments

