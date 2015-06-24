goog.provide('cljs.nodejs');
goog.require('cljs.core');
cljs.nodejs.require = require;
cljs.nodejs.process = process;
cljs.nodejs.enable_util_print_BANG_ = (function cljs$nodejs$enable_util_print_BANG_(){
cljs.core._STAR_print_newline_STAR_ = false;

return cljs.core._STAR_print_fn_STAR_ = (function() { 
var G__316__delegate = function (args){
return console.log.apply(console,cljs.core.into_array.call(null,args));
};
var G__316 = function (var_args){
var args = null;
if (arguments.length > 0) {
var G__317__i = 0, G__317__a = new Array(arguments.length -  0);
while (G__317__i < G__317__a.length) {G__317__a[G__317__i] = arguments[G__317__i + 0]; ++G__317__i;}
  args = new cljs.core.IndexedSeq(G__317__a,0);
} 
return G__316__delegate.call(this,args);};
G__316.cljs$lang$maxFixedArity = 0;
G__316.cljs$lang$applyTo = (function (arglist__318){
var args = cljs.core.seq(arglist__318);
return G__316__delegate(args);
});
G__316.cljs$core$IFn$_invoke$arity$variadic = G__316__delegate;
return G__316;
})()
;
});
