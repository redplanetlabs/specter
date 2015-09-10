(ns com.rpl.specter
  (:use [com.rpl.specter.protocols :only [StructurePath]])
  (:require [com.rpl.specter.impl :as i])
  )

;;TODO: can make usage of vals much more efficient by determining during composition how many vals
;;there are going to be. this should make it much easier to allocate space for vals without doing concats
;;all over the place. The apply to the vals + structure can also be avoided since the number of vals is known
;;beforehand
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

;; Transformfunctions


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
             (fn [e]
               (let [res (transform-fn e)]
                 (if res
                   (let [[ret user-ret] res]
                     (->> user-ret
                          (merge-fn (i/get-cell state))
                          (i/set-cell! state))
                     ret)
                   e
                   )))
             structure)
     (i/get-cell state)]
    ))

(defn replace-in
  "Similar to transform, except returns a pair of [transformd-structure sequence-of-user-ret].
  The transform-fn in this case is expected to return [ret user-ret]. ret is
   what's used to transform the data structure, while user-ret will be added to the user-ret sequence
   in the final return. replace-in is useful for situations where you need to know the specific values
   of what was transformd in the data structure."
  [selector transform-fn structure & {:keys [merge-fn] :or {merge-fn concat}}]
  (compiled-replace-in (i/comp-paths* selector) transform-fn structure :merge-fn merge-fn))

(def bind-params i/bind-params)

;; paramspath* [bindings num-params-sym [impl1 impl2]]

(defmacro paramspath [params & impls]
  (let [num-params (count params)
        retrieve-params (->> params
                          (map-indexed
                            (fn [i p]
                              [p `(aget ~i/PARAMS-SYM
                                        (+ ~i/PARAMS-IDX-SYM ~i))]
                              ))
                          (apply concat))]
    (i/paramspath* retrieve-params num-params impls)
    ))

(defmacro defparamspath [name & body]
  `(def ~name (paramspath ~@body)))

(defmacro params-paramspath [bindings & impls]
  (let [quoted-bindings (->> bindings
                             (partition 2)
                             (map (fn [[sym path-sym]]
                                    [`(quote ~sym) `(quote ~(gensym "path")) path-sym]
                                  )))]
  `(i/params-paramspath* ~quoted-bindings (quote ~impls))
  ))


(defn filterer [& path]
  (let [path (i/comp-paths* path)]
    (params-paramspath [late path]
      (select* [this structure next-fn]
        ;; same code
        )
      (transform* [this structure next-fn]
        ;; same code
        ))))


;;TODO: figure out how to express higher order selectors like filterer, selected?, cond-path
;;     - if keep params-idx in compiledpath too, then:
;;        - needs to emit paramsneeded if it needs params
;;        - at runtime, it converts internal selector into CompiledPath with
;;          the current params/params-idx
;;TODO: figure out how to express srange in terms of srange-dynamic
;;     - will need selector and transformer to call into shared functions
;;TODO: get rid of KeyPath

;; Built-in pathing and context operations

(def ALL (i/->AllStructurePath))

(def VAL (i/->ValCollect))

(def LAST (i/->PosStructurePath last i/set-last))

(def FIRST (i/->PosStructurePath first i/set-first))

;;TODO: should be parameterized
(defn srange-dynamic [start-fn end-fn] (i/->SRangePath start-fn end-fn))

;;TODO: should be parameterized
(defn srange [start end] (srange-dynamic (fn [_] start) (fn [_] end)))

(def BEGINNING (srange 0 0))

(def END (srange-dynamic count count))

(defn walker [afn] (i/->WalkerStructurePath afn))

(defn codewalker [afn] (i/->CodeWalkerStructurePath afn))

;;TODO: needs to parameterize if necessary according to its path
;; same for selected?, not-selected?, transformed, collect, collect-one,
;; cond-path, multi-path
;; TODO: but should only become a late bound object if its internal path
;; is parameterized
;; want an interface that gives regular structure path interface but 
;; creates the right thing

; (higherorderparamspath [late1 path1
;                         late2 path2
;                         late3 path3]
;   (select* [this structure next-fn]
;     (compiled-select late1 ...)
;     ;;TODO: if its multiple paths... where to do the index manipulation...?
;     ;;could take in another arg of "latebound paths" that can then be used internally...
;     ;; but if nothing was higher order, then its just direct
;     ;; this never directly accesses params
;     ))

(defn filterer [& path] (i/->FilterStructurePath (i/comp-paths* path)))

(defparamspath keypath [key]
  (select* [this structure next-fn]
    (next-fn (get structure key)))
  (transform* [this structure next-fn]
    (assoc structure key (next-fn (get structure key)))
    ))

(defn view [afn] (i/->ViewPath afn))

(defn selected?
  "Filters the current value based on whether a selector finds anything.
  e.g. (selected? :vals ALL even?) keeps the current element only if an
  even number exists for the :vals key"
  [& selectors]
  (let [s (i/comp-paths* selectors)]
    (fn [structure]
      (->> structure
           (select s)
           empty?
           not))))

(defn not-selected? [& path]
  (complement (selected? (i/comp-paths* path))))

(defn transformed
  "Navigates to a view of the current value by transforming it with the
   specified selector and update-fn."
  [selector update-fn]
  (let [compiled (i/comp-paths* selector)]
    (view
      (fn [elem]
        (compiled-transform compiled update-fn elem)
        ))))

(extend-type #?(:clj clojure.lang.Keyword :cljs cljs.core/Keyword)
  StructurePath
  (select* [kw structure next-fn]
    (next-fn (get structure kw)))
  (transform* [kw structure next-fn]
    (assoc structure kw (next-fn (get structure kw)))
    ))

(extend-type #?(:clj clojure.lang.AFn :cljs function)
  StructurePath
  (select* [afn structure next-fn]
    (i/filter-select afn structure next-fn))
  (transform* [afn structure next-fn]
    (i/filter-transform afn structure next-fn)))

(extend-type #?(:clj clojure.lang.PersistentHashSet :cljs cljs.core/PersistentHashSet)
  StructurePath
  (select* [aset structure next-fn]
    (i/filter-select aset structure next-fn))
  (transform* [aset structure next-fn]
    (i/filter-transform aset structure next-fn)))

(defn collect [& selector]
  (i/->SelectCollector select (i/comp-paths* selector)))

(defn collect-one [& selector]
  (i/->SelectCollector select-one (i/comp-paths* selector)))

(defn putval
  "Adds an external value to the collected vals. Useful when additional arguments
  are required to the transform function that would otherwise require partial
  application or a wrapper function.

  e.g., incrementing val at path [:a :b] by 3:
  (transform [:a :b (putval 3)] + some-map)"
  [val]
  (i/->PutValCollector val))

(defn cond-path
  "Takes in alternating cond-path selector cond-path selector...
   Tests the structure if selecting with cond-path returns anything.
   If so, it uses the following selector for this portion of the navigation.
   Otherwise, it tries the next cond-path. If nothing matches, then the structure
   is not selected."
  [& conds]
  (->> conds
       (partition 2)
       (map (fn [[c p]] [(i/comp-paths* c) (i/comp-paths* p)]))
       doall
       i/->ConditionalPath
       ))

(defn if-path
  "Like cond-path, but with if semantics."
  ([cond-fn if-path] (cond-path cond-fn if-path))
  ([cond-fn if-path else-path]
    (cond-path cond-fn if-path nil else-path)))

(defn multi-path
  "A path that branches on multiple paths. For updates,
   applies updates to the paths in order."
  [& paths]
  (i/->MultiPath (->> paths (map i/comp-paths*) doall)))
