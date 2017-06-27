## 1.0.3-SNAPSHOT

* Workaround for ClojureScript regression that causes warnings for record fields named "var" or other reserved names

## 1.0.2

* Added `pred=`, `pred<`, `pred>`, `pred<=`, `pred>=` for filtering using common comparisons
* Add `map-key` navigator
* Add `set-elem` navigator
* Add `ALL-WITH-META` navigator
* `walker` and `codewalker` can now be used with `NONE` to remove elements
* Improve `walker` performance by 70% by replacing clojure.walk implementation with custom recursive path
* Extend `ALL` to work on records (navigate to key/value pairs)
* Add ability to declare a function for end index of `srange-dynamic` that takes in the result of the start index fn. Use `end-fn` macro to declare this function (takes in 2 args of [collection, start-index]). Functions defined with normal mechanisms (e.g. `fn`) will still only take in the collection as an argument.
* Workaround for ClojureScript bug that emits warnings for vars named the same as a private var in cljs.core (in this case `NONE`, added as private var to cljs.core with 1.9.562)
* For ALL transforms on maps, interpret transformed key/value pair of size < 2 as removal
* Bug fix: Fix incorrect inline compilation when a dynamic function invocation is nested in a data structure within a parameter to a navigator builder

## 1.0.1

* `subselect`/`filterer` can remove entries in source by transforming to a smaller sequence
* Add `satisfies-protpath?`
* Inline cache vars are marked private so as not to interfere with tooling
* Improve performance of `ALL` transform on lists by 20%
* Bug fix: Using `pred` no longer inserts unnecessary `coerce-nav` call at callsite
* Bug fix: Dynamic navs in argument position to another nav now properly expanded and compiled
* Bug fix: Dynamic parameters nested inside data structures as arguments are now compiled correctly by inline compiler

## 1.0.0

* Transform to `com.rpl.specter/NONE` to remove elements from data structures. Works with `keypath` (for both sequences and maps), `must`, `nthpath`, `ALL`, `MAP-VALS`, `FIRST`, and `LAST`
* Add `nthpath` navigator
* Add `with-fresh-collected` higher order navigator
* Added `traverse-all` which returns a transducer that traverses over all elements matching the given path.
* `select-first` and `select-any` now avoid traversal beyond the first value matched by the path (like when using `ALL`), so they are faster now for those use cases.
* Add `MAP-KEYS` navigator that's more efficient than `[ALL FIRST]`
* Add `NAME` and `NAMESPACE` navigators
* Extend `srange`, `BEGINNING`, `END` to work on strings. Navigates to a substring.
* Extend `FIRST` and `LAST` to work on strings. Navigates to a character.
* Add `BEFORE-ELEM` and `AFTER-ELEM` for prepending or appending a single element to a sequence
* Add `NONE-ELEM` to efficiently add a single element to a set
* Improved `ALL` performance for PersistentHashSet
* Dynamic navs automatically compile sequence returns if completely static
* Eliminate reflection warnings for clj (thanks @mpenet)
* Bug fix: Collected vals now properly passed to subpaths for `if-path`, `selected?`, and `not-selected?`
* Bug fix: `LAST`, `FIRST`, `BEGINNING`, and `END` properly transform subvector types to a vector type

## 0.13.2

* Bug fix: Fix race condition relating to retrieving path from cache and AOT compilation
* Bug fix: LAST no longer converts lists to vectors
* Bug fix: Workaround issue with aot + uberjar

## 0.13.1

* Remove any? in com.rpl.specter.impl to avoid conflict with Clojure 1.9
* Enhanced dynamic navigators to continue expanding if any other dynamic navs are returned
* Added `eachnav` to turn any 1-argument navigator into a navigator that accepts any number of arguments, navigating by each argument in order
* `keypath` and `must` enhanced to take in multiple arguments for concisely specifying multiple steps
* Added `traversed`
* Bug fix: Fix regression from 0.13.0 where [ALL FIRST] on a PersistentArrayMap that created duplicate keys would create an invalid PersistentArrayMap
* Bug fix: Fix problems with multi-path and if-path in latest versions of ClojureScript
* Bug fix: Inline compiler no longer flattens and changes the type of sequential params

## 0.13.0

