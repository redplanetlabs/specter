goog.provide('cljs.nodejs');
goog.require('cljs.core');
cljs.nodejs.require = require;
cljs.nodejs.process = process;
cljs.nodejs.enable_util_print_BANG_ = (function cljs$nodejs$enable_util_print_BANG_(){
cljs.core._STAR_print_newline_STAR_ = false;

return cljs.core._STAR_print_fn_STAR_ = (function() { 
var G__423__delegate = function (args){
return console.log.apply(console,cljs.core.into_array.call(null,args));
};
var G__423 = function (var_args){
var args = null;
if (arguments.length > 0) {
var G__424__i = 0, G__424__a = new Array(arguments.length -  0);
while (G__424__i < G__424__a.length) {G__424__a[G__424__i] = arguments[G__424__i + 0]; ++G__424__i;}
  args = new cljs.core.IndexedSeq(G__424__a,0);
} 
return G__423__delegate.call(this,args);};
G__423.cljs$lang$maxFixedArity = 0;
G__423.cljs$lang$applyTo = (function (arglist__425){
var args = cljs.core.seq(arglist__425);
return G__423__delegate(args);
});
G__423.cljs$core$IFn$_invoke$arity$variadic = G__423__delegate;
return G__423;
})()
;
});
