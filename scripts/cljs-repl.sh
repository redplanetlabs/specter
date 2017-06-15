#!/bin/bash

rlwrap java -cp `lein classpath` clojure.main scripts/repl.clj

