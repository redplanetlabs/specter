(def VERSION (.trim (slurp "VERSION")))

(defproject com.rpl/specter VERSION
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.7.228"]]
  :jvm-opts ["-XX:-OmitStackTraceInFastThrow"]
  :source-paths ["src/clj" "src/cljc"]
  :java-source-paths ["src/java"]
  :test-paths ["test"]
  :auto-clean false
  :aliases {"cleantest" ["do" "clean,"
                         "test,"
                         "doo" "phantom" "test" "once"]
            "deploy" ["do" "clean," "deploy" "clojars"]}
  :profiles {:dev {:dependencies [[org.clojure/test.check "0.7.0"]]
                   :plugins [[lein-cljsbuild "1.1.2"]
                             [lein-doo "0.1.6"]]
                   :cljsbuild {:builds [{:id "prod"
                                         :source-paths ["src/cljc"]
                                         :compiler {:output-to "target/cljs/main.js"
                                                    :optimizations :advanced
                                                    :pretty-print false}}
                                        {:id "test"
                                         :source-paths ["src/cljc" "test"]
                                         :compiler {:output-to "target/cljs/unit-test.js"
                                                    :main 'com.rpl.specter.cljs-test-runner
                                                    :optimizations :whitespace
                                                    :pretty-print false}}]}}})
