# Specter [![Build Status](https://travis-ci.org/nathanmarz/specter.svg?branch=master)](http://travis-ci.org/nathanmarz/specter)

Specter is library for Clojure and ClojureScript for querying and manipulating arbitrarily complicated data structures very concisely. Its use cases range from transforming the values of a map to manipulating deeply nested data structures to performing sophisticated recursive tree transformations. Without Specter, writing these manipulations in Clojure manually is cumbersome and prone to error. 

Specter is fully extensible. At its core, it's just a protocol for how to navigate within a data structure. By extending this protocol, you can use Specter to navigate any data structure or object you have.

Even though Specter is so generic and flexible, [its performance](https://github.com/nathanmarz/specter/wiki/Specter-0.11.0:-Performance-without-the-tradeoffs) rivals hand-optimized code. The only comparable functions in Clojure's core library are `get-in` and `update-in`. The equivalent Specter code is effectively identical (just different order of arguments), but Specter runs 30% faster than `get-in` and 5x faster than `update-in`. 

# Latest Version

The latest release version of Specter is hosted on [Clojars](https://clojars.org):

[![Current Version](https://clojars.org/com.rpl/specter/latest-version.svg)](https://clojars.org/com.rpl/specter)

# Learn Specter

- Introductory blog post: [Functional-navigational programming in Clojure(Script) with Specter](http://nathanmarz.com/blog/functional-navigational-programming-in-clojurescript-with-sp.html)
- Presentation about Specter: [Specter: Powerful and Simple Data Structure Manipulation](https://www.youtube.com/watch?v=VTCy_DkAJGk)
- [API docs](http://nathanmarz.github.io/specter/)
- Performance guide: The [Specter 0.11.0 announcement post](https://github.com/nathanmarz/specter/wiki/Specter-0.11.0:-Performance-without-the-tradeoffs) provides a comprehensive overview of how Specter achieves its performance and what you need to know as a user to enable Specter to perform its optimizations.

Specter's API is contained in three files:

- [macros.clj](https://github.com/nathanmarz/specter/blob/master/src/clj/com/rpl/specter/macros.clj): This contains the core `select/transform/etc.` operations as well as macros for defining new navigators.
- [specter.cljx](https://github.com/nathanmarz/specter/blob/master/src/clj/com/rpl/specter.cljx): This contains the build-in navigators and functional versions of `select/transform/etc.`
- [zippers.cljx](https://github.com/nathanmarz/specter/blob/master/src/clj/com/rpl/specter/zipper.cljx): This integrates zipper-based navigation into Specter.

# Questions?

You can ask questions about Specter by [opening an issue](https://github.com/nathanmarz/specter/issues?utf8=%E2%9C%93&q=is%3Aissue+label%3Aquestion+) on Github.

You can also find help in the #specter channel on [Clojurians](http://clojurians.net/).

# Examples

Increment all the values in a map:
```clojure
user> (use 'com.rpl.specter)
user> (use 'com.rpl.specter.macros)
user> (transform [ALL LAST]
              inc
              {:a 1 :b 2 :c 3})
{:a 2, :b 3, :c 4}
```

Increment all the values in maps of maps:
```clojure
user> (transform [ALL LAST ALL LAST]
              inc
              {:a {:aa 1} :b {:ba -1 :bb 2}})
{:a {:aa 2}, :b {:ba 0, :bb 3}}
```

Do the previous example more concisely:
```clojure
user> (def MAP-VALS (comp-paths ALL LAST))
user> (transform [MAP-VALS MAP-VALS]
              inc
              {:a {:aa 1} :b {:ba -1 :bb 2}})
{:a {:aa 2}, :b {:ba 0, :bb 3}}
```

Increment all the even values for :a keys in a sequence of maps:

```clojure
user> (transform [ALL :a even?]
              inc
              [{:a 1} {:a 2} {:a 4} {:a 3}])
[{:a 1} {:a 3} {:a 5} {:a 3}]
```

Retrieve every number divisible by 3 out of a sequence of sequences:
```clojure
user> (select [ALL ALL #(= 0 (mod % 3))]
              [[1 2 3 4] [] [5 3 2 18] [2 4 6] [12]])
[3 3 18 6 12]
```

Increment the last odd number in a sequence:

```clojure
user> (transform [(filterer odd?) LAST]
              inc
              [2 1 3 6 9 4 8])
[2 1 3 6 10 4 8]
```

Increment all the odd numbers between indices 1 (inclusive) and 4 (exclusive):

```clojure
user> (transform [(srange 1 4) ALL odd?] inc [0 1 2 3 4 5 6 7])
[0 2 2 4 4 5 6 7]
```

Replace the subsequence from index 2 to 4 with [:a :b :c :d :e]:

```clojure
user> (setval (srange 2 4) [:a :b :c :d :e] [0 1 2 3 4 5 6 7 8 9])
[0 1 :a :b :c :d :e 4 5 6 7 8 9]
```

Concatenate the sequence [:a :b] to every nested sequence of a sequence:

```clojure
user> (setval [ALL END] [:a :b] [[1] '(1 2) [:c]])
[[1 :a :b] (1 2 :a :b) [:c :a :b]]
```

Get all the numbers out of a map, no matter how they're nested:

```clojure
user> (select (walker number?)
              {2 [1 2 [6 7]] :a 4 :c {:a 1 :d [2 nil]}})
[2 1 2 1 2 6 7 4]
```

Navigate via non-keyword keys:

```clojure
user> (select [(keypath "a") (keypath "b")]
              {"a" {"b" 10}})
[10]
```

Reverse the positions of all even numbers between indices 4 and 11:

```clojure
user> (transform [(srange 4 11) (filterer even?)]
              reverse
              [0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15])
[0 1 2 3 10 5 8 7 6 9 4 11 12 13 14 15]
```

Decrement every value in a map:

```clojure
user> (transform [ALL LAST]
              dec
              {:a 1 :b 3})
{:b 2 :a 0}
```

Append [:c :d] to every subsequence that has at least two even numbers:
```clojure
user> (setval [ALL
               (selected? (filterer even?) (view count) #(>= % 2))
               END]
              [:c :d]
              [[1 2 3 4 5 6] [7 0 -1] [8 8] []])
[[1 2 3 4 5 6 :c :d] [7 0 -1] [8 8 :c :d] []]
```

When doing more involved transformations, you often find you lose context when navigating deep within a data structure and need information "up" the data structure to perform the transformation. Specter solves this problem by allowing you to collect values during navigation to use in the transform function. Here's an example which transforms a sequence of maps by adding the value of the :b key to the value of the :a key, but only if the :a key is even:

```clojure
user> (transform [ALL (collect-one :b) :a even?]
              +
              [{:a 1 :b 3} {:a 2 :b -10} {:a 4 :b 10} {:a 3}])
[{:b 3, :a 1} {:b -10, :a -8} {:b 10, :a 14} {:a 3}]
```

The transform function receives as arguments all the collected values followed by the navigated to value. So in this case `+` receives the value of the :b key followed by the value of the :a key, and the transform is performed to :a's value. 

The four built-in ways for collecting values are `VAL`, `collect`, `collect-one`, and `putval`. `VAL` just adds whatever element it's currently on to the value list, while `collect` and `collect-one` take in a selector to navigate to the desired value. `collect` works just like `select` by finding a sequence of values, while `collect-one` expects to only navigate to a single value. Finally, `putval` adds an external value into the collected values list.


Increment the value for :a key by 10:
```clojure
user> (transform [:a (putval 10)]
              +
              {:a 1 :b 3})
{:b 3 :a 11}
```


For every map in a sequence, increment every number in :c's value if :a is even or increment :d if :a is odd:

```clojure
user> (transform [ALL (if-path [:a even?] [:c ALL] :d)]
              inc
              [{:a 2 :c [1 2] :d 4} {:a 4 :c [0 10 -1]} {:a -1 :c [1 1 1] :d 1}])
[{:c [2 3], :d 4, :a 2} {:c [1 11 0], :a 4} {:c [1 1 1], :d 2, :a -1}]
```

"Protocol paths" can be used to navigate on polymorphic data. For example, if you have two ways of storing "account" information:

```clojure
(defrecord Account [funds])
(defrecord User [account])
(defrecord Family [accounts-list])
```

You can make an "AccountPath" that dynamically chooses its path based on the type of element it is currently navigated to:


```clojure
(use 'com.rpl.specter.macros)
(defprotocolpath AccountPath [])
(extend-protocolpath AccountPath
  User :account
  Family [:accounts-list ALL])
```

Then, here is how to select all the funds out of a list of `User` and `Family`:

```clojure
user> (select [ALL AccountPath :funds]
        [(->User (->Account 50))
         (->User (->Account 51))
         (->Family [(->Account 1)
                    (->Account 2)])
         ])
[50 51 1 2]
```

The next examples demonstrate recursive navigation. Here's how to double all the even numbers in a tree:

```clojure
(defprotocolpath TreeWalker [])

(extend-protocolpath TreeWalker
  Object nil
  clojure.lang.PersistentVector [ALL TreeWalker])

(transform [TreeWalker number? even?] #(* 2 %) [:a 1 [2 [[[3]]] :e] [4 5 [6 7]]])
;; => [:a 1 [4 [[[3]]] :e] [8 5 [12 7]]]
```

Here's how to reverse the positions of all even numbers in a tree (with order based on a depth first search). This example uses conditional navigation instead of protocol paths to do the walk:

```clojure
(declarepath TreeValues)

(providepath TreeValues
  (if-path vector?
    [ALL TreeValues]
    STAY
    ))


(transform (subselect TreeValues even?)
  reverse
  [1 2 [3 [[4]] 5] [6 [7 8] 9 [[10]]]]
  )

;; => [1 10 [3 [[8]] 5] [6 [7 4] 9 [[2]]]]
```

# Future work
- Integrate Specter with other kinds of data structures, such as graphs. Desired navigations include: reduction in topological order, navigate to outgoing/incoming nodes, to a subgraph (with metadata indicating how to attach external edges on transformation), to node attributes, to node values, to specific nodes.
- Make it possible to parallelize selects/transforms
- Any connection to transducers?

# License

Copyright 2015-2016 Red Planet Labs, Inc. Specter is licensed under Apache License v2.0.
