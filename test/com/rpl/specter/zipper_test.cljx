(ns com.rpl.specter.zipper-test
  #+cljs (:require-macros
           [cljs.test :refer [is deftest]]
           [cljs.test.check.cljs-test :refer [defspec]]
           [com.rpl.specter.cljs-test-helpers :refer [for-all+]]
           [com.rpl.specter.macros
             :refer [declarepath providepath]]
           )
  (:use
    #+clj [clojure.test :only [deftest is]]
    #+clj [clojure.test.check.clojure-test :only [defspec]]
    #+clj [com.rpl.specter.test-helpers :only [for-all+]]
    #+clj [com.rpl.specter.macros
           :only [declarepath providepath]]
    )
  (:require #+clj [clojure.test.check.generators :as gen]
            #+clj [clojure.test.check.properties :as prop]
            #+cljs [cljs.test.check :as tc]
            #+cljs [cljs.test.check.generators :as gen]
            #+cljs [cljs.test.check.properties :as prop :include-macros true]
            [com.rpl.specter :as s]
            [com.rpl.specter.zipper :as z]))

(defspec zipper-end-equivalency-test
  (for-all+
    [v (gen/not-empty (gen/vector gen/int))
     i (gen/vector gen/int)]
    (= (s/setval s/END i v)
       (s/setval [z/VECTOR-ZIP z/DOWN z/RIGHTMOST z/INNER-RIGHT] i v))
    ))

(deftest zipper-multi-insert-test
  (is (= [1 2 :a :b 3 :a :b 4]
         (s/setval [z/VECTOR-ZIP
                    z/DOWN
                    z/RIGHT
                    z/RIGHT
                    (s/multi-path z/INNER-RIGHT z/INNER-LEFT)
                    ]
           [:a :b]
           [1 2 3 4]
           )
          (s/setval [z/VECTOR-ZIP
                     z/DOWN
                     z/RIGHT
                     z/RIGHT
                     (s/multi-path z/INNER-LEFT z/INNER-RIGHT)
                     ]
           [:a :b]
           [1 2 3 4]
           )
         ))
  )

(deftest zipper-down-up-test
  (is (= [1 [2 3 5] 6]
         (s/transform [z/VECTOR-ZIP
                       z/DOWN
                       z/RIGHT
                       z/DOWN
                       z/RIGHT
                       z/RIGHT
                       (s/multi-path
                         s/STAY
                         [z/UP z/RIGHT])
                       z/NODE]
           inc
           [1 [2 3 4] 5]
           )))
  )

(declarepath NEXT-WALKER)

(providepath NEXT-WALKER
  (s/stay-then-continue
    z/NEXT
    NEXT-WALKER
    ))


(deftest next-terminate-test
  (is (= [2 [3 4 [5]] 6]
         (s/transform [z/VECTOR-ZIP NEXT-WALKER z/NODE number?]
           inc
           [1 [2 3 [4]] 5])))
  )

(deftest zipper-nav-stop-test
  (is (= [1]
         (s/transform [z/VECTOR-ZIP z/UP z/NODE] inc [1])))
  (is (= [1]
         (s/transform [z/VECTOR-ZIP z/DOWN z/LEFT z/NODE] inc [1])))
  (is (= [1]
         (s/transform [z/VECTOR-ZIP z/DOWN z/RIGHT z/NODE] inc [1])))
  (is (= []
         (s/transform [z/VECTOR-ZIP z/DOWN z/NODE] inc [])))
  )

