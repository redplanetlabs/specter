# Specter
Deep introspection and transformation of data

# About

Specter is a library for concisely querying and updating nested data structures. One way to think of it is "get-in" and "assoc-in" on steroids. It is similar to the concept of a "lens" in functional programming, though it has some important extensions. 

# How to use

;; explain basic usage
;; explain structurepath interface
;; explain how structurepath can be extended (I've done it for working with directed acyclic graphs)
	- show implementations for ALL, VAL, LAST, etc.
;; explain precompiling to make things far faster

From a sequence of maps get all the even values for :a keys:
```clojure
>>> (select [ALL :a even?] [{:a 1} {:a 2} {:a 4} {:a 3}])
[2 4]
```

In a sequence of maps increment all the even values for :a keys:
>>> (update [ALL :a even?] inc [{:a 1} {:a 2} {:a 4} {:a 3}])
[{:a 1} {:a 3} {:a 5} {:a 3}]


Increment the last odd number in a sequence:
```clojure
>>> (update [(filterer odd?) LAST] inc [2 1 6 9 4 8])
[2 1 6 10 4 8]
```

For all maps in a sequence, add the value of :b key to the value of :a key, but only if the :a value is even:
```clojure
>>> (update [ALL (val-selector-one :b) :a even?] + [{:a 1 :b 3} {:a 2 :b -10} {:a 4 :b 10} {:a 3}])
[{:b 3, :a 1} {:b -10, :a -8} {:b 10, :a 14} {:a 3}]
```

# Future work
;; parallelize the transformations
;; any connection to transducers?
