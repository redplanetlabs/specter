(def VERSION (.trim (slurp "VERSION")))

(defproject com.rpl/specter VERSION
  :dependencies [[org.clojure/clojure "1.7.0"]
                 ;;TODO: how to make this a dep of only the cljs version?
                 [org.clojure/clojurescript "0.0-3308"]
                 ]
  :jvm-opts ["-XX:-OmitStackTraceInFastThrow"] ; this prevents JVM from doing optimizations which can remove stack traces from NPE and other exceptions
  :plugins [[lein-cljsbuild "1.0.6"]]
  :source-paths ["src"]
  :test-paths ["test"]
  :profiles {:dev {:dependencies
                   [[org.clojure/test.check "0.7.0"]]}
             }
  :cljsbuild {
    :builds {:dev
             {:source-paths ["src"]
              :compiler {
                         :output-to "target/main.js"
                         :optimizations :whitespace
                         :pretty-print true
                         }}
             }
    })
