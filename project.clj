(def VERSION (.trim (slurp "VERSION")))

(defproject com.rpl/specter VERSION
  :dependencies [[org.clojure/clojure "1.7.0-RC2"]
                 ]
  :jvm-opts ["-XX:-OmitStackTraceInFastThrow"] ; this prevents JVM from doing optimizations which can remove stack traces from NPE and other exceptions
  :source-paths ["src"]
  :test-paths ["test/clj"]
  :profiles {:dev {:dependencies
                   [[org.clojure/test.check "0.5.9"]]}
             })
