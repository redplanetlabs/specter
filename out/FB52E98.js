goog.provide('cljs.nodejs');
goog.require('cljs.core');
cljs.nodejs.require = require;
cljs.nodejs.process = process;
cljs.nodejs.enable_util_print_BANG_ = (function cljs$nodejs$enable_util_print_BANG_(){
cljs.core._STAR_print_newline_STAR_ = false;

return cljs.core._STAR_print_fn_STAR_ = (function() { 
var G__566__delegate = function (args){
return console.log.apply(console,cljs.core.into_array.call(null,args));
};
var G__566 = function (var_args){
var args = null;
if (arguments.length > 0) {
var G__567__i = 0, G__567__a = new Array(arguments.length -  0);
while (G__567__i < G__567__a.length) {G__567__a[G__567__i] = arguments[G__567__i + 0]; ++G__567__i;}
  args = new cljs.core.IndexedSeq(G__567__a,0);
} 
return G__566__delegate.call(this,args);};
G__566.cljs$lang$maxFixedArity = 0;
G__566.cljs$lang$applyTo = (function (arglist__568){
var args = cljs.core.seq(arglist__568);
return G__566__delegate(args);
});
G__566.cljs$core$IFn$_invoke$arity$variadic = G__566__delegate;
return G__566;
})()
;
});
