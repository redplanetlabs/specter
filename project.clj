(defproject com.rpl/specter "0.5.2"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 ]
  :jvm-opts ["-XX:-OmitStackTraceInFastThrow"] ; this prevents JVM from doing optimizations which can remove stack traces from NPE and other exceptions
  :source-paths ["src/clj"]
  :test-paths ["test/clj"]
  :profiles {:dev {:dependencies
                   [[org.clojure/test.check "0.5.9"]]}
             })
