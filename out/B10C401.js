goog.provide('cljs.nodejs');
goog.require('cljs.core');
cljs.nodejs.require = require;
cljs.nodejs.process = process;
cljs.nodejs.enable_util_print_BANG_ = (function cljs$nodejs$enable_util_print_BANG_(){
cljs.core._STAR_print_newline_STAR_ = false;

return cljs.core._STAR_print_fn_STAR_ = (function() { 
var G__613__delegate = function (args){
return console.log.apply(console,cljs.core.into_array.call(null,args));
};
var G__613 = function (var_args){
var args = null;
if (arguments.length > 0) {
var G__614__i = 0, G__614__a = new Array(arguments.length -  0);
while (G__614__i < G__614__a.length) {G__614__a[G__614__i] = arguments[G__614__i + 0]; ++G__614__i;}
  args = new cljs.core.IndexedSeq(G__614__a,0);
} 
return G__613__delegate.call(this,args);};
G__613.cljs$lang$maxFixedArity = 0;
G__613.cljs$lang$applyTo = (function (arglist__615){
var args = cljs.core.seq(arglist__615);
return G__613__delegate(args);
});
G__613.cljs$core$IFn$_invoke$arity$variadic = G__613__delegate;
return G__613;
})()
;
});
