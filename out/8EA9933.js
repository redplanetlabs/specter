goog.provide('cljs.nodejs');
goog.require('cljs.core');
cljs.nodejs.require = require;
cljs.nodejs.process = process;
cljs.nodejs.enable_util_print_BANG_ = (function cljs$nodejs$enable_util_print_BANG_(){
cljs.core._STAR_print_newline_STAR_ = false;

return cljs.core._STAR_print_fn_STAR_ = (function() { 
var G__707__delegate = function (args){
return console.log.apply(console,cljs.core.into_array.call(null,args));
};
var G__707 = function (var_args){
var args = null;
if (arguments.length > 0) {
var G__708__i = 0, G__708__a = new Array(arguments.length -  0);
while (G__708__i < G__708__a.length) {G__708__a[G__708__i] = arguments[G__708__i + 0]; ++G__708__i;}
  args = new cljs.core.IndexedSeq(G__708__a,0);
} 
return G__707__delegate.call(this,args);};
G__707.cljs$lang$maxFixedArity = 0;
G__707.cljs$lang$applyTo = (function (arglist__709){
var args = cljs.core.seq(arglist__709);
return G__707__delegate(args);
});
G__707.cljs$core$IFn$_invoke$arity$variadic = G__707__delegate;
return G__707;
})()
;
});
