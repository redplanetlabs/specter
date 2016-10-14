(require
  '[cljs.repl :as repl]
  '[cljs.repl.node :as node])

(repl/repl (node/repl-env))
