## 0.7.1 (unreleased)
* view can now be late-bound parameterized
* Added a late-bound parameterized version of using a function as a selector called "pred"

## 0.7.0
* Added late-bound parameterization feauture: allows selectors that require params to be precompiled without the parameters, and the parameters are supplied later in bulk. This effectively enables Specter to be used in any situation with very high performance.
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
