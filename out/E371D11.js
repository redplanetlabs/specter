goog.provide('cljs.nodejs');
goog.require('cljs.core');
cljs.nodejs.require = require;
cljs.nodejs.process = process;
cljs.nodejs.enable_util_print_BANG_ = (function cljs$nodejs$enable_util_print_BANG_(){
cljs.core._STAR_print_newline_STAR_ = false;

return cljs.core._STAR_print_fn_STAR_ = (function() { 
var G__243__delegate = function (args){
return console.log.apply(console,cljs.core.into_array.call(null,args));
};
var G__243 = function (var_args){
var args = null;
if (arguments.length > 0) {
var G__244__i = 0, G__244__a = new Array(arguments.length -  0);
while (G__244__i < G__244__a.length) {G__244__a[G__244__i] = arguments[G__244__i + 0]; ++G__244__i;}
  args = new cljs.core.IndexedSeq(G__244__a,0);
} 
return G__243__delegate.call(this,args);};
G__243.cljs$lang$maxFixedArity = 0;
G__243.cljs$lang$applyTo = (function (arglist__245){
var args = cljs.core.seq(arglist__245);
return G__243__delegate(args);
});
G__243.cljs$core$IFn$_invoke$arity$variadic = G__243__delegate;
return G__243;
})()
;
});
