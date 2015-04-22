(ns com.rpl.specter.impl
  (:use [com.rpl.specter protocols])
  (:require [clojure.walk :as walk]
            [clojure.core.reducers :as r])
  )

(defn benchmark [iters afn]
  (time
   (dotimes [_ iters]
     (afn))))

(defprotocol CoerceStructureValsPath
  (coerce-path [this]))

(extend-protocol CoerceStructureValsPath

  com.rpl.specter.protocols.StructureValsPath
  (coerce-path [this] this)

  com.rpl.specter.protocols.Collector
  (coerce-path [collector]
    (reify StructureValsPath
      (select-full* [this vals structure next-fn]
        (next-fn (conj vals (collect-val collector structure)) structure))
      (update-full* [this vals structure next-fn]
        (next-fn (conj vals (collect-val collector structure)) structure))))

  ;; need to say Object instead of StructurePath so that things like Keyword are properly coerced
  Object
  (coerce-path [spath]
    (reify StructureValsPath
      (select-full* [this vals structure next-fn]
        (select* spath structure (fn [structure] (next-fn vals structure))))
      (update-full* [this vals structure next-fn]
        (update* spath structure (fn [structure] (next-fn vals structure)))
        )))
  )


(extend-protocol StructureValsPathComposer
  Object
  (comp-paths* [sp]
    (coerce-path sp))
  java.util.List
  (comp-paths* [structure-paths]
    (reduce (fn [sp-curr sp]
              (reify StructureValsPath
                (select-full* [this vals structure next-fn]
                  (select-full* sp vals structure
                                (fn [vals-next structure-next]
                                  (select-full* sp-curr vals-next structure-next next-fn)))
                  )
                (update-full* [this vals structure next-fn]
                  (update-full* sp vals structure
                                (fn [vals-next structure-next]
                                  (update-full* sp-curr vals-next structure-next next-fn))))
                ))
          (->> structure-paths flatten (map coerce-path) reverse))
    ))

;; cell implementation idea taken from prismatic schema library
(definterface PMutableCell
  (get_cell ^Object [])
  (set_cell [^Object x]))

(deftype MutableCell [^:volatile-mutable ^Object q]
  PMutableCell
  (get_cell [this] q)
  (set_cell [this x] (set! q x)))

(defn mutable-cell ^PMutableCell
  ([] (mutable-cell nil))
  ([init] (MutableCell. init)))

(defn set-cell! [^PMutableCell cell val]
  (.set_cell cell val))

(defn get-cell [^PMutableCell cell]
  (.get_cell cell))

(defmacro throw* [etype & args]
  `(throw (new ~etype (pr-str ~@args))))

(defmacro throw-illegal [& args]
  `(throw* IllegalArgumentException ~@args))

(defn update-cell! [cell afn]
  (let [ret (afn (get-cell cell))]
    (set-cell! cell ret)
    ret))

(defn- append [coll elem]
  (-> coll vec (conj elem)))

(defprotocol SetExtremes
  (set-first [s val])
  (set-last [s val]))

(defn- set-first-list [l v]
  (cons v (rest l)))

(defn- set-last-list [l v]
  (append (butlast l) v))

(extend-protocol SetExtremes
  clojure.lang.PersistentVector
  (set-first [v val]
    (assoc v 0 val))
  (set-last [v val]
    (assoc v (-> v count dec) val))
  Object
  (set-first [l val]
    (set-first-list l val))
  (set-last [l val]
    (set-last-list l val)
    ))

(defn- walk-until [pred on-match-fn structure]
  (if (pred structure)
    (on-match-fn structure)
    (walk/walk (partial walk-until pred on-match-fn) identity structure)
    ))

(defn- fn-invocation? [f]
  (or (instance? clojure.lang.Cons f)
      (instance? clojure.lang.LazySeq f)
      (list? f)))

(defn- codewalk-until [pred on-match-fn structure]
  (if (pred structure)
    (on-match-fn structure)
    (let [ret (walk/walk (partial codewalk-until pred on-match-fn) identity structure)]
      (if (and (fn-invocation? structure) (fn-invocation? ret))
        (with-meta ret (meta structure))
        ret
        ))))

(defn- conj-all! [cell elems]
  (set-cell! cell (concat (get-cell cell) elems)))

