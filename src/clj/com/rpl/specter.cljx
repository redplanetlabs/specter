(ns com.rpl.specter
  #+cljs (:require-macros
            [com.rpl.specter.macros
              :refer
              [pathed-collector
               variable-pathed-nav
               fixed-pathed-nav
               defcollector
               defnav
               defpathedfn
               richnav
               defnavconstructor
              ]]
            [com.rpl.specter.util-macros :refer
              [doseqres]]
            )
  (:use [com.rpl.specter.protocols :only [Navigator]]
    #+clj [com.rpl.specter.macros :only
            [pathed-collector
             variable-pathed-nav
             fixed-pathed-nav
             defcollector
             defnav
             defpathedfn
             richnav
             defnavconstructor]]
    #+clj [com.rpl.specter.util-macros :only [doseqres]]
    )
  (:require [com.rpl.specter.impl :as i]
            [clojure.set :as set])
  )

(defn comp-paths
  "Returns a compiled version of the given path for use with
   compiled-{select/transform/setval/etc.} functions. This can compile navigators
   (defined with `defnav`) without their parameters, and the resulting compiled
   path will require parameters for all such navigators in the order in which
   they were declared."
  [& apath]
  (i/comp-paths* (vec apath)))

(def ^{:doc "Mandate that operations that do inline path factoring and compilation
             (select/transform/setval/replace-in/path/etc.) must succeed in 
             factoring the path into static and dynamic portions. If not, an
             error will be thrown and the reasons for not being able to factor
             will be printed. Defaults to false, and `(must-cache-paths! false)`
             can be used to turn this feature off.

             Reasons why it may not be able to factor a path include using
             a local symbol, special form, or regular function invocation
             where a navigator is expected."}
  must-cache-paths! i/must-cache-paths!)

;; Selection functions

(def ^{:doc "Version of select that takes in a path precompiled with comp-paths"}
  compiled-select i/compiled-select*)

(defn select*
  "Navigates to and returns a sequence of all the elements specified by the path."
  [path structure]
  (compiled-select (i/comp-paths* path)
                   structure))

(def ^{:doc "Version of select-one that takes in a path precompiled with comp-paths"}
  compiled-select-one i/compiled-select-one*)

(defn select-one*
  "Like select, but returns either one element or nil. Throws exception if multiple elements found"
  [path structure]
  (compiled-select-one (i/comp-paths* path) structure))

(def ^{:doc "Version of select-one! that takes in a path precompiled with comp-paths"}
  compiled-select-one! i/compiled-select-one!*)

(defn select-one!*
  "Returns exactly one element, throws exception if zero or multiple elements found"
  [path structure]
  (compiled-select-one! (i/comp-paths* path) structure))

(def ^{:doc "Version of select-first that takes in a path precompiled with comp-paths"}
  compiled-select-first i/compiled-select-first*)


(defn select-first*
  "Returns first element found."
  [path structure]
  (compiled-select-first (i/comp-paths* path) structure))

(def ^{:doc "Version of select-any that takes in a path precompiled with comp-paths"}
  compiled-select-any i/compiled-select-any*)

(def ^{:doc "Global value used to indicate no elements selected during
             [[select-any]]."}
  NONE i/NONE)

(defn select-any*
  "Returns any element found or [[NONE]] if nothing selected. This is the most
   efficient of the various selection operations."
  [path structure]
  (compiled-select-any (i/comp-paths* path) structure))

(def ^{:doc "Version of selected-any? that takes in a path precompiled with comp-paths"}
  compiled-selected-any? i/compiled-selected-any?*)

(defn selected-any?*
  "Returns true if any element was selected, false otherwise."
  [path structure]
  (compiled-selected-any? (i/comp-paths* path) structure))

;; Reducible traverse functions

(def ^{:doc "Version of traverse that takes in a path precompiled with comp-paths"}
  compiled-traverse i/do-compiled-traverse)

(defn traverse*
  "Return a reducible object that traverses over `structure` to every element
   specified by the path"
  [apath structure]
  (compiled-traverse (i/comp-paths* apath) structure))

;; Transformation functions

(def ^{:doc "Version of transform that takes in a path precompiled with comp-paths"}
  compiled-transform i/compiled-transform*)

(defn transform*
  "Navigates to each value specified by the path and replaces it by the result of running
  the transform-fn on it"
  [path transform-fn structure]
  (compiled-transform (i/comp-paths* path) transform-fn structure))

(def ^{:doc "Version of `multi-transform` that takes in a path precompiled with `comp-paths`"}
  compiled-multi-transform i/compiled-multi-transform*)


(defn multi-transform*
  "Just like `transform` but expects transform functions to be specified
   inline in the path using `terminal`. Error is thrown if navigation finishes
   at a non-`terminal` navigator."
  [path structure]
  (compiled-multi-transform (i/comp-paths* path) structure))


(def ^{:doc "Version of setval that takes in a path precompiled with comp-paths"}
  compiled-setval i/compiled-setval*)

(defn setval*
  "Navigates to each value specified by the path and replaces it by val"
  [path val structure]
  (compiled-setval (i/comp-paths* path) val structure))

(def ^{:doc "Version of replace-in that takes in a path precompiled with comp-paths"}
  compiled-replace-in i/compiled-replace-in*)

(defn replace-in*
  "Similar to transform, except returns a pair of [transformed-structure sequence-of-user-ret].
   The transform-fn in this case is expected to return [ret user-ret]. ret is
   what's used to transform the data structure, while user-ret will be added to the user-ret sequence
   in the final return. replace-in is useful for situations where you need to know the specific values
   of what was transformed in the data structure."
  [path transform-fn structure & {:keys [merge-fn] :or {merge-fn concat}}]
  (compiled-replace-in (i/comp-paths* path) transform-fn structure :merge-fn merge-fn))

;; Helpers for defining selectors and collectors with late-bound params

(def ^{:doc "Takes a compiled path that needs late-bound params and supplies it with
             an array of params and a position in the array from which to begin reading
             params. The return value is an executable selector."}
  bind-params* i/bind-params*)

(defn params-reset [params-path]
  ;; TODO: error if not paramsneededpath
  (let [s (i/params-needed-selector params-path)
        t (i/params-needed-transformer params-path)
        needed (i/num-needed-params params-path)]
    (i/->ParamsNeededPath
      (i/->TransformFunctions
        i/RichPathExecutor
        (fn [params params-idx vals structure next-fn]
          (s params (- params-idx needed) vals structure next-fn)
          )
        (fn [params params-idx vals structure next-fn]
          (t params (- params-idx needed) vals structure next-fn)
          ))
      0)))

;; Built-in pathing and context operations

(defnav
  ^{:doc "Stops navigation at this point. For selection returns nothing and for 
          transformation returns the structure unchanged"}
  STOP
  []
  (select* [this structure next-fn]
    NONE )
  (transform* [this structure next-fn]
    structure
    ))

(defnav
  ^{:doc "Stays navigated at the current point. Essentially a no-op navigator."}
  STAY
  []
  (select* [this structure next-fn]
    (next-fn structure))
  (transform* [this structure next-fn]
    (next-fn structure)))


(def
  ^{:doc "For usage with `multi-transform`, defines an endpoint in the navigation
          that will have the parameterized transform function run. The transform
          function works just like it does in `transform`, with collected values
          given as the first arguments"}
  terminal
  (richnav 1
    (select* [params params-idx vals structure next-fn]
      (i/throw-illegal "'terminal' should only be used in multi-transform"))
    (transform* [params params-idx vals structure next-fn]
      (i/terminal* params params-idx vals structure)
      )))

(defnavconstructor terminal-val
  "Like `terminal` but specifies a val to set at the location regardless of
   the collected values or the value at the location."
  [p terminal]
  [v]
  (p (i/fast-constantly v)))

(def
  ^{:doc "Navigate to every element of the collection. For maps navigates to
          a vector of `[key value]`."}
  ALL
  (comp-paths (i/->AllNavigator)))

(defnav
  ^{:doc "Navigate to each value of the map. This is more efficient than 
          navigating via [ALL LAST]"}
  MAP-VALS
  []
  (select* [this structure next-fn]
    (doseqres NONE [v (vals structure)]
      (next-fn v)
      ))
  (transform* [this structure next-fn]
    (i/map-vals-transform structure next-fn)
    ))


(def VAL (i/->ValCollect))

(def
  ^{:doc "Navigate to the last element of the collection. If the collection is
          empty navigation is stopped at this point."}
  LAST
  (comp-paths (i/->PosNavigator i/get-last i/update-last)))

(def
  ^{:doc "Navigate to the first element of the collection. If the collection is
          empty navigation is stopped at this point."}
  FIRST
  (comp-paths (i/->PosNavigator i/get-first i/update-first)))

(defnav
  ^{:doc "Uses start-fn and end-fn to determine the bounds of the subsequence
          to select when navigating. Each function takes in the structure as input."}
  srange-dynamic
  [start-fn end-fn]
  (select* [this structure next-fn]
    (i/srange-select structure (start-fn structure) (end-fn structure) next-fn))
  (transform* [this structure next-fn]
    (i/srange-transform structure (start-fn structure) (end-fn structure) next-fn)
    ))

(defnav
  ^{:doc "Navigates to the subsequence bound by the indexes start (inclusive)
          and end (exclusive)"}
  srange
  [start end]
  (select* [this structure next-fn]
    (i/srange-select structure start end next-fn))
  (transform* [this structure next-fn]
    (i/srange-transform structure start end next-fn)
    ))

(defnav
  ^{:doc "Navigates to every continuous subsequence of elements matching `pred`"}
  continuous-subseqs
  [pred]
  (select* [this structure next-fn]
    (doseqres NONE [[s e] (i/matching-ranges structure pred)]
      (i/srange-select structure s e next-fn)
      ))
  (transform* [this structure next-fn]
    (reduce
      (fn [structure [s e]]
        (i/srange-transform structure s e next-fn))
      structure
      (reverse (i/matching-ranges structure pred))
      )))

(defnav
  ^{:doc "Navigate to the empty subsequence before the first element of the collection."}
  BEGINNING
  []
  (select* [this structure next-fn]
    (next-fn []))
  (transform* [this structure next-fn]
    (let [to-prepend (next-fn [])]
      (i/prepend-all structure to-prepend)
      )))

(defnav
 ^{:doc "Navigate to the empty subsequence after the last element of the collection."}
  END
  []
  (select* [this structure next-fn]
    (next-fn []))
  (transform* [this structure next-fn]
    (let [to-append (next-fn [])]
      (i/append-all structure to-append)
      )))
  
(defnav
  ^{:doc "Navigates to the specified subset (by taking an intersection).
          In a transform, that subset in the original set is changed to the
          new value of the subset."}
  subset
  [aset]
  (select* [this structure next-fn]
    (next-fn (set/intersection structure aset)))
  (transform* [this structure next-fn]
    (let [subset (set/intersection structure aset)
          newset (next-fn subset)]
      (-> structure
          (set/difference subset)
          (set/union newset))
          )))

(defnav
  ^{:doc "Navigates to the specified submap (using select-keys).
          In a transform, that submap in the original map is changed to the new
          value of the submap."}
  submap
  [m-keys]
  (select* [this structure next-fn]
    (next-fn (select-keys structure m-keys)))

  (transform* [this structure next-fn]
    (let [submap (select-keys structure m-keys)
          newmap (next-fn submap)]
      (merge (reduce dissoc structure m-keys)
             newmap))))

(defnav
  ^{:doc "Using clojure.walk, navigate the data structure until reaching
          a value for which `afn` returns truthy."}
  walker
  [afn]
  (select* [this structure next-fn]
    (i/walk-select afn next-fn structure))
  (transform* [this structure next-fn]
    (i/walk-until afn next-fn structure)))

(defnav
  ^{:doc "Like `walker` but maintains metadata of any forms traversed."}
  codewalker
  [afn]
  (select* [this structure next-fn]
    (i/walk-select afn next-fn structure))
  (transform* [this structure next-fn]
    (i/codewalk-until afn next-fn structure)))

(defpathedfn subselect
  "Navigates to a sequence that contains the results of (select ...),
  but is a view to the original structure that can be transformed.

  Requires that the input navigators will walk the structure's
  children in the same order when executed on \"select\" and then
  \"transform\"."
  [& path]
  (fixed-pathed-nav [late path]
    (select* [this structure next-fn]
             (next-fn (compiled-select late structure)))
    (transform* [this structure next-fn]
      (let [select-result (compiled-select late structure)
            transformed (next-fn select-result)
            values-to-insert (i/mutable-cell transformed)]
        (compiled-transform late
                            (fn [_] (let [next-val (first (i/get-cell values-to-insert))]
                                      (i/update-cell! values-to-insert rest)
                                      next-val))
                            structure)))))

(defnav
  ^{:doc "Navigates to the specified key, navigating to nil if it does not exist."}
  keypath
  [key]
  (select* [this structure next-fn]
    (next-fn (get structure key)))
  (transform* [this structure next-fn]
    (assoc structure key (next-fn (get structure key)))
    ))

(defnav
  ^{:doc "Navigates to the key only if it exists in the map."}
  must
  [k]
  (select* [this structure next-fn]
    (if (contains? structure k)
      (next-fn (get structure k))
      NONE))
  (transform* [this structure next-fn]
   (if (contains? structure k)
     (assoc structure k (next-fn (get structure k)))
     structure
     )))

(defnav
  ^{:doc "Navigates to result of running `afn` on the currently navigated value."}
  view
  [afn]
  (select* [this structure next-fn]
    (next-fn (afn structure)))
  (transform* [this structure next-fn]
    (next-fn (afn structure))
    ))

(defnav
  ^{:doc "Navigate to the result of running `parse-fn` on the value. For 
          transforms, the transformed value then has `unparse-fn` run on 
          it to get the final value at this point."}
  parser
  [parse-fn unparse-fn]
  (select* [this structure next-fn]
    (next-fn (parse-fn structure)))
  (transform* [this structure next-fn]
    (unparse-fn (next-fn (parse-fn structure)))
    ))

(defnav
  ^{:doc "Navigates to atom value."}
  ATOM
  []
  (select* [this structure next-fn]
    (next-fn @structure))
  (transform* [this structure next-fn]
    (do
      (swap! structure next-fn)
      structure)))

(defpathedfn selected?
  "Filters the current value based on whether a path finds anything.
  e.g. (selected? :vals ALL even?) keeps the current element only if an
  even number exists for the :vals key.

  The input path may be parameterized, in which case the result of selected?
  will be parameterized in the order of which the parameterized navigators
  were declared."
  [& path]
  (if-let [afn (i/extract-basic-filter-fn path)]
    afn
    (fixed-pathed-nav [late path]
      (select* [this structure next-fn]
        (i/filter-select
          #(i/selected?* late %)
          structure
          next-fn))
      (transform* [this structure next-fn]
        (i/filter-transform
          #(i/selected?* late %)
          structure
          next-fn)))))

(defpathedfn not-selected? [& path]
  (if-let [afn (i/extract-basic-filter-fn path)]
    (fn [s] (not (afn s)))
    (fixed-pathed-nav [late path]
      (select* [this structure next-fn]
        (i/filter-select
          #(i/not-selected?* late %)
          structure
          next-fn))
      (transform* [this structure next-fn]
        (i/filter-transform
          #(i/not-selected?* late %)
          structure
          next-fn)))))

(defpathedfn filterer
  "Navigates to a view of the current sequence that only contains elements that
  match the given path. An element matches the selector path if calling select
  on that element with the path yields anything other than an empty sequence.

   The input path may be parameterized, in which case the result of filterer
   will be parameterized in the order of which the parameterized selectors
   were declared."
  [& path]
  (subselect ALL (selected? path)))

(defpathedfn transformed
  "Navigates to a view of the current value by transforming it with the
   specified path and update-fn.

   The input path may be parameterized, in which case the result of transformed
   will be parameterized in the order of which the parameterized navigators
   were declared."
  [path ^:notpath update-fn]
  (fixed-pathed-nav [late path]
    (select* [this structure next-fn]
      (next-fn (compiled-transform late update-fn structure)))
    (transform* [this structure next-fn]
      (next-fn (compiled-transform late update-fn structure)))))

(extend-type #+clj clojure.lang.Keyword #+cljs cljs.core/Keyword
  Navigator
  (select* [kw structure next-fn]
    (next-fn (get structure kw)))
  (transform* [kw structure next-fn]
    (assoc structure kw (next-fn (get structure kw)))
    ))

(extend-type #+clj clojure.lang.AFn #+cljs function
  Navigator
  (select* [afn structure next-fn]
    (i/filter-select afn structure next-fn))
  (transform* [afn structure next-fn]
    (i/filter-transform afn structure next-fn)))

(extend-type #+clj clojure.lang.PersistentHashSet #+cljs cljs.core/PersistentHashSet
  Navigator
  (select* [aset structure next-fn]
    (i/filter-select aset structure next-fn))
  (transform* [aset structure next-fn]
    (i/filter-transform aset structure next-fn)))

(def
  ^{:doc "Keeps the element only if it matches the supplied predicate. This is the
          late-bound parameterized version of using a function directly in a path."}
  pred
  i/pred*
  )

(defnav
  ^{:doc "Navigates to the provided val if the structure is nil. Otherwise it stays
          navigated at the structure."}
  nil->val
  [v]
  (select* [this structure next-fn]
    (next-fn (if (nil? structure) v structure)))
  (transform* [this structure next-fn]
    (next-fn (if (nil? structure) v structure))))

(def
  ^{:doc "Navigates to #{} if the value is nil. Otherwise it stays
          navigated at the current value."}
  NIL->SET
  (nil->val #{}))

(def
  ^{:doc "Navigates to '() if the value is nil. Otherwise it stays
          navigated at the current value."}
  NIL->LIST
  (nil->val '()))

(def
  ^{:doc "Navigates to [] if the value is nil. Otherwise it stays
          navigated at the current value."}
  NIL->VECTOR
  (nil->val []))

(defnav ^{:doc "Navigates to the metadata of the structure, or nil if
  the structure has no metadata or may not contain metadata."}
  META
  []
  (select* [this structure next-fn]
    (next-fn (meta structure)))
  (transform* [this structure next-fn]
    (with-meta structure (next-fn (meta structure)))))

(defpathedfn
  ^{:doc "Adds the result of running select with the given path on the
          current value to the collected vals."}
  collect
  [& path]
  (pathed-collector [late path]
    (collect-val [this structure]
      (compiled-select late structure)
      )))

(defpathedfn
  ^{:doc "Adds the result of running select-one with the given path on the
          current value to the collected vals."}
  collect-one
  [& path]
  (pathed-collector [late path]
    (collect-val [this structure]
      (compiled-select-one late structure)
      )))

(defcollector
  ^{:doc
    "Adds an external value to the collected vals. Useful when additional arguments
     are required to the transform function that would otherwise require partial
     application or a wrapper function.

     e.g., incrementing val at path [:a :b] by 3:
     (transform [:a :b (putval 3)] + some-map)"}
  putval
  [val]
  (collect-val [this structure]
    val ))

(def
  ^{:doc "Drops all collected values for subsequent navigation."}
  DISPENSE i/DISPENSE*)


(defpathedfn if-path
  "Like cond-path, but with if semantics."
  ([cond-p then-path]
    (if-path cond-p then-path STOP))
  ([cond-p then-path else-path]
    (let [then-comp (i/comp-paths* then-path)
          else-comp (i/comp-paths* else-path)
          then-needed (i/num-needed-params then-comp)
          else-needed (i/num-needed-params else-comp)
          [then-s then-t] (i/extract-rich-tfns then-comp)
          [else-s else-t] (i/extract-rich-tfns else-comp)]
      (if-let [afn (i/extract-basic-filter-fn cond-p)]
        (richnav (+ then-needed else-needed)
          (select* [params params-idx vals structure next-fn]
            (i/if-select
              params
              params-idx
              vals
              structure
              next-fn
              afn
              then-s
              then-needed
              else-s
              ))
          (transform* [params params-idx vals structure next-fn]
            (i/if-transform
              params
              params-idx
              vals
              structure
              next-fn
              afn
              then-t
              then-needed
              else-t
              ))))
        (let [cond-comp (i/comp-paths* cond-p)            
              cond-needed (i/num-needed-params cond-comp)]
          (richnav (+ then-needed else-needed cond-needed)
            (select* [params params-idx vals structure next-fn]
              (let [late-cond (i/parameterize-path cond-comp params params-idx)]
                (i/if-select
                  params
                  (+ params-idx cond-needed)
                  vals
                  structure
                  next-fn
                  #(i/selected?* late-cond %)
                  then-s
                  then-needed
                  else-s
                  )))
            (transform* [params params-idx vals structure next-fn]
              (let [late-cond (i/parameterize-path cond-comp params params-idx)]
                (i/if-transform
                  params
                  (+ params-idx cond-needed)
                  vals
                  structure
                  next-fn
                  #(i/selected?* late-cond %)
                  then-t
                  then-needed
                  else-t
                  ))))))))

(defpathedfn cond-path
  "Takes in alternating cond-path path cond-path path...
   Tests the structure if selecting with cond-path returns anything.
   If so, it uses the following path for this portion of the navigation.
   Otherwise, it tries the next cond-path. If nothing matches, then the structure
   is not selected.

   The input paths may be parameterized, in which case the result of cond-path
   will be parameterized in the order of which the parameterized navigators
   were declared."
  [& conds]
  (let [pairs (reverse (partition 2 conds))]
    (reduce
      (fn [p [tester apath]]
        (if-path tester apath p))
      STOP
      pairs
      )))

(defpathedfn multi-path
  "A path that branches on multiple paths. For updates,
   applies updates to the paths in order."
  [& paths]
  (let [paths-comp (mapv i/comp-paths* (vec paths))
        all-needed (mapv i/num-needed-params paths-comp)
        idx-deltas (vec (cons 0 (reductions + all-needed)))
        extracted (mapv i/extract-rich-tfns paths-comp)
        sel-info (mapv vector (mapv first extracted) idx-deltas)
        tran-info (mapv vector (mapv second extracted) idx-deltas)]
    (richnav (reduce + 0 all-needed)
      (select* [params params-idx vals structure next-fn]
        (doseqres NONE [[s delta] sel-info]
          (s params (+ params-idx delta) vals structure next-fn)
          ))
      (transform* [params params-idx vals structure next-fn]
        (reduce
          (fn [structure [t delta]]
            (t params (+ params-idx delta) vals structure next-fn)
            )
          structure
          tran-info
          )))))

(defpathedfn stay-then-continue
  "Navigates to the current element and then navigates via the provided path.
   This can be used to implement pre-order traversal."
  [& path]
  (multi-path STAY path))

(defpathedfn continue-then-stay
  "Navigates to the provided path and then to the current element. This can be used
   to implement post-order traversal."
  [& path]
  (multi-path path STAY))
