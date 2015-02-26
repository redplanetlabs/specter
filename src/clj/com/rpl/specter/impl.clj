(ns com.rpl.specter.impl
  (:use [com.rpl.specter protocols])
  (:require [clojure.walk :as walk]
            [clojure.core.reducers :as r])
  )

(extend-protocol StructurePathComposer
  Object
  (comp-structure-paths* [sp]
    sp)
  java.util.List
  (comp-structure-paths* [structure-paths]
    (reduce (fn [sp-curr sp]
              (reify StructurePath
                (select* [this vals structure next-fn]
                  (select* sp vals structure
                           (fn [vals-next structure-next]
                             (select* sp-curr vals-next structure-next next-fn)))
                  )
                (update* [this vals structure next-fn]
                  (update* sp vals structure
                           (fn [vals-next structure-next]
                             (update* sp-curr vals-next structure-next next-fn))))
                ))
          (-> structure-paths flatten reverse))
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

(defn key-select [akey vals structure next-fn]
  (next-fn vals (get structure akey)))

(defn key-update [akey vals structure next-fn]
  (assoc structure akey (next-fn vals (get structure akey))
  ))

(deftype AllStructurePath []
  StructurePath
  (select* [this vals structure next-fn]
    (into [] (r/mapcat (partial next-fn vals) structure)))
  (update* [this vals structure next-fn]
    (let [ret (r/map (partial next-fn vals) structure)]
      (cond (vector? structure)
            (into [] ret)

            (map? structure)
            (into {} ret)
            
            :else
            (into '() ret)))
    ))

(deftype ValStructurePath []
  StructurePath
  (select* [this vals structure next-fn]
    (next-fn (conj vals structure) structure))
  (update* [this vals structure next-fn]
    (next-fn (conj vals structure) structure)))

(deftype LastStructurePath []
  StructurePath
  (select* [this vals structure next-fn]
    (next-fn vals (last structure)))
  (update* [this vals structure next-fn]
    (set-last structure (next-fn vals (last structure)))))

(deftype FirstStructurePath []
  StructurePath
  (select* [this vals structure next-fn]
    (next-fn vals (first structure)))
  (update* [this vals structure next-fn]
    (set-first structure (next-fn vals (first structure)))))

(deftype WalkerStructurePath [afn]
  StructurePath
  (select* [this vals structure next-fn]
    (walk-select afn (partial next-fn vals) structure))
  (update* [this vals structure next-fn]
    (walk-until afn (partial next-fn vals) structure)))

(deftype CodeWalkerStructurePath [afn]
  StructurePath
  (select* [this vals structure next-fn]
    (walk-select afn (partial next-fn vals) structure))
  (update* [this vals structure next-fn]
    (codewalk-until afn (partial next-fn vals) structure)))

(deftype FilterStructurePath [afn]
  StructurePath
  (select* [this vals structure next-fn]
    (next-fn vals (filter afn structure)))
  (update* [this vals structure next-fn]
    (let [[filtered ancestry] (filter+ancestry afn structure)
          ;; the vec is necessary so that we can get by index later
          ;; (can't get by index for cons'd lists)
          next (vec (next-fn vals filtered))]
      (reduce (fn [curr [newi oldi]]
                (assoc curr oldi (get next newi)))
              (vec structure)
              ancestry))))

(deftype KeyPath [akey]
  StructurePath
  (select* [this vals structure next-fn]
    (key-select akey vals structure next-fn))
  (update* [this vals structure next-fn]
    (key-update akey vals structure next-fn)
    ))

(defn- selector-vals* [sel-fn selector vals structure next-fn]
  (next-fn (vec (concat vals
                        [(sel-fn selector structure)]))
           structure))

(deftype SelectorValsPath [sel-fn selector]
  StructurePath
  (select* [this vals structure next-fn]
    (selector-vals* sel-fn selector vals structure next-fn))
  (update* [this vals structure next-fn]
    (selector-vals* sel-fn selector vals structure next-fn)))
