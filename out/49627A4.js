goog.provide('cljs.nodejs');
goog.require('cljs.core');
cljs.nodejs.require = require;
cljs.nodejs.process = process;
cljs.nodejs.enable_util_print_BANG_ = (function cljs$nodejs$enable_util_print_BANG_(){
cljs.core._STAR_print_newline_STAR_ = false;

return cljs.core._STAR_print_fn_STAR_ = (function() { 
var G__424__delegate = function (args){
return console.log.apply(console,cljs.core.into_array.call(null,args));
};
var G__424 = function (var_args){
var args = null;
if (arguments.length > 0) {
var G__425__i = 0, G__425__a = new Array(arguments.length -  0);
while (G__425__i < G__425__a.length) {G__425__a[G__425__i] = arguments[G__425__i + 0]; ++G__425__i;}
  args = new cljs.core.IndexedSeq(G__425__a,0);
} 
return G__424__delegate.call(this,args);};
G__424.cljs$lang$maxFixedArity = 0;
G__424.cljs$lang$applyTo = (function (arglist__426){
var args = cljs.core.seq(arglist__426);
return G__424__delegate(args);
});
G__424.cljs$core$IFn$_invoke$arity$variadic = G__424__delegate;
return G__424;
})()
;
});
