(ns com.rpl.specter.zipper
  #+cljs (:require-macros
            [com.rpl.specter.macros
              :refer [fixed-pathed-path defpath]])
  (:use
    #+clj [com.rpl.specter.macros :only [fixed-pathed-path defpath]]
    [com.rpl specter])
  (:require [clojure [zip :as zip]]))

(def VECTOR-ZIP (view zip/vector-zip))
(def SEQ-ZIP (view zip/seq-zip))
(def XML-ZIP (view zip/xml-zip))
(def NEXT (view zip/next))
(def RIGHT (view zip/right))
(def RIGHTMOST (view zip/rightmost))
(def LEFT (view zip/left))
(def DOWN (view zip/down))
(def LEFTMOST (view zip/leftmost))
(def UP (view zip/up))
(def ROOT (view zip/root))

(defpath NODE []
  (select* [this structure next-fn]
    (next-fn (zip/node structure))
    )
  (transform* [this structure next-fn]
    (zip/root (zip/edit structure next-fn))
    ))

(defn edited [path update-fn]
  (fixed-pathed-path [late path]
    (select* [this structure next-fn]
      (next-fn
        (zip/edit structure
                  #(compiled-transform late update-fn %))))
    (transform* [this structure next-fn]
      (next-fn
        (zip/edit structure
                  #(compiled-transform late update-fn %))))))
