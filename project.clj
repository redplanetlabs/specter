(def VERSION (.trim (slurp "VERSION")))

(defproject com.rpl/specter VERSION
  :dependencies [[org.clojure/clojure "1.7.0-RC2"]
                 ]
  :jvm-opts ["-XX:-OmitStackTraceInFastThrow"] ; this prevents JVM from doing optimizations which can remove stack traces from NPE and other exceptions
  :plugins [[lein-cljsbuild "1.0.6"]]
  :source-paths ["src"]
  :test-paths ["test"]
  :profiles {:dev {:dependencies
                   [[org.clojure/test.check "0.5.9"]]}
             }
  :cljsbuild {
    :builds [{
        :source-paths ["src"]
        :test-paths ["test"]
        :compiler {
          :output-to "target/cljsbuild-main.js"}}]
    })
