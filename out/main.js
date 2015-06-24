goog.addDependency("base.js", ['goog'], []);
goog.addDependency("../cljs/core.js", ['cljs.core'], ['goog.string', 'goog.object', 'goog.string.StringBuffer', 'goog.array']);
goog.addDependency("../clojure/core/reducers.js", ['clojure.core.reducers'], ['cljs.core']);
goog.addDependency("../clojure/walk.js", ['clojure.walk'], ['cljs.core']);
goog.addDependency("../com/rpl/specter/protocols.js", ['com.rpl.specter.protocols'], ['cljs.core']);
goog.addDependency("../com/rpl/specter/impl.js", ['com.rpl.specter.impl'], ['clojure.core.reducers', 'cljs.core', 'clojure.walk', 'com.rpl.specter.protocols']);
goog.addDependency("../com/rpl/specter.js", ['com.rpl.specter'], ['cljs.core', 'com.rpl.specter.impl', 'com.rpl.specter.protocols']);
