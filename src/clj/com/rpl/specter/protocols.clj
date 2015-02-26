(ns com.rpl.specter.protocols)

(defprotocol StructurePath
  (select* [this vals structure next-fn])
  (update* [this vals structure next-fn])
  )

(defprotocol StructurePathComposer
  (comp-structure-paths* [structure-paths]))
