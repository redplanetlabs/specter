(ns com.rpl.specter.cljs-test-runner
  (:require [cljs.test :as test :refer-macros [run-tests]]
            [com.rpl.specter.core-test]))

(run-tests 'com.rpl.specter.core-test)
