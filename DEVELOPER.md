# Running Clojure tests

```
lein cleantest
```

# Running ClojureScript tests

```
lein javac
lein doo phantom test-build once
```

# Running self-hosted ClojureScript tests

Clone and `lein install` [test.check](https://github.com/clojure/test.check) so that 0.9.1-SNAPSHOT is installed locally.
 
```
scripts/test-self-host
```
