(ns com.rpl.specter.protocols)


(defprotocol StructureValsPath
  (select-full* [this vals structure next-fn])
  (update-full* [this vals structure next-fn]))

(defprotocol StructurePath
  (select* [this structure next-fn])
  (update* [this structure next-fn]))

(defprotocol ValPath
  (select-val [this structure]))

(defprotocol StructurePathComposer
  (comp-structure-paths* [structure-paths]))
