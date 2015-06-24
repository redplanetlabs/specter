goog.provide('cljs.nodejs');
goog.require('cljs.core');
cljs.nodejs.require = require;
cljs.nodejs.process = process;
cljs.nodejs.enable_util_print_BANG_ = (function cljs$nodejs$enable_util_print_BANG_(){
cljs.core._STAR_print_newline_STAR_ = false;

return cljs.core._STAR_print_fn_STAR_ = (function() { 
var G__1914__delegate = function (args){
return console.log.apply(console,cljs.core.into_array.call(null,args));
};
var G__1914 = function (var_args){
var args = null;
if (arguments.length > 0) {
var G__1915__i = 0, G__1915__a = new Array(arguments.length -  0);
while (G__1915__i < G__1915__a.length) {G__1915__a[G__1915__i] = arguments[G__1915__i + 0]; ++G__1915__i;}
  args = new cljs.core.IndexedSeq(G__1915__a,0);
} 
return G__1914__delegate.call(this,args);};
G__1914.cljs$lang$maxFixedArity = 0;
G__1914.cljs$lang$applyTo = (function (arglist__1916){
var args = cljs.core.seq(arglist__1916);
return G__1914__delegate(args);
});
G__1914.cljs$core$IFn$_invoke$arity$variadic = G__1914__delegate;
return G__1914;
})()
;
});
