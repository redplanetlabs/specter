(ns com.rpl.specter.protocols)


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