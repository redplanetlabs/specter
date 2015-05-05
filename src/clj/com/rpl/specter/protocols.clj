(ns com.rpl.specter.protocols)


;;TODO: might be able to speed it up more by having these return a function rather than 
;;rely on protocol methods (can then compose functions together directly)
;;so protocol would just be used during composition - what about execution?
;;could have a select-fast function that takes in a selector FUNCTION explicitly
(defprotocol StructureValsPath
  (select-full* [this vals structure next-fn])
  (update-full* [this vals structure next-fn]))

(defprotocol StructurePath
  (select* [this structure next-fn])
  (update* [this structure next-fn]))

(defprotocol Collector
  (collect-val [this structure]))

;;TODO: Collectors in sequence become a StructureValsPath that does all collection at once
;;StructurePath in sequence become a single StructurePath
;;any StructureValsPath composed with anything becomes a StructureValsPath
;;TODO: update update/select to be able to execute a StructurePath directly without coercing it
;;   - this will avoid MANY layers of indirection and overhead
