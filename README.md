# Specter
Deep introspection and transformation of data

# About

Specter is a library for concisely querying and updating nested data structures. One way to think of it is "get-in" and "assoc-in" on steroids. It is similar to the concept of a "lens" in functional programming, though it has some important extensions. 

# Latest Version

The latest release version of Specter is hosted on [Clojars](https://clojars.org):

[![Current Version](https://clojars.org/com.rpl/specter/latest-version.svg)](https://clojars.org/com.rpl/specter)

# How to use

The usage of Specter will be explained via example. Suppose you have a sequence of maps, and you want to extract all the even values for :a keys. Here's how you do it:
```clojure
user> (use 'com.rpl.specter)
nil
user> (select [ALL :a even?]
            [{:a 1} {:a 2} {:a 4} {:a 3}])
[2 4]
```

`select` extracts a sequence of results from a data structure. It takes in a "selector", which is a sequence of steps on how to navigate into that data structure. In this case, `ALL` looks at every element in the sequence, `:a` looks at the :a key for each element currently navigated to, and `even?` filters out any elements that aren't an even value.

Another function called `update` is used to perform a transformation on a data structure. In addition to a selector, it takes in an "update function" which specified what to do with each element navigated to. For example, here's how to increment all the even values for :a keys in a sequence of maps:

```clojure
user> (update [ALL :a even?]
              inc
              [{:a 1} {:a 2} {:a 4} {:a 3}])
[{:a 1} {:a 3} {:a 5} {:a 3}]
```

Specter comes with all sorts of built-in ways of navigating data structures. For example, here's how to increment the last odd number in a sequence:

```clojure
user> (update [(filterer odd?) LAST]
              inc
              [2 1 3 6 9 4 8])
[2 1 3 6 10 4 8]
```

`filterer` navigates you to a view of the sequence currently being looked at. `LAST` navigates you to the last element of whatever sequence you're looking at. But of course during updates, the updates are performed on the original data structure. 

`walker` is another useful selector that walks the data structure until a predicate is matched. Here's how to get all the numbers out of a map:

```clojure
user> (select (walker number?)
              {2 [1 2 [6 7]] :a 4 :c {:a 1 :d [2 nil]}})
[2 1 2 1 2 6 7 4]
```

When doing more involved transformations, you often find you lose context when navigating deep within a data structure and need information "up" the data structure to perform the transformation. Specter solves this problem by allowing you to collect values during navigation to use in the update function. Here's an example which transforms a sequence of maps by adding the value of the :b key to the value of the :a key, but only if the :a key is even:

```clojure
user> (update [ALL (val-selector-one :b) :a even?]
              +
              [{:a 1 :b 3} {:a 2 :b -10} {:a 4 :b 10} {:a 3}])
[{:b 3, :a 1} {:b -10, :a -8} {:b 10, :a 14} {:a 3}]
```

The update function receives as argumnets all the collected values followed by the navigated to value. So in this case `+` receives the value of the :b key followed by the value of the :a key, and the update is performed to :a's value. 

The three built-in ways for collecting values are `VAL`, `val-selector`, and `val-selector-one`. `VAL` just adds whatever element it's currently on to the value list, while `val-selector` and `val-selector-one` take in a selector to navigate to the desired value. `val-selector` works just like `select` by finding a sequence of values, while `val-selector-one` expects to only navigate to a single value.

Each step of a selector implements the `StructurePath` protocol, which looks like:

```clojure
(defprotocol StructurePath
  (select* [this vals structure next-fn])
  (update* [this vals structure next-fn])
  )
```

Looking at the implementations of the built-in operations should provide you with the guidance you need to make your own selectors.

Finally, you can make `select` and `update` work much faster by precompiling your selectors using the `comp-structure-paths` function. There's about a 5x speed difference between the following two invocations of update:

```clojure
(def precompiled (comp-structure-paths ALL :a even?))

(update [ALL :a even?] structure)
(update precompiled structure)
```


# Future work
- Make it possible to parallelize selects/updates
- Any connection to transducers?
- Add Clojurescript compatibility

# License

Copyright 2015 Red Planet Labs. Specter is licensed under Apache License v2.0.
