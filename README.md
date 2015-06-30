# Specter

Most of Clojure programming involves creating, manipulating, and transforming immutable values. However, as soon as your values become more complicated than a simple map or list – like a list of maps of maps – transforming these data structures becomes extremely cumbersome. 

Specter is a library for querying and updating nested data structures. One way to think of it is "get-in" and "assoc-in" on steroids, though Specter works on any data structure, not just maps. It is similar to the concept of a "lens" in functional programming, though it has some important extensions.

Specter is fully extensible. At its core, its just a protocol for how to navigate within a data structure. By extending this protocol, you can use Specter to navigate any data structure or object you have.

Specter is a very high performance library. For example: the Specter equivalent to get-in runs 30% faster than get-in, and the Specter equivalent to update-in runs 5x faster than update-in. In each case the Specter code is equally as convenient. 

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

If you had a map with a sequence as the value for the :a key, here's how to get all odd numbers in that sequence:

```clojure
user> (use 'com.rpl.specter)
nil
user> (select [:a ALL odd?]
              {:a [1 2 3 5] :b :c})
[1 3 5]
```

Another function called `transform` is used to perform a transformation on a data structure. In addition to a selector, it takes in an "transform function" which specifies what to do with each element navigated to. For example, here's how to increment all the even values for :a keys in a sequence of maps:

```clojure
user> (transform [ALL :a even?]
                 inc
                 [{:a 1} {:a 2} {:a 4} {:a 3}])
[{:a 1} {:a 3} {:a 5} {:a 3}]
```

Here's another example of `transform`:

```clojure
user> (use 'com.rpl.specter)
nil
user> (transform [:a ALL odd?]
                 dec
                 {:a [1 2 3 5] :b :c})
{:b :c, :a [0 2 2 4]}
```

Specter comes with all sorts of built-in ways of navigating data structures. For example, here's how to increment the last odd number in a sequence:

```clojure
user> (transform [(filterer odd?) LAST]
                 inc
                 [2 1 3 6 9 4 8])
[2 1 3 6 10 4 8]
```

`filterer` navigates you to a view of the sequence currently being looked at. `LAST` navigates you to the last element of whatever sequence you're looking at. But of course during transforms, the transforms are performed on the original data structure. 

`srange` is a selector for looking at or replacing a subsequence of a sequence. For example, here's how to increment all the odd numbers between indexes 1 (inclusive) and 4 (exclusive):

```clojure
user> (transform [(srange 1 4) ALL odd?] inc [0 1 2 3 4 5 6 7])
[0 2 2 4 4 5 6 7]
```

`srange` can also be used to replace that subsequence entirely with a new sequence. For example, here's how to replace the subsequence from index 2 to 4 with [-1 -1 -1]:

```clojure
user> (transform (srange 2 4) (fn [_] [-1 -1 -1]) [0 1 2 3 4 5 6 7 8 9])
[0 1 -1 -1 -1 4 5 6 7 8 9]
```

The above can be written more concisely using the `setval` function, which is a wrapper around `transform`:

```clojure
user> (setval (srange 2 4) [-1 -1 -1] [0 1 2 3 4 5 6 7 8 9])
[0 1 -1 -1 -1 4 5 6 7 8 9]
```

Here's how to concatenate the sequence [:a :b] to every nested sequence of a sequence:

```clojure
user> (setval [ALL END] [:a :b] [[1] '(1 2) [:c]])
[[1 :a :b] (1 2 :a :b) [:c :a :b]]
```

`END` is a wrapper around `srange-dynamic`, which takes in functions that return the start index and end index given the structure.

`walker` is another useful selector that walks the data structure until a predicate is matched. Here's how to get all the numbers out of a map:

```clojure
user> (select (walker number?)
              {2 [1 2 [6 7]] :a 4 :c {:a 1 :d [2 nil]}})
[2 1 2 1 2 6 7 4]
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

To make your own selector, implement the `StructurePath` protocol which looks like:

```clojure
(defprotocol StructurePath
  (select* [this structure next-fn])
  (transform* [this structure next-fn])
  )
```

As an example, here is how Clojure keywords implement this protocol:

```clojure
(extend-type clojure.lang.Keyword
  StructurePath
  (select* [kw structure next-fn]
    (next-fn (get structure kw)))
  (transform* [kw structure next-fn]
    (assoc structure kw (next-fn (get structure kw)))
    ))
```

`next-fn` represents the rest of the select or transform, respectively. As you can see, this implementation perfectly captures what it means to navigate via a keyword within a data structure. In the select case, it completes the select by calling `next-fn` on the value of the keyword. In the transform case, it transforms the nested data structure using next-fn and then replaces the current value of the keyword with that transformed data structure.

Finally, you can make `select` and `transform` work much faster by precompiling your selectors using the `comp-paths` function. There's about a 3x speed difference between the following two invocations of `transform`:

```clojure
(def precompiled (comp-paths ALL :a even?))

(transform [ALL :a even?] inc structure)
(compiled-transform precompiled inc structure)
```

Depending on the details of the selector and the data being transformed, precompiling can sometimes provide more than a 10x speedup.

Some more examples:

Decrement every value in a map:
```clojure
user> (transform [ALL LAST]
                 dec
                 {:a 1 :b 3})
{:b 2 :a 0}
```

Increment the value for :a key by 10:
```clojure
user> (transform [:a (putval 10)]
                 +
                 {:a 1 :b 3})
{:b 3 :a 11}
```

Get every number divisible by 3 out of a sequence of sequences:
```clojure
user> (select [ALL ALL #(= 0 (mod % 3))]
              [[1 2 3 4] [] [5 3 2 18] [2 4 6] [12]])
[3 3 18 6 12]
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

For every map in a sequence, increment every number in :c's value if :a is even or increment :d if :a is odd:

```clojure
user> (transform [ALL (if-path [:a even?] [:c ALL] :d)]
                 inc
                 [{:a 2 :c [1 2] :d 4} {:a 4 :c [0 10 -1]} {:a -1 :c [1 1 1] :d 1}])
[{:c [2 3], :d 4, :a 2} {:c [1 11 0], :a 4} {:c [1 1 1], :d 2, :a -1}]
```

# Future work
- Make it possible to parallelize selects/transforms
- Any connection to transducers?
- Add Clojurescript compatibility

# License

Copyright 2015 Red Planet Labs, Inc. Specter is licensed under Apache License v2.0.
