goog.provide('cljs.nodejs');
goog.require('cljs.core');
cljs.nodejs.require = require;
cljs.nodejs.process = process;
cljs.nodejs.enable_util_print_BANG_ = (function cljs$nodejs$enable_util_print_BANG_(){
cljs.core._STAR_print_newline_STAR_ = false;

return cljs.core._STAR_print_fn_STAR_ = (function() { 
var G__842__delegate = function (args){
return console.log.apply(console,cljs.core.into_array.call(null,args));
};
var G__842 = function (var_args){
var args = null;
if (arguments.length > 0) {
var G__843__i = 0, G__843__a = new Array(arguments.length -  0);
while (G__843__i < G__843__a.length) {G__843__a[G__843__i] = arguments[G__843__i + 0]; ++G__843__i;}
  args = new cljs.core.IndexedSeq(G__843__a,0);
} 
return G__842__delegate.call(this,args);};
G__842.cljs$lang$maxFixedArity = 0;
G__842.cljs$lang$applyTo = (function (arglist__844){
var args = cljs.core.seq(arglist__844);
return G__842__delegate(args);
});
G__842.cljs$core$IFn$_invoke$arity$variadic = G__842__delegate;
return G__842;
})()
;
});
