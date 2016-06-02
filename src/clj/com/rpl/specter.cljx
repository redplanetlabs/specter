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
              ]]
            )
  (:use [com.rpl.specter.protocols :only [Navigator]]
    #+clj [com.rpl.specter.macros :only
            [pathed-collector
             variable-pathed-nav
             fixed-pathed-nav
             defcollector
             defnav
             defpathedfn]]
    )
  (:require [com.rpl.specter.impl :as i]
            [clojure.set :as set])
  )

(defn comp-paths [& paths]
  (i/comp-paths* (vec paths)))

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

(def ^{:doc "Version of select that takes in a path pre-compiled with comp-paths"}
  compiled-select i/compiled-select*)

(defn select*
  "Navigates to and returns a sequence of all the elements specified by the path."
  [path structure]
  (compiled-select (i/comp-paths* path)
                   structure))

(def ^{:doc "Version of select-one that takes in a path pre-compiled with comp-paths"}
  compiled-select-one i/compiled-select-one*)

(defn select-one*
  "Like select, but returns either one element or nil. Throws exception if multiple elements found"
  [path structure]
  (compiled-select-one (i/comp-paths* path) structure))

(def ^{:doc "Version of select-one! that takes in a path pre-compiled with comp-paths"}
  compiled-select-one! i/compiled-select-one!*)

(defn select-one!*
  "Returns exactly one element, throws exception if zero or multiple elements found"
  [path structure]
  (compiled-select-one! (i/comp-paths* path) structure))

(def ^{:doc "Version of select-first that takes in a path pre-compiled with comp-paths"}
  compiled-select-first i/compiled-select-first*)


(defn select-first*
  "Returns first element found. Not any more efficient than select, just a convenience"
  [path structure]
  (compiled-select-first (i/comp-paths* path) structure))


;; Transformation functions

(def ^{:doc "Version of transform that takes in a path pre-compiled with comp-paths"}
  compiled-transform i/compiled-transform*)

(defn transform*
  "Navigates to each value specified by the path and replaces it by the result of running
  the transform-fn on it"
  [path transform-fn structure]
  (compiled-transform (i/comp-paths* path) transform-fn structure))

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
    nil )
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

(def ALL (comp-paths (i/->AllNavigator)))

(def VAL (i/->ValCollect))

(def LAST (comp-paths (i/->PosNavigator i/get-last i/update-last)))

(def FIRST (comp-paths (i/->PosNavigator i/get-first i/update-first)))

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
    (doall
      (mapcat
        (fn [[s e]] (i/srange-select structure s e next-fn))
        (i/matching-ranges structure pred)
        )))
  (transform* [this structure next-fn]
    (reduce
      (fn [structure [s e]]
        (i/srange-transform structure s e next-fn))
      structure
      (reverse (i/matching-ranges structure pred))
      )))

(def BEGINNING (srange 0 0))

(def END (srange-dynamic count count))

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
  walker
  [afn]
  (select* [this structure next-fn]
    (i/walk-select afn next-fn structure))
  (transform* [this structure next-fn]
    (i/walk-until afn next-fn structure)))

(defnav
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
      (next-fn (get structure k))))
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

(defnav parser [parse-fn unparse-fn]
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
        next-fn))))

(defpathedfn not-selected? [& path]
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
        next-fn))))

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
    (next-fn (if structure structure v)))
  (transform* [this structure next-fn]
    (next-fn (if structure structure v))))

(def NIL->SET (nil->val #{}))
(def NIL->LIST (nil->val '()))
(def NIL->VECTOR (nil->val []))

(defpathedfn collect [& path]
  (pathed-collector [late path]
    (collect-val [this structure]
      (compiled-select late structure)
      )))

(defpathedfn collect-one [& path]
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
  (variable-pathed-nav [compiled-paths conds]
    (select* [this structure next-fn]
      (if-let [selector (i/retrieve-cond-selector compiled-paths structure)]
        (->> (compiled-select selector structure)
             (mapcat next-fn)
             doall)))
    (transform* [this structure next-fn]
      (if-let [selector (i/retrieve-cond-selector compiled-paths structure)]
        (compiled-transform selector next-fn structure)
        structure
        ))))

(defpathedfn if-path
  "Like cond-path, but with if semantics."
  ([cond-p if-path] (cond-path cond-p if-path))
  ([cond-p if-path else-path]
    (cond-path cond-p if-path nil else-path)))

(defpathedfn multi-path
  "A path that branches on multiple paths. For updates,
   applies updates to the paths in order."
  [& paths]
  (variable-pathed-nav [compiled-paths paths]
    (select* [this structure next-fn]
      (->> compiled-paths
           (mapcat #(compiled-select % structure))
           (mapcat next-fn)
           doall
           ))
    (transform* [this structure next-fn]
      (reduce
        (fn [structure path]
          (compiled-transform path next-fn structure))
        structure
        compiled-paths
        ))))

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
