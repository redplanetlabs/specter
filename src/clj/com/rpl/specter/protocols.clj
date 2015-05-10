(ns com.rpl.specter.protocols)


;;TODO: can use find-protocol-impl function to avoid all the protocol calls
(defprotocol StructureValsPath
  (select-full* [this vals structure next-fn])
  (update-full* [this vals structure next-fn]))

(defprotocol StructurePath
  (select* [this structure next-fn])
  (update* [this structure next-fn]))

(defprotocol Collector
  (collect-val [this structure]))

(defprotocol StructureValsPathComposer
  (comp-paths* [paths]))
