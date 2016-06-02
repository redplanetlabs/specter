(def VERSION (.trim (slurp "VERSION")))

(defproject com.rpl/specter VERSION
  :jvm-opts ["-XX:-OmitStackTraceInFastThrow"] ; this prevents JVM from doing optimizations which can remove stack traces from NPE and other exceptions
  :source-paths ["src/clj"]
  :java-source-paths ["src/java"]
  :test-paths ["test", "target/test-classes"]
  :jar-exclusions [#"\.cljx"]
  :auto-clean false
  :dependencies [[riddley "0.1.12"]]
  :plugins [[lein-codox "0.9.5"]]
  :codox {:source-paths ["target/classes" "src/clj"]
          :namespaces [com.rpl.specter
                       com.rpl.specter.macros
                       com.rpl.specter.zipper]
          :source-uri
            {#"target/classes" "https://github.com/foo/bar/blob/master/src/{classpath}x#L{line}"
             #".*"             "https://github.com/foo/bar/blob/master/{filepath}#L{line}"}
          }
  :profiles {:provided {:dependencies
                        [[org.clojure/clojure "1.7.0"]
                         [org.clojure/clojurescript "1.7.10"]]}
             :dev {:dependencies
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
