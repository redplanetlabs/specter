## 0.5.3
* Added cond-path and if-path selectors for choosing paths depending on value of structure at that location

## 0.5.2
* Fix error for selectors with one element defined using comp-paths, e.g. [:a (comp-paths :b)]

## 0.5.1
* Added putval for adding external values to collected values list
* nil is now interpreted as identity selector
* empty selector is now interpreted as identity selector instead of producing error
