(ns com.rpl.specter.core-test
  #+cljs (:require-macros
           [cljs.test :refer [is deftest]]
           [cljs.test.check.cljs-test :refer [defspec]]
           [com.rpl.specter.cljs-test-helpers :refer [for-all+]]
           [com.rpl.specter.macros
             :refer [paramsfn defprotocolpath defpath extend-protocolpath
                     declarepath providepath]])
  (:use
    #+clj [clojure.test :only [deftest is]]
    #+clj [clojure.test.check.clojure-test :only [defspec]]
    #+clj [com.rpl.specter.test-helpers :only [for-all+]]
    #+clj [com.rpl.specter.macros
           :only [paramsfn defprotocolpath defpath extend-protocolpath
                  declarepath providepath]]

    )

  (:require #+clj [clojure.test.check.generators :as gen]
            #+clj [clojure.test.check.properties :as prop]
            #+cljs [cljs.test.check :as tc]
            #+cljs [cljs.test.check.generators :as gen]
            #+cljs [cljs.test.check.properties :as prop :include-macros true]
            [com.rpl.specter :as s]
            [clojure.set :as set]))

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
   [s (gen/vector gen/int)
    target-type (gen/elements ['() []])
    pred (gen/elements [even? odd?])
    updater (gen/elements [inc dec])]
   (let [v (into target-type s)
         v2 (s/transform [(s/filterer pred) s/ALL] updater v)
         v3 (s/transform [s/ALL pred] updater v)]
     (and (= v2 v3) (= (type v2) (type v3)))
     )))

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

