## 0.10.1 (unreleased)
* Added `must` navigator

## 0.10.0
* Make codebase bootstrap cljs compatible
* Remove usage of reducers in cljs version in favor of transducers (thanks @StephenRudolph)
* ALL now maintains type of queues (thanks @StephenRudolph)
* Added `parser` path (thanks @thomasathorne)
* Added `submap` path (thanks @bfabry)
* Added `subselect` path (thanks @aengelberg)
* Fix filterer to maintain the type of the input sequence in transforms
* Integrated zipper navigation into com.rpl.specter.zipper namespace

## 0.9.3
* Change clojure/clojurescript to provided dependencies
* ALL on maps auto-coerces MapEntry to vector, enabling smoother transformation of map keys
* declarepath can now be parameterized
* Added params-reset which calls its path with the params index walked back by the number of params needed by its path. This enables recursive parameterized paths
* Added convenience syntax for defprotocolpath with no params, e.g. (defprotocolpath foo)
* Rename VOID to STOP

## 0.9.2
* Added VOID selector which navigates nowhere
* Better syntax checking for defpath
* Fixed bug in protocol paths (#48)
* Protocol paths now error when extension has invalid number of needed parameters
* Fix replace-in to work with value collection
* Added STAY selector
* Added stay-then-continue and continue-then-stay selectors which enable pre-order/post-order traversals
* Added declarepath and providepath, which enable arbitrary recursive or mutually recursive paths
* Renamed paramspath to path

## 0.9.1
* Fixed reflection in protocol path code
* Optimized late-bound parameterization for JVM implementation by directly creating the object array rather than use object-array 
* Incorrectly specified function names in defpath will now throw error

## 0.9.0
* Fixed bug where comp-paths wouldn't work on lazy seqs in cljs
* Renamed defparamspath and defparamscollector to defpath and defcollector
* For Clojure version only, implemented protocol paths (see #38)

## 0.8.0
* Now compatible with Clojure 1.6.0 and 1.5.1 by switching build to cljx (thanks @MerelyAPseudonym)
* Added subset selector (like srange but for sets)
* Added nil->val, NIL->SET, NIL->LIST, and NIL->VECTOR selectors to make it easier to manipulate maps (e.g. (setval [:akey NIL->VECTOR END] [:a :b] amap) to append that vector into that value for the map, even if nothing was at that value at the start)

## 0.7.1
* view can now be late-bound parameterized
* Added a late-bound parameterized version of using a function as a selector called "pred"
* Added paramsfn helper macro for defining filter functions that take late-bound parameters
* walker and codewalker can now be late-bound parameterized

## 0.7.0
* Added late-bound parameterization feature: allows selectors that require params to be precompiled without the parameters, and the parameters are supplied later in bulk. This effectively enables Specter to be used in any situation with very high performance.
* Converted Specter built-in selectors to use late-bound parameterization when appropriate
* ALL, FIRST, and LAST are now precompiled

## 0.6.2
* Added not-selected? selector
* Added transformed selector
* Sped up CLJS implementation for comp-paths by replacing obj-extends? call with satisfies? 
* Fixed CLJS implementation to extend core types appropriately
* Used not-native hint to enable direct method invocation to speed up CLJS implementation


## 0.6.1
* Huge speedup to ClojureScript implementation by optimizing field access

## 0.6.0
* Added ClojureScript compatibility

## 0.5.7
* Fix bug in select-one! which wouldn't allow nil result

## 0.5.6
* Add multi-path implementation
* change FIRST/LAST to select nothing on an empty sequence
* Allow sets to be used directly as selectors (acts as filter)

## 0.5.5
* Change filterer to accept a selector (that acts like selected? to determine whether or not to select value)

## 0.5.4
* Change cond-path and if-path to take in a selector for conditionals (same idea as selected?)

## 0.5.3
* Added cond-path and if-path selectors for choosing paths depending on value of structure at that location

## 0.5.2
* Fix error for selectors with one element defined using comp-paths, e.g. [:a (comp-paths :b)]

## 0.5.1
* Added putval for adding external values to collected values list
* nil is now interpreted as identity selector
* empty selector is now interpreted as identity selector instead of producing error
