(require '[cljs.build.api]
         '[clojure.java.io :as io])

(cljs.build.api/build "scripts/self-host"
  {:main       'com.rpl.specter.self-host.test-runner
   :output-to  "target/out-self-host/main.js"
   :output-dir "target/out-self-host"
   :target     :nodejs})

(defn copy-source
  [filename]
  (let [fully-qualified (str "target/out-self-host/" filename)]
    (io/make-parents fully-qualified)
    (spit fully-qualified
      (slurp (io/resource filename)))))

;; Copy some core source files so they can be loaded by self-host tests
(copy-source "cljs/test.cljc")
(copy-source "cljs/analyzer/api.cljc")
(copy-source "clojure/template.clj")

;; Copy all test.check source out of JAR so it can be loaded by self-host tests
;; Note: If test.check adds or renames namespaces, this will need to be updated.
(copy-source "clojure/test/check.cljc")
(copy-source "clojure/test/check/clojure_test.cljc")
(copy-source "clojure/test/check/generators.cljc") 
(copy-source "clojure/test/check/impl.cljc")
(copy-source "clojure/test/check/properties.cljc")
(copy-source "clojure/test/check/random/doubles.cljs")
(copy-source "clojure/test/check/random/longs/bit_count_impl.cljs")
(copy-source "clojure/test/check/random/longs.cljs")
(copy-source "clojure/test/check/random.clj")
(copy-source "clojure/test/check/random.cljs")
(copy-source "clojure/test/check/results.cljc")
(copy-source "clojure/test/check/rose_tree.cljc")
