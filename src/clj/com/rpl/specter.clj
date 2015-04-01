(ns com.rpl.specter
  (:use [com.rpl.specter impl protocols])
  )

;;TODO: can make usage of vals much more efficient by determining during composition how many vals
;;there are going to be. this should make it much easier to allocate space for vals without doing concats
;;all over the place. The apply to the vals + structure can also be avoided since the number of vals is known
;;beforehand
(defn comp-structure-paths [& structure-paths]
  (comp-structure-paths* (vec structure-paths)))

;; Selector functions

(defn select [selector structure]
  (let [sp (comp-structure-paths* selector)]
    (select* sp
             []
             structure
             (fn [vals structure]
               (if-not (empty? vals) [(conj vals structure)] [structure])))
    ))

(defn select-one
  "Like select, but returns either one element or nil. Throws exception if multiple elements returned"
  [selector structure]
  (let [res (select selector structure)]
    (when (> (count res) 1)
      (throw-illegal "More than one element found for params: " selector structure))
    (first res)
    ))

(defn select-one!
  "Returns exactly one element, throws exception if zero or multiple elements returned"
  [selector structure]
  (let [res (select-one selector structure)]
    (when (nil? res) (throw-illegal "No elements found for params: " selector structure))
    res
    ))

(defn select-first
  "Returns first element returned. Not any more efficient than select, just a convenience"
  [selector structure]
  (first (select selector structure)))

;; Update functions

(defn update [selector update-fn structure]
  (let [selector (comp-structure-paths* selector)]
    (update* selector
             []
             structure
             (fn [vals structure]
               (if (empty? vals)
                 (update-fn structure)
                 (apply update-fn (conj vals structure)))
               ))))

(defn setval [selector val structure]
  (update selector (fn [_] val) structure))

(defn replace-in [selector update-fn structure & {:keys [merge-fn] :or {merge-fn concat}}]
  "Returns [new structure [<user-ret> <user-ret>...]"
  (let [state (mutable-cell nil)]
    [(update selector
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

;; Built-in pathing and context operations

(def ALL (->AllStructurePath))

(def VAL (->ValStructurePath))

(def LAST (->LastStructurePath))

(def FIRST (->FirstStructurePath))

(defn walker [afn] (->WalkerStructurePath afn))

(defn codewalker [afn] (->CodeWalkerStructurePath afn))

(defn filterer [afn] (->FilterStructurePath afn))

(defn keypath [akey] (->KeyPath akey))

(extend-type clojure.lang.Keyword
  StructurePath
  (select* [kw vals structure next-fn]
    (key-select kw vals structure next-fn))
  (update* [kw vals structure next-fn]
    (key-update kw vals structure next-fn)
    ))

(extend-type clojure.lang.AFn
  StructurePath
  (select* [afn vals structure next-fn]
    (if (afn structure)
      (next-fn vals structure)))
  (update* [afn vals structure next-fn]
    (if (afn structure)
      (next-fn vals structure)
      structure)))

(defn val-selector [& selector]
  (->SelectorValsPath select (comp-structure-paths* selector)))

(defn val-selector-one [& selector]
  (->SelectorValsPath select-one (comp-structure-paths* selector)))
