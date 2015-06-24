goog.provide('cljs.nodejs');
goog.require('cljs.core');
cljs.nodejs.require = require;
cljs.nodejs.process = process;
cljs.nodejs.enable_util_print_BANG_ = (function cljs$nodejs$enable_util_print_BANG_(){
cljs.core._STAR_print_newline_STAR_ = false;

return cljs.core._STAR_print_fn_STAR_ = (function() { 
var G__1084__delegate = function (args){
return console.log.apply(console,cljs.core.into_array.call(null,args));
};
var G__1084 = function (var_args){
var args = null;
if (arguments.length > 0) {
var G__1085__i = 0, G__1085__a = new Array(arguments.length -  0);
while (G__1085__i < G__1085__a.length) {G__1085__a[G__1085__i] = arguments[G__1085__i + 0]; ++G__1085__i;}
  args = new cljs.core.IndexedSeq(G__1085__a,0);
} 
return G__1084__delegate.call(this,args);};
G__1084.cljs$lang$maxFixedArity = 0;
G__1084.cljs$lang$applyTo = (function (arglist__1086){
var args = cljs.core.seq(arglist__1086);
return G__1084__delegate(args);
});
G__1084.cljs$core$IFn$_invoke$arity$variadic = G__1084__delegate;
return G__1084;
})()
;
});
