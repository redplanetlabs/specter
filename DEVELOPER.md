# Running Clojure tests

```
lein test
```

# Running ClojureScript tests

```
$ rlwrap java -cp `lein classpath` clojure.main repl.clj
cljs.user=> (require 'com.rpl.specter.cljs-test-runner)
```