;; returns vector of all results
(defn- walk-select [pred continue-fn structure]
  (let [ret (mutable-cell [])
        walker (fn this [structure]
                 (if (pred structure)
                   (conj-all! ret (continue-fn structure))
                   (walk/walk this identity structure))
                 )]
    (walker structure)
    (get-cell ret)
    ))

(defn- filter+ancestry [afn aseq]
  (let [aseq (vec aseq)]
    (reduce (fn [[s m :as orig] i]
              (let [e (get aseq i)
                    pos (count s)]
                (if (afn e)
                  [(conj s e) (assoc m pos i)]
                  orig
                  )))
            [[] {}]
            (range (count aseq))
            )))

(defn key-select [akey structure next-fn]
  (next-fn (get structure akey)))

(defn key-update [akey structure next-fn]
  (assoc structure akey (next-fn (get structure akey))
  ))

(deftype AllStructurePath []
  StructurePath
  (select* [this structure next-fn]
    (into [] (r/mapcat next-fn structure)))
  (update* [this structure next-fn]
    (let [empty-structure (empty structure)]
      (if (list? empty-structure)
        ;; this is done to maintain order, otherwise lists get reversed
        (doall (map next-fn structure))
        (->> structure (r/map next-fn) (into empty-structure))
        ))))

(deftype ValCollect []
  Collector
  (collect-val [this structure]
    structure))

(deftype LastStructurePath []
  StructurePath
  (select* [this structure next-fn]
    (next-fn (last structure)))
  (update* [this structure next-fn]
    (set-last structure (next-fn (last structure)))))

(deftype FirstStructurePath []
  StructurePath
  (select* [this structure next-fn]
    (next-fn (first structure)))
  (update* [this structure next-fn]
    (set-first structure (next-fn (first structure)))))

(deftype WalkerStructurePath [afn]
  StructurePath
  (select* [this structure next-fn]
    (walk-select afn next-fn structure))
  (update* [this structure next-fn]
    (walk-until afn next-fn structure)))

(deftype CodeWalkerStructurePath [afn]
  StructurePath
  (select* [this structure next-fn]
    (walk-select afn next-fn structure))
  (update* [this structure next-fn]
    (codewalk-until afn next-fn structure)))

(deftype FilterStructurePath [afn]
  StructurePath
  (select* [this structure next-fn]
    (next-fn (filter afn structure)))
  (update* [this structure next-fn]
    (let [[filtered ancestry] (filter+ancestry afn structure)
          ;; the vec is necessary so that we can get by index later
          ;; (can't get by index for cons'd lists)
          next (vec (next-fn filtered))]
      (reduce (fn [curr [newi oldi]]
                (assoc curr oldi (get next newi)))
              (vec structure)
              ancestry))))

(deftype KeyPath [akey]
  StructurePath
  (select* [this structure next-fn]
    (key-select akey structure next-fn))
  (update* [this structure next-fn]
    (key-update akey structure next-fn)
    ))

(deftype SelectCollector [sel-fn selector]
  Collector
  (collect-val [this structure]
    (sel-fn selector structure)))

(deftype SRangePath [start-fn end-fn]
  StructurePath
  (select* [this structure next-fn]
    (let [start (start-fn structure)
          end (end-fn structure)]
      (next-fn (-> structure vec (subvec start end)))
      ))
  (update* [this structure next-fn]
    (let [start (start-fn structure)
          end (end-fn structure)
          structurev (vec structure)
          newpart (next-fn (-> structurev (subvec start end)))
          res (concat (subvec structurev 0 start)
                      newpart
                      (subvec structurev end (count structure)))]
      (if (vector? structure)
        (vec res)
        res
        ))))

(deftype ViewPath [view-fn]
  StructurePath
  (select* [this structure next-fn]
    (-> structure view-fn next-fn))
  (update* [this structure next-fn]
    (-> structure view-fn next-fn)
    ))

(deftype SplitPath [selectors]
  StructureValsPath
  (select-full* [this vals structure next-fn]
    (into [] (r/mapcat #(select-full* % vals structure next-fn) selectors)))
  (update-full* [this vals structure next-fn]
    (reduce (fn [structure s] (update-full* s vals structure next-fn)) structure selectors)
    ))
