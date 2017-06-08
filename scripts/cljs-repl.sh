#!/bin/bash

rlwrap java -cp `lein classpath` clojure.main repl.clj

