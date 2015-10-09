(require 'cljs.repl)
(require 'cljs.build.api)
(require 'cljs.repl.node)

(cljs.build.api/build "target/classes/com/rpl"
  {:output-to "out/main.js"
   :verbose true})

(cljs.repl/repl (cljs.repl.node/repl-env)
  :watch "target/classes/com/rpl"
  :output-dir "out"
  :static-fns true)
