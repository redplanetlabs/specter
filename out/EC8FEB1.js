goog.provide('cljs.nodejs');
goog.require('cljs.core');
cljs.nodejs.require = require;
cljs.nodejs.process = process;
cljs.nodejs.enable_util_print_BANG_ = (function cljs$nodejs$enable_util_print_BANG_(){
cljs.core._STAR_print_newline_STAR_ = false;

return cljs.core._STAR_print_fn_STAR_ = (function() { 
var G__684__delegate = function (args){
return console.log.apply(console,cljs.core.into_array.call(null,args));
};
var G__684 = function (var_args){
var args = null;
if (arguments.length > 0) {
var G__685__i = 0, G__685__a = new Array(arguments.length -  0);
while (G__685__i < G__685__a.length) {G__685__a[G__685__i] = arguments[G__685__i + 0]; ++G__685__i;}
  args = new cljs.core.IndexedSeq(G__685__a,0);
} 
return G__684__delegate.call(this,args);};
G__684.cljs$lang$maxFixedArity = 0;
G__684.cljs$lang$applyTo = (function (arglist__686){
var args = cljs.core.seq(arglist__686);
return G__684__delegate(args);
});
G__684.cljs$core$IFn$_invoke$arity$variadic = G__684__delegate;
return G__684;
})()
;
});