* BREAKING CHANGE: `com.rpl.specter.macros` namespace removed and all macros moved into core `com.rpl.specter` namespace
* BREAKING CHANGE: Core protocol `Navigator` changed to `RichNavigator` and functions now have an extra argument.
* BREAKING CHANGE: All navigators must be defined with `defnav` and its variations from `com.rpl.specter`. The core protocols may no longer be extended. Existing types can be turned into navigators with the new `IndirectNav` protocol.
* BREAKING CHANGE: Removed `fixed-pathed-nav` and `variable-pathed-nav` and replaced with much more generic `late-bound-nav`. `late-bound-nav` can have normal values be late-bound parameterized (not just paths). Use `late-path` function to indicate which parameters are paths. If all bindings given to `late-bound-nav` are static, the navigator will be resolved and cached immediately. See `transformed` and `selected?` for examples.
* BREAKING CHANGE: Paths can no longer be compiled without their parameters. Instead, use the `path` macro to handle the parameterization.
* BREAKING CHANGE: Parameterized protocol paths now work differently since paths cannot be specified without their parameters. Instead, use the parameter names from the declaration in the extension to specify where the parameters should go. For example:
```clojure
(defprotocolpath MyProtPath [a])
(extend-protocolpath MyProtPath
  clojure.lang.PersistentArrayMap
  (must a))
```
* BREAKING CHANGE: Removed `defpathedfn` and replaced with much more generic `defdynamicnav`. `defdynamicnav` works similar to a macro and takes as input the parameters seen during inline caching. Use `dynamic-param?` to distinguish which parameters are statically specified and which are dynamic. `defdynamicnav` is typically used in conjunction with `late-bound-nav` – see implementation of `selected?` for an example.
* Inline caching now works with locals, dynamic vars, and special forms used in the nav position. When resolved at runtime, those values will be coerced to a navigator if a vector or implicit nav (e.g. keyword). Can hint with ^:direct-nav metadata to remove this coercion if know for sure those values will be implementations of `RichNavigator` interface.
* Redesigned internals so navigators use interface dispatch rather than storing transform/selection functions as separate fields.
* Added `local-declarepath` to assist in making local recursive or mutually recursive paths. Use with `providepath`.
* Added `recursive-path` to assist in making recursive paths, both parameterized and unparameterized. Example:
```clojure
(let [tree-walker (recursive-path [] p (if-path vector? [ALL p] STAY))]
  (select tree-walker [1 [2 [3 4] 5] [[6]]]))
```
* Significantly improved performance of paths that use dynamic parameters.
* Inline factoring now parameterizes navigators immediately when all parameters are constants (rather than factoring it to use late-bound parameterization). This creates leaner, faster code.
* Added `IndirectNav` protocol for turning a value type into a navigator.
* Removed `must-cache-paths!`. No longer relevant since all paths can now be compiled and cached.
* Added `with-inline-debug` macro that prints information about the code being analyzed and produced by the inline compiler / cacher.
* Switched codebase from cljx to cljc
* Improved performance of ALL and MAP-VALS on PersistentArrayMap by about 2x
* `defnav` now generates helper functions for every method. For example, `keypath` now has helpers `keypath-select*` and `keypath-transform*`. These functions take parameters `[key structure next-fn]`
* Bug fix: ALL and MAP-VALS transforms on PersistentArrayMap above the threshold now output PersistentArrayMap instead of PersistentHashMap


## 0.12.0

* BREAKING CHANGE: Changed semantics of `Navigator` protocol `select*` in order to enable very large performance improvements to `select`, `select-one`, `select-first`, and `select-one!`. Custom navigators will need to be updated to conform to the new required semantics. Codebases that do not use custom navigators do not require any changes. See the docstring on the protocol for the details.
* Added `select-any` operation which selects a single element navigated to by the path. Which element returned is undefined. If no elements are navigated to, returns `com.rpl.specter/NONE`. This is the fastest selection operation.
* Added `selected-any?` operation that returns true if any element is navigated to.
* Added `traverse` operation which returns a reducible object of all the elements navigated to by the path. Very efficient.
* Added `multi-transform` operation which can be used to perform multiple transformations in a single traversal. Much more efficient than doing the
transformations with `transform` one after another when the transformations share a lot of navigation. `multi-transform` is used in conjunction with `terminal` and `terminal-val` – see the docstring for details.
* Huge performance improvements to `select`, `select-one`, `select-first`, and `select-one!`
* Huge performance improvement to `multi-path`
* Added META navigator (thanks @aengelberg)
* Added DISPENSE navigator to drop all collected values for subsequent navigation
* Added `collected?` macro to create a filter function which operates on the collected values.
* Error now thrown if a pathedfn (like filterer) is used without being parameterized
* Performance improvement for ALL and MAP-VALS on small maps for Clojure by leveraging IMapIterable interface
* Added low-level `richnav` macro for creating navigators with full flexibility
* Bug fix: multi-path and if-path now work properly with value collection
* Bug fix: END, BEGINNING, FIRST, LAST, and MAP-VALS now work properly on nil
* Bug fix: ALL and MAP-VALS now maintain the comparator of sorted maps
* Bug fix: Using value collection along with `setval` no longer throws exception
* Bug fix: Fix error when trying to use Specter along with AOT compilation

## 0.11.2
* Renamed com.rpl.specter.transient namespace to com.rpl.specter.transients to eliminate ClojureScript compiler warning about reserved keyword
* Eliminated compiler warnings for ClojureScript version

## 0.11.1
* More efficient inline caching for Clojure version, benchmarks show inline caching within 5% of manually precompiled code for all cases
* Added navigators for transients in com.rpl.specter.transient namespace (thanks @aengelberg)
* Huge performance improvement for ALL transform on maps and vectors
* Significant performance improvements for FIRST/LAST for vectors
* Huge performance improvements for `if-path`, `cond-path`, `selected?`, and `not-selected?`, especially for condition path containing only static functions
* Huge performance improvement for `END` on vectors
* Added specialized MAP-VALS navigator that is twice as fast as using [ALL LAST]
* Dropped support for Clojurescript below v1.7.10
* Added :notpath metadata to signify pathedfn arguments that should be treated as regular arguments during inline factoring. If one of these arguments is not a static var reference or non-collection value, the path will not factor.
* Bug fix: `transformed` transform-fn no longer factors into `pred` when an anonymous function during inline factoring
* Bug fix: Fixed nil->val to not replace the val on `false`
* Bug fix: Eliminate reflection when using primitive parameters in an inline cached path

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
