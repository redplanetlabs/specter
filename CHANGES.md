## 0.12.0 (unreleased)

* BREAKING CHANGE: Changed semantics of `Navigator` protocol `select*` in order to enable very large performance improvements to `select`, `select-one`, `select-first`, and `select-one!`. Custom navigators will need to be updated to comform to the new required semantics. Codebases that do not use custom navigators do not require any changes. See the docstring on the protocol for the details. 
* Added `select-any` operation which selects a single element navigated to by the path. Which element returned is undefined. If no elements are navigated to, returns `com.rpl.specter/NONE`. This is the fastest selection operation.
* Added `selected-any?` operation that returns true if any element is navigated to.
* Huge performance improvements to `select`, `select-one`, `select-first`, and `select-one!`

## 0.11.1 (unreleased)
* More efficient inline caching for Clojure version, benchmarks show inline caching within 5% of manually precompiled code for all cases
* Huge performance improvement for ALL transform on maps and vectors
* Significant performance improvements for FIRST/LAST for vectors
* Huge performance improvements for `if-path`, `cond-path`, `selected?`, and `not-selected?`, especially for condition path containing only static functions
* Huge performance improvement for `END` on vectors
* Added specialized MAP-VALS navigator that is twice as fast as using [ALL LAST]
* Eliminated compiler warnings for ClojureScript version
* Dropped support for Clojurescript below v1.7.10
* Added :notpath metadata to signify pathedfn arguments that should be treated as regular arguments during inline factoring. If one of these arguments is not a static var reference or non-collection value, the path will not factor. 
* Bug fix: `transformed` transform-fn no longer factors into `pred` when an anonymous function during inline factoring
* Bug fix: Fixed nil->val to not replace the val on `false`
* Bug fix: Eliminate reflection when using primitive paramaters in an inline cached path

## 0.11.0
* New `path` macro does intelligent inline caching of the provided path. The path is factored into a static portion and into params which may change on each usage of the path (e.g. local parameters). The static part is factored and compiled on the first run-through, and then re-used for all subsequent invocations. As an example, `[ALL (keypath k)]` is factored into `[ALL keypath]`, which is compiled and cached, and `[k]`, which is provided on each execution. If it is not possible to precompile the path (e.g. [ALL some-local-variable]), nothing is cached and the path will be compiled on each run-through.
* BREAKING CHANGE: all `select/transform/setval/replace-in` functions changed to macros and moved to com.rpl.specter.macros namespace. The new macros now automatically wrap the provided path in `path` to enable inline caching. Expect up to a 100x performance improvement without using explicit precompilation, and to be within 2% to 15% of the performance of explicitly precompiled usage.
* Added `select*/transform*/setval*/replace-in*/etc.` functions that have the same functionality as the old `select/transform/setval/replace-in` functions.
* Added `must-cache-paths!` function to throw an error if it is not possible to factor a path into a static portion and dynamic parameters.
* BREAKING CHANGE: `defpath` renamed to `defnav`
* BREAKING CHANGE: `path` renamed to `nav`
* BREAKING CHANGE: `fixed-pathed-path` and `variable-pathed-path` renamed to `fixed-pathed-nav` and `variabled-pathed-nav`
* Added `must` navigator to navigate to a key if and only if it exists in the structure
* Added `continuous-subseqs` navigator
* Added `ATOM` navigator (thanks @rakeshp)
* Added "navigator constructors" that can be defined via `defnavconstructor`. These allow defining a flexible function to parameterize a defnav, and the function integrates with inline caching for high performance.


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
