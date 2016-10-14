# Running Clojure tests

```
lein cleantest
```

# Running ClojureScript tests

```
rm -rf .cljs_node_repl
lein javac
rlwrap java -cp `lein classpath` clojure.main repl.clj
(require 'com.rpl.specter.cljs-test-runner)
```
