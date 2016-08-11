(require 'cljs.repl)
(require 'cljs.build.api)
(require 'cljs.repl.node)

(cljs.build.api/build "target/classes/com/rpl"
  {:output-to "out/main.js"
   :verbose true
   :warning-handlers [(fn [warning-type env extra]
                       (when (warning-type cljs.analyzer/*cljs-warnings*)
                         (when-let [s (cljs.analyzer/error-message warning-type extra)]
                           (binding [*out* *err*]
                             (println "WARNING:" (cljs.analyzer/message env s))
                             (println "Failed to build because of warning!"))

                           (System/exit 1))))]})

(cljs.repl/repl (cljs.repl.node/repl-env)
  :watch "target/classes/com/rpl"
  :output-dir "out"
  :static-fns true)
