# Specter

Most of Clojure programming involves creating, manipulating, and transforming immutable values. However, as soon as your values become more complicated than a simple map or list – like a list of maps of maps – transforming these data structures becomes extremely cumbersome. 

Specter is a library (for both Clojure and ClojureScript) for doing these queries and transformations extremely concisely and elegantly. These kinds of manipulations are so common when using Clojure – and so cumbersome without Specter – that Specter is in many ways Clojure's missing piece.

Specter is fully extensible. At its core, its just a protocol for how to navigate within a data structure. By extending this protocol, you can use Specter to navigate any data structure or object you have.

Specter does not sacrifice performance to achieve its elegance. Actually, Specter is faster than the limited facilities Clojure provides for doing nested operations. For example: the Specter equivalent to get-in runs 30% faster than get-in, and the Specter equivalent to update-in runs 5x faster than update-in. In each case the Specter code is equally as convenient. 

# Latest Version

The latest release version of Specter is hosted on [Clojars](https://clojars.org):

[![Current Version](https://clojars.org/com.rpl/specter/latest-version.svg)](https://clojars.org/com.rpl/specter)

# Learn Specter

- Introductory blog post: [Functional-navigational programming in Clojure(Script) with Specter](http://nathanmarz.com/blog/functional-navigational-programming-in-clojurescript-with-sp.html)
- Presentation about Specter: [Specter: Clojure's missing piece](https://www.youtube.com/watch?v=mXZxkpX5nt8)

Specter's API is contained in a single, well-documented file: [specter.cljx](https://github.com/nathanmarz/specter/blob/master/src/com/rpl/specter.cljx)

# Questions?

You can ask questions about Specter by opening an issue on Github and [labeling it "question"](https://github.com/nathanmarz/specter/issues?utf8=%E2%9C%93&q=is%3Aissue+label%3Aquestion+). 

# Examples

Here's how to increment all the even values for :a keys in a sequence of maps:

```clojure
user> (transform [ALL :a even?]
              inc
              [{:a 1} {:a 2} {:a 4} {:a 3}])
[{:a 1} {:a 3} {:a 5} {:a 3}]
```

Here's how to retrieve every number divisible by 3 out of a sequence of sequences:
```clojure
user> (select [ALL ALL #(= 0 (mod % 3))]
              [[1 2 3 4] [] [5 3 2 18] [2 4 6] [12]])
[3 3 18 6 12]
```

Here's how to increment the last odd number in a sequence:

```clojure
user> (transform [(filterer odd?) LAST]
              inc
              [2 1 3 6 9 4 8])
[2 1 3 6 10 4 8]
```

Here's how to increment all the odd numbers between indexes 1 (inclusive) and 4 (exclusive):

```clojure
user> (transform [(srange 1 4) ALL odd?] inc [0 1 2 3 4 5 6 7])
[0 2 2 4 4 5 6 7]
```

Here's how to replace the subsequence from index 2 to 4 with [-1 -1 -1]:

```clojure
user> (setval (srange 2 4) [-1 -1 -1] [0 1 2 3 4 5 6 7 8 9])
[0 1 -1 -1 -1 4 5 6 7 8 9]
```

Here's how to concatenate the sequence [:a :b] to every nested sequence of a sequence:

```clojure
user> (setval [ALL END] [:a :b] [[1] '(1 2) [:c]])
[[1 :a :b] (1 2 :a :b) [:c :a :b]]
```

Here's how to get all the numbers out of a map, no matter how they're nested:

```clojure
user> (select (walker number?)
              {2 [1 2 [6 7]] :a 4 :c {:a 1 :d [2 nil]}})
[2 1 2 1 2 6 7 4]
```

Here's now to navigate via non-keyword keys:

```clojure
user> (select [(keypath "a") (keypath "b")]
              {"a" {"b" 10}})
[10]
```

Here's how to reverse the positions of all even numbers between indexes 4 and 11:

```clojure
user> (transform [(srange 4 11) (filterer even?)]
              reverse
              [0 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15])
[0 1 2 3 10 5 8 7 6 9 4 11 12 13 14 15]
```

Here's how to decrement every value in a map:

```clojure
user> (transform [ALL LAST]
              dec
              {:a 1 :b 3})
{:b 2 :a 0}
```

Here's how to append [:c :d] to every subsequence that has at least two even numbers:
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


Here's how to increment the value for :a key by 10:
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

You can make `select` and `transform` work much faster by precompiling your selectors using the `comp-paths` function. There's about a 3x speed difference between the following two invocations of transform:

```clojure
(def precompiled (comp-paths ALL :a even?))

(transform [ALL :a even?] inc structure)
(compiled-transform precompiled inc structure)
```

Depending on the details of the selector and the data being transformed, precompiling can sometimes provide more than a 10x speedup.

You can even precompile selectors that require parameters! For example, `keypath` can be used to navigate into a map by any arbitrary key, such as numbers, strings, or your own types. One way to use `keypath` would be to parameterize it at the time you use it, like so:

```clojure
(defn foo [data k]
  (select [(keypath k) ALL odd?] data))
```

It seems difficult to precompile the entire path because it is dependent on the argument `k` of `foo`. Specter gets around this by allowing you to precompile a path without its parameters and bind the parameters to the selector later, like so:

```clojure
(def foo-path (comp-paths keypath ALL odd?))
(defn foo [data k]
  (compiled-select (foo-path k) data))
```

This code will execute extremely efficiently. 

When `comp-paths` is used on selectors that require parameters, the result of `comp-paths` will require parameters equal to the sum of the number of parameters required by each selector. It expects to receive those parameters in the order in which the selectors were declared. This feature, called "late-bound parameterization", also works on selectors which themselves take in selector paths, such as `selected?`, `filterer`, and `transformed`.


# Future work
- Integrate Specter with other kinds of data structures, such as graphs
- Make it possible to parallelize selects/transforms
- Any connection to transducers?

# License

Copyright 2015 Red Planet Labs, Inc. Specter is licensed under Apache License v2.0.
