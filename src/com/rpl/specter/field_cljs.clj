(ns com.rpl.specter.field-cljs)

(defmacro field [obj field]
  (let [getter (symbol (str ".-" field))]
  	`(~getter ~obj)
  	))
