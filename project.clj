(def VERSION (.trim (slurp "VERSION")))

(defproject com.rpl/specter VERSION
  :jvm-opts ["-XX:-OmitStackTraceInFastThrow"] ; this prevents JVM from doing optimizations which can remove stack traces from NPE and other exceptions
             ;"-agentpath:/Applications/YourKit_Java_Profiler_2015_build_15056.app/Contents/Resources/bin/mac/libyjpagent.jnilib"]

  :source-paths ["src/clj"]
  :java-source-paths ["src/java"]
  :test-paths ["test", "target/test-classes"]
  :auto-clean false
  :dependencies [[riddley "0.1.12"]]
  :plugins [[lein-codox "0.9.5"]
            [lein-doo "0.1.7"]]
  :codox {:source-paths ["target/classes" "src/clj"]
          :namespaces [com.rpl.specter
                       com.rpl.specter.zipper
                       com.rpl.specter.protocols
                       com.rpl.specter.transients]
          :source-uri
            {#"target/classes" "https://github.com/nathanmarz/specter/tree/{version}/src/clj/{classpath}x#L{line}"
             #".*"             "https://github.com/nathanmarz/specter/tree/{version}/src/clj/{classpath}#L{line}"}}


  :cljsbuild {:builds [{:id "test-build"
                        :source-paths ["src/clj" "target/classes" "test"]
                        :compiler {:output-to "out/testable.js"
                                   :main 'com.rpl.specter.cljs-test-runner
                                   :target :nodejs
                                   :optimizations :none}}]}

  :profiles {:dev {:dependencies
                   [[org.clojure/test.check "0.9.0"]
                    [org.clojure/clojure "1.9.0"]
                    [org.clojure/clojurescript "1.9.229"]]}

             :test {:dependencies [[org.clojure/clojure "1.7.0"]]}}

  :aliases {"deploy" ["do" "clean," "deploy" "clojars"]})
