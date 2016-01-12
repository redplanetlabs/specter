(ns com.rpl.specter
  #+cljs (:require-macros
            [com.rpl.specter.macros
              :refer
              [pathed-collector
               variable-pathed-path
               fixed-pathed-path
               defcollector
               defpath
               paramscollector
               paramspath
              ]]
            )
  (:use [com.rpl.specter.protocols :only [StructurePath]]
    #+clj [com.rpl.specter.macros :only
            [pathed-collector
             variable-pathed-path
             fixed-pathed-path
             defcollector
             defpath
             paramscollector
             paramspath]]
    )
  (:require [com.rpl.specter.impl :as i]
            [clojure.set :as set])
  )

(defn comp-paths [& paths]
  (i/comp-paths* (vec paths)))

;; Selector functions

(def ^{:doc "Version of select that takes in a selector pre-compiled with comp-paths"}
  compiled-select i/compiled-select*)

(defn select
  "Navigates to and returns a sequence of all the elements specified by the selector."
  [selector structure]
  (compiled-select (i/comp-paths* selector)
                   structure))

(defn compiled-select-one
  "Version of select-one that takes in a selector pre-compiled with comp-paths"
  [selector structure]
  (let [res (compiled-select selector structure)]
    (when (> (count res) 1)
      (i/throw-illegal "More than one element found for params: " selector structure))
    (first res)
    ))

(defn select-one
  "Like select, but returns either one element or nil. Throws exception if multiple elements found"
  [selector structure]
  (compiled-select-one (i/comp-paths* selector) structure))

(defn compiled-select-one!
  "Version of select-one! that takes in a selector pre-compiled with comp-paths"
  [selector structure]
  (let [res (compiled-select selector structure)]
    (when (not= 1 (count res)) (i/throw-illegal "Expected exactly one element for params: " selector structure))
    (first res)
    ))

(defn select-one!
  "Returns exactly one element, throws exception if zero or multiple elements found"
  [selector structure]
  (compiled-select-one! (i/comp-paths* selector) structure))

(defn compiled-select-first
  "Version of select-first that takes in a selector pre-compiled with comp-paths"
  [selector structure]
  (first (compiled-select selector structure)))

(defn select-first
  "Returns first element found. Not any more efficient than select, just a convenience"
  [selector structure]
  (compiled-select-first (i/comp-paths* selector) structure))


;; Transform functions

(def ^{:doc "Version of transform that takes in a selector pre-compiled with comp-paths"}
  compiled-transform i/compiled-transform*)

(defn transform
  "Navigates to each value specified by the selector and replaces it by the result of running
  the transform-fn on it"
  [selector transform-fn structure]
  (compiled-transform (i/comp-paths* selector) transform-fn structure))

(defn compiled-setval
  "Version of setval that takes in a selector pre-compiled with comp-paths"
  [selector val structure]
  (compiled-transform selector (fn [_] val) structure))

(defn setval
  "Navigates to each value specified by the selector and replaces it by val"
  [selector val structure]
  (compiled-setval (i/comp-paths* selector) val structure))

(defn compiled-replace-in
  "Version of replace-in that takes in a selector pre-compiled with comp-paths"
  [selector transform-fn structure & {:keys [merge-fn] :or {merge-fn concat}}]
  (let [state (i/mutable-cell nil)]
    [(compiled-transform selector
             (fn [& args]
               (let [res (apply transform-fn args)]
                 (if res
                   (let [[ret user-ret] res]
                     (->> user-ret
                          (merge-fn (i/get-cell state))
                          (i/set-cell! state))
                     ret)
                   (last args)
                   )))
             structure)
     (i/get-cell state)]
    ))

(defn replace-in
  "Similar to transform, except returns a pair of [transformed-structure sequence-of-user-ret].
  The transform-fn in this case is expected to return [ret user-ret]. ret is
   what's used to transform the data structure, while user-ret will be added to the user-ret sequence
   in the final return. replace-in is useful for situations where you need to know the specific values
   of what was transformed in the data structure."
  [selector transform-fn structure & {:keys [merge-fn] :or {merge-fn concat}}]
  (compiled-replace-in (i/comp-paths* selector) transform-fn structure :merge-fn merge-fn))

;; Helpers for defining selectors and collectors with late-bound params

