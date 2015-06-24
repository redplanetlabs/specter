goog.provide('cljs.nodejs');
goog.require('cljs.core');
cljs.nodejs.require = require;
cljs.nodejs.process = process;
cljs.nodejs.enable_util_print_BANG_ = (function cljs$nodejs$enable_util_print_BANG_(){
cljs.core._STAR_print_newline_STAR_ = false;

return cljs.core._STAR_print_fn_STAR_ = (function() { 
var G__465__delegate = function (args){
return console.log.apply(console,cljs.core.into_array.call(null,args));
};
var G__465 = function (var_args){
var args = null;
if (arguments.length > 0) {
var G__466__i = 0, G__466__a = new Array(arguments.length -  0);
while (G__466__i < G__466__a.length) {G__466__a[G__466__i] = arguments[G__466__i + 0]; ++G__466__i;}
  args = new cljs.core.IndexedSeq(G__466__a,0);
} 
return G__465__delegate.call(this,args);};
G__465.cljs$lang$maxFixedArity = 0;
G__465.cljs$lang$applyTo = (function (arglist__467){
var args = cljs.core.seq(arglist__467);
return G__465__delegate(args);
});
G__465.cljs$core$IFn$_invoke$arity$variadic = G__465__delegate;
return G__465;
})()
;
});
