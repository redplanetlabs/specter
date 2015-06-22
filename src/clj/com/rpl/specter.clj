(ns com.rpl.specter
  (:use [com.rpl.specter impl protocols])
  )

;;TODO: can make usage of vals much more efficient by determining during composition how many vals
;;there are going to be. this should make it much easier to allocate space for vals without doing concats
;;all over the place. The apply to the vals + structure can also be avoided since the number of vals is known
;;beforehand
(defn comp-paths [& paths]
  (comp-paths* (vec paths)))

;; Selector functions

(def ^{:doc "Version of select that takes in a selector pre-compiled with comp-paths"}
  compiled-select compiled-select*)

(defn select
  "Navigates to and returns a sequence of all the elements specified by the selector."
  [selector structure]
  (compiled-select (comp-unoptimal selector)
                   structure))

(defn compiled-select-one
  "Version of select-one that takes in a selector pre-compiled with comp-paths"
  [selector structure]
  (let [res (compiled-select selector structure)]
    (when (> (count res) 1)
      (throw-illegal "More than one element found for params: " selector structure))
    (first res)
    ))

(defn select-one
  "Like select, but returns either one element or nil. Throws exception if multiple elements found"
  [selector structure]
  (compiled-select-one (comp-unoptimal selector) structure))

(defn compiled-select-one!
  "Version of select-one! that takes in a selector pre-compiled with comp-paths"
  [selector structure]
  (let [res (compiled-select-one selector structure)]
    (when (nil? res) (throw-illegal "No elements found for params: " selector structure))
    res
    ))

(defn select-one!
  "Returns exactly one element, throws exception if zero or multiple elements found"
  [selector structure]
  (compiled-select-one! (comp-unoptimal selector) structure))

(defn compiled-select-first
  "Version of select-first that takes in a selector pre-compiled with comp-paths"
  [selector structure]
  (first (compiled-select selector structure)))

(defn select-first
  "Returns first element found. Not any more efficient than select, just a convenience"
  [selector structure]
  (compiled-select-first (comp-unoptimal selector) structure))

;; Update functions


(def ^{:doc "Version of update that takes in a selector pre-compiled with comp-paths"}
  compiled-update compiled-update*)

(defn update
  "Navigates to each value specified by the selector and replaces it by the result of running
  the update-fn on it"
  [selector update-fn structure]
  (compiled-update (comp-unoptimal selector) update-fn structure))

(defn compiled-setval
  "Version of setval that takes in a selector pre-compiled with comp-paths"
  [selector val structure]
  (compiled-update selector (fn [_] val) structure))

(defn setval
  "Navigates to each value specified by the selector and replaces it by val"
  [selector val structure]
  (compiled-setval (comp-unoptimal selector) val structure))

(defn compiled-replace-in
  "Version of replace-in that takes in a selector pre-compiled with comp-paths"
  [selector update-fn structure & {:keys [merge-fn] :or {merge-fn concat}}]
  (let [state (mutable-cell nil)]
    [(compiled-update selector
             (fn [e]
               (let [res (update-fn e)]
                 (if res
                   (let [[ret user-ret] res]
                     (->> user-ret
                          (merge-fn (get-cell state))
                          (set-cell! state))
                     ret)
                   e
                   )))
             structure)
     (get-cell state)]
    ))

(defn replace-in
  "Similar to update, except returns a pair of [updated-structure sequence-of-user-ret].
  The update-fn in this case is expected to return [ret user-ret]. ret is
   what's used to update the data structure, while user-ret will be added to the user-ret sequence
   in the final return. replace-in is useful for situations where you need to know the specific values
   of what was updated in the data structure."
  [selector update-fn structure & {:keys [merge-fn] :or {merge-fn concat}}]
  (compiled-replace-in (comp-unoptimal selector) update-fn structure :merge-fn merge-fn))

;; Built-in pathing and context operations

(def ALL (->AllStructurePath))

(def VAL (->ValCollect))

(def LAST (->LastStructurePath))

(def FIRST (->FirstStructurePath))

(defn srange-dynamic [start-fn end-fn] (->SRangePath start-fn end-fn))

(defn srange [start end] (srange-dynamic (fn [_] start) (fn [_] end)))

(def START (srange 0 0))

(def END (srange-dynamic count count))

(defn walker [afn] (->WalkerStructurePath afn))

(defn codewalker [afn] (->CodeWalkerStructurePath afn))

(defn filterer [& path] (->FilterStructurePath (comp-paths* path)))

(defn keypath [akey] (->KeyPath akey))

(defn view [afn] (->ViewPath afn))

(defmacro viewfn [& args]
  `(view (fn ~@args)))

(defn selected?
  "Filters the current value based on whether a selector finds anything.
  e.g. (selected? :vals ALL even?) keeps the current element only if an
  even number exists for the :vals key"
  [& selectors]
  (let [s (comp-paths* selectors)]
    (fn [structure]
      (->> structure
           (select s)
           empty?
           not))))

(extend-type clojure.lang.Keyword
  StructurePath
  (select* [kw structure next-fn]
    (next-fn (get structure kw)))
  (update* [kw structure next-fn]
    (assoc structure kw (next-fn (get structure kw)))
    ))

(extend-type clojure.lang.AFn
  StructurePath
  (select* [afn structure next-fn]
    (if (afn structure)
      (next-fn structure)))
  (update* [afn structure next-fn]
    (if (afn structure)
      (next-fn structure)
      structure)))

(defn collect [& selector]
  (->SelectCollector select (comp-paths* selector)))

(defn collect-one [& selector]
  (->SelectCollector select-one (comp-paths* selector)))

(defn putval
  "Adds an external value to the collected vals. Useful when additional arguments
  are required to the update function that would otherwise require partial
  application or a wrapper function.

  e.g., incrementing val at path [:a :b] by 3:
  (update [:a :b (putval 3)] + some-map)"
  [val]
  (->PutValCollector val))

(defn cond-path
  "Takes in alternating cond-path selector cond-path selector...
   Tests the structure if selecting with cond-path returns anything.
   If so, it uses the following selector for this portion of the navigation.
   Otherwise, it tries the next cond-path. If nothing matches, then the structure
   is not selected."
  [& conds]
  (->> conds
       (partition 2)
       (map (fn [[c p]] [(comp-paths* c) (comp-paths* p)]))
       doall
       ->ConditionalPath
       ))

(defn if-path
  "Like cond-path, but with if semantics."
  ([cond-fn if-path] (cond-path cond-fn if-path))
  ([cond-fn if-path else-path]
    (cond-path cond-fn if-path nil else-path)))
