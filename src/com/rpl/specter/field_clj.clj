(ns com.rpl.specter.field-clj)

(defmacro field [obj field]
  `(. ~obj ~field))