(deftest atom-test
  (let [v (s/transform s/ATOM inc (atom 1))]
    (is (instance? clojure.lang.Atom v))
    (is (= 2 (s/select-one s/ATOM v) @v)))

  (is (nil? (s/select-one s/ATOM nil)))

  (let [v (s/transform s/ATOM #(if % (inc %) 1) nil)]
    (is (instance? clojure.lang.Atom v))
    (is (= 1 (s/select-one s/ATOM v) @v))))

(defspec view-test
  (for-all+
    [i gen/int
     afn (gen/elements [inc dec])]
    (= (first (s/select (s/view afn) i))
       (afn i)
       (s/transform (s/view afn) identity i)
       )))

(defspec must-test
  (for-all+
    [k1 gen/int
     k2 (gen/such-that #(not= k1 %) gen/int)
     m (gen-map-with-keys gen/int gen/int k1)
     op (gen/elements [inc dec])
     ]
    (let [m (dissoc m k2)]
      (and (= (s/transform (s/must k1) op m)
              (s/transform (s/keypath k1) op m))
           (= (s/transform (s/must k2) op m) m)
           (= (s/select (s/must k1) m) (s/select (s/keypath k1) m))
           (empty? (s/select (s/must k2) m))
           ))))

(defspec parser-test
  (for-all+
    [i gen/int
     afn (gen/elements [inc dec #(* % 2)])
     bfn (gen/elements [inc dec #(* % 2)])
     cfn (gen/elements [inc dec #(* % 2)])]
    (and (= (s/select-one! (s/parser afn bfn) i)
            (afn i))
         (= (s/transform (s/parser afn bfn) cfn i)
            (-> i afn cfn bfn))
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

(defspec subselect-nested-vectors
  (for-all+
    [v1 (gen/vector
         (gen/vector gen/int))]
    (let [path (s/comp-paths (s/subselect s/ALL s/ALL))
          v2 (s/compiled-transform path reverse v1)]
      (and
        (= (s/compiled-select path v1) [(flatten v1)])
        (= (flatten v1) (reverse (flatten v2)))
        (= (map count v1) (map count v2))))))

(defspec subselect-param-test
  (for-all+
    [k gen/keyword
     v (gen/vector
         (limit-size 5
           (gen-map-with-keys
             gen/keyword
             gen/int
             k)))]
    (and
     (= (s/compiled-select ((s/subselect s/ALL s/keypath) k) v)
        [(map k v)])
     (let [v2 (s/compiled-transform ((s/comp-paths (s/subselect s/ALL s/keypath)) k)
                                    reverse
                                    v)]
       (and (= (map k v) (reverse (map k v2)))
            (= (map #(dissoc % k) v)
               (map #(dissoc % k) v2))) ; only key k was touched in any of the maps
       ))))

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

(defspec subset-test
  (for-all+
    [s1 (gen/vector (limit-size 5 gen/keyword))
     s2 (gen/vector (limit-size 5 gen/keyword))
     s3 (gen/vector (limit-size 5 gen/int))
     s4 (gen/vector (limit-size 5 gen/keyword))]
    (let [s1 (set s1)
          s2 (set s1)
          s3 (set s1)
          s4 (set s1)
          combined (set/union s1 s2)
          ss (set/union s2 s3)]
      (and
        (= (s/transform (s/subset s3) identity combined) combined)
        (= (s/setval (s/subset s3) #{} combined) (set/difference combined s2))
        (= (s/setval (s/subset s3) s4 combined) (-> combined (set/difference s2) (set/union s4)))
        ))))

(deftest submap-test
  (is (= [{:foo 1}]
         (s/select [(s/submap [:foo :baz])] {:foo 1 :bar 2})))
  (is (= {:foo 1, :barry 1}
         (s/setval [(s/submap [:bar])] {:barry 1} {:foo 1 :bar 2})))
  (is (= {:bar 1, :foo 2}
         (s/transform [(s/submap [:foo :baz]) s/ALL s/LAST] inc {:foo 1 :bar 1})))
  (is (= {:a {:new 1}
          :c {:new 1
              :old 1}}
         (s/setval [s/ALL s/LAST (s/submap [])] {:new 1} {:a nil, :c {:old 1}}))))

(deftest nil->val-test
  (is (= {:a #{:b}}
         (s/setval [:a s/NIL->SET (s/subset #{})] #{:b} nil)))
  (is (= {:a #{:b :c :d}}
         (s/setval [:a s/NIL->SET (s/subset #{})] #{:b} {:a #{:c :d}})))
  (is (= {:a [:b]}
         (s/setval [:a s/NIL->VECTOR s/END] [:b] nil)))
  )

(defspec void-test
  (for-all+
    [s1 (gen/vector (limit-size 5 gen/int))]
    (and
      (empty? (s/select s/STOP s1))
      (empty? (s/select [s/STOP s/ALL s/ALL s/ALL s/ALL] s1))
      (= s1 (s/transform s/STOP inc s1))
      (= s1 (s/transform [s/ALL s/STOP s/ALL] inc s1))
      (= (s/transform [s/ALL (s/cond-path even? nil odd? s/STOP)] inc s1)
         (s/transform [s/ALL even?] inc s1))
      )))

(deftest stay-continue-tests
  (is (= [[1 2 [:a :b]] [3 [:a :b]] [:a :b [:a :b]]]
         (s/setval [(s/stay-then-continue s/ALL) s/END] [[:a :b]] [[1 2] [3]])))
  (is (= [[1 2 [:a :b]] [3 [:a :b]] [:a :b]]
         (s/setval [(s/continue-then-stay s/ALL) s/END] [[:a :b]] [[1 2] [3]])))
  (is (= [[1 2 3] 1 3]
         (s/select (s/stay-then-continue s/ALL odd?) [1 2 3])))
  (is (= [1 3 [1 2 3]]
         (s/select (s/continue-then-stay s/ALL odd?) [1 2 3])))
  )


(declarepath MyWalker)

(providepath MyWalker
  (s/if-path vector?
    (s/if-path [s/FIRST #(= :abc %)]
      (s/continue-then-stay s/ALL MyWalker)
      [s/ALL MyWalker]
      )))

(deftest recursive-path-test
  (is (= [9 1 10 3 1]
         (s/select [MyWalker s/ALL number?]
           [:bb [:aa 34 [:abc 10 [:ccc 9 8 [:abc 9 1]]]] [:abc 1 [:abc 3]]])
           ))
  (is (= [:bb [:aa 34 [:abc 11 [:ccc 9 8 [:abc 10 2]]]] [:abc 2 [:abc 4]]]
         (s/transform [MyWalker s/ALL number?] inc
           [:bb [:aa 34 [:abc 10 [:ccc 9 8 [:abc 9 1]]]] [:abc 1 [:abc 3]]])
           ))
  )

(declarepath map-key-walker [akey])

(providepath map-key-walker
  [s/ALL
   (s/if-path [s/FIRST (paramsfn [akey] [curr] (= curr akey))]
     s/LAST
     [s/LAST (s/params-reset map-key-walker)])])

(deftest recursive-params-path-test
  (is (= #{1 2 3} (set (s/select (map-key-walker :aaa)
                                 {:a {:aaa 3  :b {:c {:aaa 2} :aaa 1}}}))))
  (is (= {:a {:aaa 4 :b {:c {:aaa 3} :aaa 2}}}
         (s/transform (map-key-walker :aaa) inc
                      {:a {:aaa 3  :b {:c {:aaa 2} :aaa 1}}})))
  (is (= {:a {:c {:b "X"}}}
         (s/setval (map-key-walker :b) "X" {:a {:c {:b {:d 1}}}})))
  )

(deftest recursive-params-composable-path-test
  (let [p (s/comp-paths s/keypath map-key-walker)]
    (is (= [1] (s/select (p 1 :a) [{:a 3} {:a 1} {:a 2}])))
    ))

(deftest all-map-test
  (is (= {3 3} (s/transform [s/ALL s/FIRST] inc {2 3})))
  (is (= {3 21 4 31} (s/transform [s/ALL s/ALL] inc {2 20 3 30})))
  )

(declarepath NestedHigherOrderWalker [k])

(providepath NestedHigherOrderWalker
  (s/if-path vector?
    (s/if-path [s/FIRST (paramsfn [k] [e] (= k e))]
      (s/continue-then-stay s/ALL (s/params-reset NestedHigherOrderWalker))
      [s/ALL (s/params-reset NestedHigherOrderWalker)]
      )))

(deftest nested-higher-order-walker-test
  (is (= [:q [:abc :I 3] [:ccc [:abc :I] [:abc :I "a" [:abc :I [:abc :I [:d]]]]]]
         (s/setval [(NestedHigherOrderWalker :abc) (s/srange 1 1)]
                   [:I]
                   [:q [:abc 3] [:ccc [:abc] [:abc "a" [:abc [:abc [:d]]]]]]
                   ))))

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

#+clj
(do
  (defprotocolpath AccountPath [])
  (defrecord Account [funds])
  (defrecord User [account])
  (defrecord Family [accounts])
  (extend-protocolpath AccountPath User :account Family [:accounts s/ALL])
  )

#+clj
(deftest protocolpath-basic-test
  (let [data [(->User (->Account 30))
              (->User (->Account 50))
              (->Family [(->Account 51) (->Account 52)])]]
    (is (= [30 50 51 52]
           (s/select [s/ALL AccountPath :funds] data)))
    (is (= [(->User (->Account 31))
            (->User (->Account 51))
            (->Family [(->Account 52) (->Account 53)])]
           (s/transform [s/ALL AccountPath :funds]
                      inc
                      data)))
    ))

#+clj
(do
  (defprotocolpath LabeledAccountPath [label])
  (defrecord LabeledUser [account])
  (defrecord LabeledFamily [accounts])
  (extend-protocolpath LabeledAccountPath
    LabeledUser [:account s/keypath]
    LabeledFamily [:accounts s/keypath s/ALL])
  )

#+clj
(deftest protocolpath-params-test
  (let [data [(->LabeledUser {:a (->Account 30)})
              (->LabeledUser {:a (->Account 50)})
              (->LabeledFamily {:a [(->Account 51) (->Account 52)]})]]
    (is (= [30 50 51 52]
           (s/select [s/ALL (LabeledAccountPath :a) :funds] data)))
    (is (= [(->LabeledUser {:a (->Account 31)})
            (->LabeledUser {:a (->Account 51)})
            (->LabeledFamily {:a [(->Account 52) (->Account 53)]})]
           (s/transform [s/ALL (LabeledAccountPath :a) :funds]
                      inc
                      data)))
    ))


#+clj
(do
  (defprotocolpath CustomWalker [])
  (extend-protocolpath CustomWalker
    Object nil
    clojure.lang.PersistentHashMap [(s/keypath :a) CustomWalker]
    clojure.lang.PersistentArrayMap [(s/keypath :a) CustomWalker]
    clojure.lang.PersistentVector [s/ALL CustomWalker]
    )

  )

#+clj
(deftest mixed-rich-regular-protocolpath
  (is (= [1 2 3 11 21 22 25]
         (s/select [CustomWalker number?] [{:a [1 2 :c [3]]} [[[[[[11]]] 21 [22 :c 25]]]]])))
  (is (= [2 3 [[[4]] :b 0] {:a 4 :b 10}]
         (s/transform [CustomWalker number?] inc [1 2 [[[3]] :b -1] {:a 3 :b 10}])))
  )

#+cljs
(defn make-queue [coll]
  (reduce
    #(conj %1 %2)
    #queue []
    coll))

#+clj
(defn make-queue [coll]
  (reduce
    #(conj %1 %2)
    clojure.lang.PersistentQueue/EMPTY
    coll))

(defspec transform-idempotency 50
         (for-all+
           [v1 (gen/vector gen/int)
            l1 (gen/list gen/int)
            m1 (gen/map gen/keyword gen/int)]
           (let [s1 (set v1)
                 q1 (make-queue v1)
                 v2 (s/transform s/ALL identity v1)
                 m2 (s/transform s/ALL identity m1)
                 s2 (s/transform s/ALL identity s1)
                 l2 (s/transform s/ALL identity l1)
                 q2 (s/transform s/ALL identity q1)]
             (and
               (= v1 v2)
               (= (type v1) (type v2))
               (= m1 m2)
               (= (type m1) (type m2))
               (= s1 s2)
               (= (type s1) (type s2))
               (= l1 l2)
               (seq? l2) ; Transformed lists are only guaranteed to impelment ISeq
               (= q1 q2)
               (= (type q1) (type q2))))))

