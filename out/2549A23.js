goog.provide('cljs.nodejs');
goog.require('cljs.core');
cljs.nodejs.require = require;
cljs.nodejs.process = process;
cljs.nodejs.enable_util_print_BANG_ = (function cljs$nodejs$enable_util_print_BANG_(){
cljs.core._STAR_print_newline_STAR_ = false;

return cljs.core._STAR_print_fn_STAR_ = (function() { 
var G__320__delegate = function (args){
return console.log.apply(console,cljs.core.into_array.call(null,args));
};
var G__320 = function (var_args){
var args = null;
if (arguments.length > 0) {
var G__321__i = 0, G__321__a = new Array(arguments.length -  0);
while (G__321__i < G__321__a.length) {G__321__a[G__321__i] = arguments[G__321__i + 0]; ++G__321__i;}
  args = new cljs.core.IndexedSeq(G__321__a,0);
} 
return G__320__delegate.call(this,args);};
G__320.cljs$lang$maxFixedArity = 0;
G__320.cljs$lang$applyTo = (function (arglist__322){
var args = cljs.core.seq(arglist__322);
return G__320__delegate(args);
});
G__320.cljs$core$IFn$_invoke$arity$variadic = G__320__delegate;
return G__320;
})()
;
});
