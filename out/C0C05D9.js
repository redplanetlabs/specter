goog.provide('cljs.nodejs');
goog.require('cljs.core');
cljs.nodejs.require = require;
cljs.nodejs.process = process;
cljs.nodejs.enable_util_print_BANG_ = (function cljs$nodejs$enable_util_print_BANG_(){
cljs.core._STAR_print_newline_STAR_ = false;

return cljs.core._STAR_print_fn_STAR_ = (function() { 
var G__1205__delegate = function (args){
return console.log.apply(console,cljs.core.into_array.call(null,args));
};
var G__1205 = function (var_args){
var args = null;
if (arguments.length > 0) {
var G__1206__i = 0, G__1206__a = new Array(arguments.length -  0);
while (G__1206__i < G__1206__a.length) {G__1206__a[G__1206__i] = arguments[G__1206__i + 0]; ++G__1206__i;}
  args = new cljs.core.IndexedSeq(G__1206__a,0);
} 
return G__1205__delegate.call(this,args);};
G__1205.cljs$lang$maxFixedArity = 0;
G__1205.cljs$lang$applyTo = (function (arglist__1207){
var args = cljs.core.seq(arglist__1207);
return G__1205__delegate(args);
});
G__1205.cljs$core$IFn$_invoke$arity$variadic = G__1205__delegate;
return G__1205;
})()
;
});
