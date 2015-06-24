goog.provide('cljs.nodejs');
goog.require('cljs.core');
cljs.nodejs.require = require;
cljs.nodejs.process = process;
cljs.nodejs.enable_util_print_BANG_ = (function cljs$nodejs$enable_util_print_BANG_(){
cljs.core._STAR_print_newline_STAR_ = false;

return cljs.core._STAR_print_fn_STAR_ = (function() { 
var G__461__delegate = function (args){
return console.log.apply(console,cljs.core.into_array.call(null,args));
};
var G__461 = function (var_args){
var args = null;
if (arguments.length > 0) {
var G__462__i = 0, G__462__a = new Array(arguments.length -  0);
while (G__462__i < G__462__a.length) {G__462__a[G__462__i] = arguments[G__462__i + 0]; ++G__462__i;}
  args = new cljs.core.IndexedSeq(G__462__a,0);
} 
return G__461__delegate.call(this,args);};
G__461.cljs$lang$maxFixedArity = 0;
G__461.cljs$lang$applyTo = (function (arglist__463){
var args = cljs.core.seq(arglist__463);
return G__461__delegate(args);
});
G__461.cljs$core$IFn$_invoke$arity$variadic = G__461__delegate;
return G__461;
})()
;
});
