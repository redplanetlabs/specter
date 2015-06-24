goog.provide('cljs.nodejs');
goog.require('cljs.core');
cljs.nodejs.require = require;
cljs.nodejs.process = process;
cljs.nodejs.enable_util_print_BANG_ = (function cljs$nodejs$enable_util_print_BANG_(){
cljs.core._STAR_print_newline_STAR_ = false;

return cljs.core._STAR_print_fn_STAR_ = (function() { 
var G__586__delegate = function (args){
return console.log.apply(console,cljs.core.into_array.call(null,args));
};
var G__586 = function (var_args){
var args = null;
if (arguments.length > 0) {
var G__587__i = 0, G__587__a = new Array(arguments.length -  0);
while (G__587__i < G__587__a.length) {G__587__a[G__587__i] = arguments[G__587__i + 0]; ++G__587__i;}
  args = new cljs.core.IndexedSeq(G__587__a,0);
} 
return G__586__delegate.call(this,args);};
G__586.cljs$lang$maxFixedArity = 0;
G__586.cljs$lang$applyTo = (function (arglist__588){
var args = cljs.core.seq(arglist__588);
return G__586__delegate(args);
});
G__586.cljs$core$IFn$_invoke$arity$variadic = G__586__delegate;
return G__586;
})()
;
});
