goog.provide('cljs.nodejs');
goog.require('cljs.core');
cljs.nodejs.require = require;
cljs.nodejs.process = process;
cljs.nodejs.enable_util_print_BANG_ = (function cljs$nodejs$enable_util_print_BANG_(){
cljs.core._STAR_print_newline_STAR_ = false;

return cljs.core._STAR_print_fn_STAR_ = (function() { 
var G__963__delegate = function (args){
return console.log.apply(console,cljs.core.into_array.call(null,args));
};
var G__963 = function (var_args){
var args = null;
if (arguments.length > 0) {
var G__964__i = 0, G__964__a = new Array(arguments.length -  0);
while (G__964__i < G__964__a.length) {G__964__a[G__964__i] = arguments[G__964__i + 0]; ++G__964__i;}
  args = new cljs.core.IndexedSeq(G__964__a,0);
} 
return G__963__delegate.call(this,args);};
G__963.cljs$lang$maxFixedArity = 0;
G__963.cljs$lang$applyTo = (function (arglist__965){
var args = cljs.core.seq(arglist__965);
return G__963__delegate(args);
});
G__963.cljs$core$IFn$_invoke$arity$variadic = G__963__delegate;
return G__963;
})()
;
});
