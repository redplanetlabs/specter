(ns com.rpl.specter.cljs-self-test-runner
  (:require [cljs.test :refer-macros [run-tests]]
            [com.rpl.specter.core-test]
            [com.rpl.specter.zipper-test]))

(run-tests 'com.rpl.specter.core-test
           'com.rpl.specter.zipper-test)