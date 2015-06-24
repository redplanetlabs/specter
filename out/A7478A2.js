goog.provide('cljs.nodejs');
goog.require('cljs.core');
cljs.nodejs.require = require;
cljs.nodejs.process = process;
cljs.nodejs.enable_util_print_BANG_ = (function cljs$nodejs$enable_util_print_BANG_(){
cljs.core._STAR_print_newline_STAR_ = false;

return cljs.core._STAR_print_fn_STAR_ = (function() { 
var G__609__delegate = function (args){
return console.log.apply(console,cljs.core.into_array.call(null,args));
};
var G__609 = function (var_args){
var args = null;
if (arguments.length > 0) {
var G__610__i = 0, G__610__a = new Array(arguments.length -  0);
while (G__610__i < G__610__a.length) {G__610__a[G__610__i] = arguments[G__610__i + 0]; ++G__610__i;}
  args = new cljs.core.IndexedSeq(G__610__a,0);
} 
return G__609__delegate.call(this,args);};
G__609.cljs$lang$maxFixedArity = 0;
G__609.cljs$lang$applyTo = (function (arglist__611){
var args = cljs.core.seq(arglist__611);
return G__609__delegate(args);
});
G__609.cljs$core$IFn$_invoke$arity$variadic = G__609__delegate;
return G__609;
})()
;
});
