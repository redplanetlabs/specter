(def VERSION (.trim (slurp "VERSION")))

(defproject com.rpl/specter VERSION
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-3211"]
                 ]
  :jvm-opts ["-XX:-OmitStackTraceInFastThrow"] ; this prevents JVM from doing optimizations which can remove stack traces from NPE and other exceptions
  :source-paths ["src/clj"]
  :java-source-paths ["src/java"]
  :test-paths ["test", "target/test-classes"]
  :jar-exclusions [#"\.cljx"]
  :auto-clean false
  :profiles {:dev {:dependencies
                   [[org.clojure/test.check "0.7.0"]]
                   :plugins
                   [[com.keminglabs/cljx "0.6.0"]]
                   :cljx {:builds [{:source-paths ["src/clj"]
                                    :output-path "target/classes"
                                    :rules :clj}
                                   {:source-paths ["src/clj"]
                                    :output-path "target/classes"
                                    :rules :cljs}
                                   {:source-paths ["test"]
                                    :output-path "target/test-classes"
                                    :rules :clj}
                                   {:source-paths ["test"]
                                    :output-path "target/test-classes"
                                    :rules :cljs}]}
                   }
             }
  :aliases {"cleantest" ["do" "clean,"
                         "cljx" "once,"
                         "test"]
            "deploy" ["do" "clean," "cljx" "once," "deploy" "clojars"]})
