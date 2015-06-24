goog.provide('cljs.nodejs');
goog.require('cljs.core');
cljs.nodejs.require = require;
cljs.nodejs.process = process;
cljs.nodejs.enable_util_print_BANG_ = (function cljs$nodejs$enable_util_print_BANG_(){
cljs.core._STAR_print_newline_STAR_ = false;

return cljs.core._STAR_print_fn_STAR_ = (function() { 
var G__326__delegate = function (args){
return console.log.apply(console,cljs.core.into_array.call(null,args));
};
var G__326 = function (var_args){
var args = null;
if (arguments.length > 0) {
var G__327__i = 0, G__327__a = new Array(arguments.length -  0);
while (G__327__i < G__327__a.length) {G__327__a[G__327__i] = arguments[G__327__i + 0]; ++G__327__i;}
  args = new cljs.core.IndexedSeq(G__327__a,0);
} 
return G__326__delegate.call(this,args);};
G__326.cljs$lang$maxFixedArity = 0;
G__326.cljs$lang$applyTo = (function (arglist__328){
var args = cljs.core.seq(arglist__328);
return G__326__delegate(args);
});
G__326.cljs$core$IFn$_invoke$arity$variadic = G__326__delegate;
return G__326;
})()
;
});
