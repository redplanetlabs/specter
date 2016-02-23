(ns com.rpl.specter.cljs-test-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [com.rpl.specter.core-test]))

(doo-tests 'com.rpl.specter.core-test)
