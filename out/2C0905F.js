goog.provide('cljs.nodejs');
goog.require('cljs.core');
cljs.nodejs.require = require;
cljs.nodejs.process = process;
cljs.nodejs.enable_util_print_BANG_ = (function cljs$nodejs$enable_util_print_BANG_(){
cljs.core._STAR_print_newline_STAR_ = false;

return cljs.core._STAR_print_fn_STAR_ = (function() { 
var G__473__delegate = function (args){
return console.log.apply(console,cljs.core.into_array.call(null,args));
};
var G__473 = function (var_args){
var args = null;
if (arguments.length > 0) {
var G__474__i = 0, G__474__a = new Array(arguments.length -  0);
while (G__474__i < G__474__a.length) {G__474__a[G__474__i] = arguments[G__474__i + 0]; ++G__474__i;}
  args = new cljs.core.IndexedSeq(G__474__a,0);
} 
return G__473__delegate.call(this,args);};
G__473.cljs$lang$maxFixedArity = 0;
G__473.cljs$lang$applyTo = (function (arglist__475){
var args = cljs.core.seq(arglist__475);
return G__473__delegate(args);
});
G__473.cljs$core$IFn$_invoke$arity$variadic = G__473__delegate;
return G__473;
})()
;
});