(def ^{:doc "Takes a compiled selector that needs late-bound params and supplies it with
             an array of params and a position in the array from which to begin reading
             params. The return value is an executable selector."}
  bind-params* i/bind-params*)

;; Built-in pathing and context operations

(defpath
  ^{:doc "Stops navigation at this point. For selection returns nothing and for 
          transformation returns the structure unchanged"}
  VOID
  []
  (select* [this structure next-fn]
    nil )
  (transform* [this structure next-fn]
    structure
    ))

(def ALL (comp-paths (i/->AllStructurePath)))

(def VAL (i/->ValCollect))

(def LAST (comp-paths (i/->PosStructurePath last i/set-last)))

(def FIRST (comp-paths (i/->PosStructurePath first i/set-first)))

(defpath
  ^{:doc "Uses start-fn and end-fn to determine the bounds of the subsequence
          to select when navigating. Each function takes in the structure as input."}
  srange-dynamic
  [start-fn end-fn]
  (select* [this structure next-fn]
    (i/srange-select structure (start-fn structure) (end-fn structure) next-fn))
  (transform* [this structure next-fn]
    (i/srange-transform structure (start-fn structure) (end-fn structure) next-fn)
    ))

(defpath
  ^{:doc "Navigates to the subsequence bound by the indexes start (inclusive)
          and end (exclusive)"}
  srange
  [start end]
  (select* [this structure next-fn]
    (i/srange-select structure start end next-fn))
  (transform* [this structure next-fn]
    (i/srange-transform structure start end next-fn)
    ))

(def BEGINNING (srange 0 0))

(def END (srange-dynamic count count))

(defpath
  ^{:doc "Navigates to the specified subset (by taking an intersection).
          In a transform, that subset in the original set is changed to the
          new value of the subset."}
  subset
  [aset]
  (select* [this structure next-fn]
    (next-fn (set/intersection structure aset)))
  (transform* [this structure next-fn]
    (let [subset  (set/intersection structure aset)
          newset (next-fn subset)]
      (-> structure
          (set/difference subset)
          (set/union newset))
          )))

(defpath
  walker
  [afn]
  (select* [this structure next-fn]
    (i/walk-select afn next-fn structure))
  (transform* [this structure next-fn]
    (i/walk-until afn next-fn structure)))

(defpath
  codewalker
  [afn]
  (select* [this structure next-fn]
    (i/walk-select afn next-fn structure))
  (transform* [this structure next-fn]
    (i/codewalk-until afn next-fn structure)))

(defn filterer
  "Navigates to a view of the current sequence that only contains elements that
  match the given selector path. An element matches the selector path if calling select
  on that element with the selector path yields anything other than an empty sequence.

   The input path may be parameterized, in which case the result of filterer
   will be parameterized in the order of which the parameterized selectors
   were declared."
  [& path]
  (fixed-pathed-path [late path]
    (select* [this structure next-fn]
      (->> structure (filter #(i/selected?* late %)) doall next-fn))
    (transform* [this structure next-fn]
      (let [[filtered ancestry] (i/filter+ancestry late structure)
            ;; the vec is necessary so that we can get by index later
            ;; (can't get by index for cons'd lists)
            next (vec (next-fn filtered))]
        (reduce (fn [curr [newi oldi]]
                  (assoc curr oldi (get next newi)))
                (vec structure)
                ancestry))
      )))


(defpath keypath [key]
  (select* [this structure next-fn]
    (next-fn (get structure key)))
  (transform* [this structure next-fn]
    (assoc structure key (next-fn (get structure key)))
    ))

(defpath view [afn]
  (select* [this structure next-fn]
    (next-fn (afn structure)))
  (transform* [this structure next-fn]
    (next-fn (afn structure))
    ))

(defn selected?
  "Filters the current value based on whether a selector finds anything.
  e.g. (selected? :vals ALL even?) keeps the current element only if an
  even number exists for the :vals key.

  The input path may be parameterized, in which case the result of selected?
  will be parameterized in the order of which the parameterized selectors
  were declared."
  [& path]
  (fixed-pathed-path [late path]
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

(defn not-selected? [& path]
  (fixed-pathed-path [late path]
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

(defn transformed
  "Navigates to a view of the current value by transforming it with the
   specified selector and update-fn.

   The input path may be parameterized, in which case the result of transformed
   will be parameterized in the order of which the parameterized selectors
   were declared."
  [path update-fn]
  (fixed-pathed-path [late path]
    (select* [this structure next-fn]
      (next-fn (compiled-transform late update-fn structure)))
    (transform* [this structure next-fn]
      (next-fn (compiled-transform late update-fn structure)))))

(extend-type #+clj clojure.lang.Keyword #+cljs cljs.core/Keyword
  StructurePath
  (select* [kw structure next-fn]
    (next-fn (get structure kw)))
  (transform* [kw structure next-fn]
    (assoc structure kw (next-fn (get structure kw)))
    ))

(extend-type #+clj clojure.lang.AFn #+cljs function
  StructurePath
  (select* [afn structure next-fn]
    (i/filter-select afn structure next-fn))
  (transform* [afn structure next-fn]
    (i/filter-transform afn structure next-fn)))

(extend-type #+clj clojure.lang.PersistentHashSet #+cljs cljs.core/PersistentHashSet
  StructurePath
  (select* [aset structure next-fn]
    (i/filter-select aset structure next-fn))
  (transform* [aset structure next-fn]
    (i/filter-transform aset structure next-fn)))

(defpath
  ^{:doc "Keeps the element only if it matches the supplied predicate. This is the
          late-bound parameterized version of using a function directly in a path."}
  pred
  [afn]
  (select* [this structure next-fn]
    (i/filter-select afn structure next-fn))
  (transform* [this structure next-fn]
    (i/filter-transform afn structure next-fn)))

(defpath
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

(defn collect [& path]
  (pathed-collector [late path]
    (collect-val [this structure]
      (compiled-select late structure)
      )))

(defn collect-one [& path]
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

(defn cond-path
  "Takes in alternating cond-path selector cond-path selector...
   Tests the structure if selecting with cond-path returns anything.
   If so, it uses the following selector for this portion of the navigation.
   Otherwise, it tries the next cond-path. If nothing matches, then the structure
   is not selected.

   The input paths may be parameterized, in which case the result of cond-path
   will be parameterized in the order of which the parameterized selectors
   were declared."
  [& conds]
  (variable-pathed-path [compiled-paths conds]
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

(defn if-path
  "Like cond-path, but with if semantics."
  ([cond-p if-path] (cond-path cond-p if-path))
  ([cond-p if-path else-path]
    (cond-path cond-p if-path nil else-path)))

(defn multi-path
  "A path that branches on multiple paths. For updates,
   applies updates to the paths in order."
  [& paths]
  (variable-pathed-path [compiled-paths paths]
    (select* [this structure next-fn]
      (->> compiled-paths
           (mapcat #(compiled-select % structure))
           (mapcat next-fn)
           doall
           ))
    (transform* [this structure next-fn]
      (reduce
        (fn [structure selector]
          (compiled-transform selector next-fn structure))
        structure
        compiled-paths
        ))))
