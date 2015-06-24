goog.provide('cljs.nodejs');
goog.require('cljs.core');
cljs.nodejs.require = require;
cljs.nodejs.process = process;
cljs.nodejs.enable_util_print_BANG_ = (function cljs$nodejs$enable_util_print_BANG_(){
cljs.core._STAR_print_newline_STAR_ = false;

return cljs.core._STAR_print_fn_STAR_ = (function() { 
var G__519__delegate = function (args){
return console.log.apply(console,cljs.core.into_array.call(null,args));
};
var G__519 = function (var_args){
var args = null;
if (arguments.length > 0) {
var G__520__i = 0, G__520__a = new Array(arguments.length -  0);
while (G__520__i < G__520__a.length) {G__520__a[G__520__i] = arguments[G__520__i + 0]; ++G__520__i;}
  args = new cljs.core.IndexedSeq(G__520__a,0);
} 
return G__519__delegate.call(this,args);};
G__519.cljs$lang$maxFixedArity = 0;
G__519.cljs$lang$applyTo = (function (arglist__521){
var args = cljs.core.seq(arglist__521);
return G__519__delegate(args);
});
G__519.cljs$core$IFn$_invoke$arity$variadic = G__519__delegate;
return G__519;
})()
;
});
