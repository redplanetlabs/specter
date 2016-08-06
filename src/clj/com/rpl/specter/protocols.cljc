(ns com.rpl.specter.protocols)

(defprotocol Navigator
  "Do not use this protocol directly. All navigators must be created using
  com.rpl.specter.macros namespace."
  (select* [this structure next-fn]
    "An implementation of `select*` must call `next-fn` on each
     subvalue of `structure`. The result of `select*` is specified
     as follows:

     1. `NONE` if `next-fn` never called
     2. `NONE` if all calls to `next-fn` return `NONE`
     3. Otherwise, any non-`NONE` return value from calling `next-fn`
     ")
  (transform* [this structure next-fn]
    "An implementation of `transform*` must use `next-fn` to transform
     any subvalues of `structure` and then merge those transformed values
     back into `structure`. Everything else in `structure` must be unchanged."
    ))

(defprotocol Collector
  "Do not use this protocol directly. All navigators must be created using
  com.rpl.specter.macros namespace."
  (collect-val [this structure]))

(defprotocol ImplicitNav
  (implicit-nav [obj]))
