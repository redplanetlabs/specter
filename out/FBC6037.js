goog.provide('cljs.nodejs');
goog.require('cljs.core');
cljs.nodejs.require = require;
cljs.nodejs.process = process;
cljs.nodejs.enable_util_print_BANG_ = (function cljs$nodejs$enable_util_print_BANG_(){
cljs.core._STAR_print_newline_STAR_ = false;

return cljs.core._STAR_print_fn_STAR_ = (function() { 
var G__366__delegate = function (args){
return console.log.apply(console,cljs.core.into_array.call(null,args));
};
var G__366 = function (var_args){
var args = null;
if (arguments.length > 0) {
var G__367__i = 0, G__367__a = new Array(arguments.length -  0);
while (G__367__i < G__367__a.length) {G__367__a[G__367__i] = arguments[G__367__i + 0]; ++G__367__i;}
  args = new cljs.core.IndexedSeq(G__367__a,0);
} 
return G__366__delegate.call(this,args);};
G__366.cljs$lang$maxFixedArity = 0;
G__366.cljs$lang$applyTo = (function (arglist__368){
var args = cljs.core.seq(arglist__368);
return G__366__delegate(args);
});
G__366.cljs$core$IFn$_invoke$arity$variadic = G__366__delegate;
return G__366;
})()
;
});
