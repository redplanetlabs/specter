# Specter [![Build Status](https://secure.travis-ci.org/redplanetlabs/specter.png?branch=master)](http://travis-ci.org/redplanetlabs/specter)

Specter rejects Clojure's restrictive approach to immutable data structure manipulation, instead exposing an elegant API to allow any sort of manipulation imaginable. Specter especially excels at querying and transforming nested and recursive data, important use cases that are very complex to handle with vanilla Clojure.

Specter has an extremely simple core, just a single abstraction called "navigator". Queries and transforms are done by composing navigators into a "path" precisely targeting what you want to retrieve or change. Navigators can be composed with any other navigators, allowing sophisticated manipulations to be expressed very concisely.

In addition, Specter has performance rivaling hand-optimized code  (see [this benchmark](https://gist.github.com/nathanmarz/b7c612b417647db80b9eaab618ff8d83)). Clojure's only comparable built-in operations are `get-in` and `update-in`, and the Specter equivalents are 30% and 85% faster respectively (while being just as concise). Under the hood, Specter uses [advanced dynamic techniques](https://github.com/redplanetlabs/specter/wiki/Specter's-inline-caching-implementation) to strip away the overhead of composition.

There are some key differences between the Clojure approach to data manipulation and the Specter approach. Unlike Clojure, Specter always uses the most efficient method possible to implement an operation for a datatype (e.g. `last` vs. `LAST`). Clojure intentionally leaves out many operations, such as prepending to a vector or inserting into the middle of a sequence. Specter has navigators that cover these use cases (`BEFORE-ELEM` and `before-index`) and many more. Finally, Specter transforms always target precise parts of a data structure, leaving everything else the same. For instance, `ALL` targets every value within a sequence, and the resulting transform is always the same type as the input (e.g. a vector stays a vector, a sorted map stays a sorted map).

Consider these examples:

**Example 1: Increment every even number nested within map of vector of maps**

```clojure
(def data {:a [{:aa 1 :bb 2}
               {:cc 3}]
           :b [{:dd 4}]})

;; Manual Clojure
(defn map-vals [m afn]
  (->> m (map (fn [[k v]] [k (afn v)])) (into (empty m))))

(map-vals data
  (fn [v]
    (mapv
      (fn [m]
        (map-vals
          m
          (fn [v] (if (even? v) (inc v) v))))
      v)))

;; Specter
(transform [MAP-VALS ALL MAP-VALS even?] inc data)
```

**Example 2: Append a sequence of elements to a nested vector**

```clojure
(def data {:a [1 2 3]})

;; Manual Clojure
(update data :a (fn [v] (into (if v v []) [4 5])))

;; Specter
(setval [:a END] [4 5] data)
```

**Example 3: Increment the last odd number in a sequence**

```clojure
(def data [1 2 3 4 5 6 7 8])

;; Manual Clojure
(let [idx (reduce-kv (fn [res i v] (if (odd? v) i res)) nil data)]
  (if idx (update data idx inc) data))

;; Specter
(transform [(filterer odd?) LAST] inc data)
```

**Example 4: Map a function over a sequence without changing the type or order of the sequence**

```clojure
;; Manual Clojure
(map inc data) ;; doesn't work, becomes a lazy sequence
(into (empty data) (map inc data)) ;; doesn't work, reverses the order of lists

;; Specter
(transform ALL inc data) ;; works for all Clojure datatypes with near-optimal efficiency
```



# Latest Version

The latest release version of Specter is hosted on [Clojars](https://clojars.org):

[![Current Version](https://clojars.org/com.rpl/specter/latest-version.svg)](https://clojars.org/com.rpl/specter)

# Learn Specter

- Introductory blog post: [Clojure's missing piece](http://nathanmarz.com/blog/clojures-missing-piece.html)
- Presentation about Specter: [Specter: Powerful and Simple Data Structure Manipulation](https://www.youtube.com/watch?v=VTCy_DkAJGk)
  - Note that this presentation was given before Specter's inline compilation/caching system was developed. You no longer need to do anything special to get near-optimal performance.
- Screencast on Specter: [Understanding Specter](https://www.youtube.com/watch?v=rh5J4vacG98)
- List of navigators with examples: [This wiki page](https://github.com/redplanetlabs/specter/wiki/List-of-Navigators) provides a more comprehensive overview than the API docs about the behavior of specific navigators and includes many examples.
- Core operations and defining new navigators: [This wiki page](https://github.com/redplanetlabs/specter/wiki/List-of-Macros) provides a more comprehensive overview than the API docs of the core select/transform/etc. operations and the operations for defining new navigators.
- [This wiki page](https://github.com/redplanetlabs/specter/wiki/Using-Specter-Recursively) explains how to do precise and efficient recursive navigation with Specter.
- [This wiki page](https://github.com/redplanetlabs/specter/wiki/Using-Specter-With-Zippers) provides a comprehensive overview of how to use Specter's zipper navigators. Zippers are a much slower navigation method but can perform certain tasks that are not possible with Specter's regular navigators. Note that zippers are rarely needed.
- [Cheat Sheet](https://github.com/redplanetlabs/specter/wiki/Cheat-Sheet)
- [API docs](http://redplanetlabs.github.io/specter/)
- Performance guide: [This post](https://github.com/redplanetlabs/specter/wiki/Specter's-inline-caching-implementation) provides an overview of how Specter achieves its performance.

Specter's API is contained in these files:

- [specter.cljc](https://github.com/redplanetlabs/specter/blob/master/src/clj/com/rpl/specter.cljc): This contains the built-in navigators and the definition of the core operations.
- [transients.cljc](https://github.com/redplanetlabs/specter/blob/master/src/clj/com/rpl/specter/transients.cljc): This contains navigators for transient collections.
- [zipper.cljc](https://github.com/redplanetlabs/specter/blob/master/src/clj/com/rpl/specter/zipper.cljc): This integrates zipper-based navigation into Specter.

# Questions?

You can ask questions about Specter by [opening an issue](https://github.com/redplanetlabs/specter/issues?utf8=%E2%9C%93&q=is%3Aissue+label%3Aquestion+) on Github.

You can also find help in the #specter channel on [Clojurians](http://clojurians.net/).

# Examples

Increment all the values in maps of maps:
```clojure
user> (use 'com.rpl.specter)
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

Remove nils from a nested sequence:

```clojure
user> (setval [:a ALL nil?] NONE {:a [1 2 nil 3 nil]})
{:a [1 2 3]}
```

Remove key/value pair from nested map:

```clojure
user> (setval [:a :b :c] NONE {:a {:b {:c 1}}})
{:a {:b {}}}
```

Remove key/value pair from nested map, removing maps that become empty along the way:

```clojure
user> (setval [:a (compact :b :c)] NONE {:a {:b {:c 1}}})
{}
```

Increment all the odd numbers between indices 1 (inclusive) and 4 (exclusive):

```clojure
user> (transform [(srange 1 4) ALL odd?] inc [0 1 2 3 4 5 6 7])
[0 2 2 4 4 5 6 7]
```

Replace the subsequence from indices 2 to 4 with [:a :b :c :d :e]:

```clojure
user> (setval (srange 2 4) [:a :b :c :d :e] [0 1 2 3 4 5 6 7 8 9])
[0 1 :a :b :c :d :e 4 5 6 7 8 9]
```

Concatenate the sequence [:a :b] to every nested sequence of a sequence:

```clojure
user> (setval [ALL END] [:a :b] [[1] '(1 2) [:c]])
[[1 :a :b] (1 2 :a :b) [:c :a :b]]
```

Get all the numbers out of a data structure, no matter how they're nested:

```clojure
user> (select (walker number?)
              {2 [1 2 [6 7]] :a 4 :c {:a 1 :d [2 nil]}})
[2 1 2 1 2 6 7 4]
```

Navigate with string keys:

```clojure
user> (select ["a" "b"]
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

Append [:c :d] to every subsequence that has at least two even numbers:
```clojure
user> (setval [ALL
               (selected? (filterer even?) (view count) (pred>= 2))
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

The next examples demonstrate recursive navigation. Here's one way to double all the even numbers in a tree:

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
(def TreeValues
  (recursive-path [] p
    (if-path vector?
      [ALL p]
      STAY
      )))


(transform (subselect TreeValues even?)
  reverse
  [1 2 [3 [[4]] 5] [6 [7 8] 9 [[10]]]]
  )
;; => [1 10 [3 [[8]] 5] [6 [7 4] 9 [[2]]]]
```

# ClojureScript

Specter supports ClojureScript! However, some of the differences between Clojure and ClojureScript affect how you use Specter in ClojureScript, in particular with the namespace declarations. In Clojure, you might `(use 'com.rpl.specter)` or say `(:require [com.rpl.specter :refer :all])` in your namespace declaration. But in ClojureScript, these options [aren't allowed](https://groups.google.com/d/msg/clojurescript/SzYK08Oduxo/MxLUjg50gQwJ). Instead, consider using one of these options:

```clojure
(:require [com.rpl.specter :as s])
(:require [com.rpl.specter :as s :refer-macros [select transform]]) ;; add in the Specter macros that you need
```

# Future work
- Integrate Specter with other kinds of data structures, such as graphs. Desired navigations include: reduction in topological order, navigate to outgoing/incoming nodes, to a subgraph (with metadata indicating how to attach external edges on transformation), to node attributes, to node values, to specific nodes.

# License

Copyright 2015-2019 Red Planet Labs, Inc. Specter is licensed under Apache License v2.0.
