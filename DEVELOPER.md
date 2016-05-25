# Running Clojure tests

```
lein cleantest
```

# Running ClojureScript tests

```
rm -rf out/
lein do javac, cljx
rlwrap java -cp `lein classpath` clojure.main repl.clj
(require 'com.rpl.specter.cljs-test-runner)
```
