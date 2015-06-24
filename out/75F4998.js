goog.provide('cljs.nodejs');
goog.require('cljs.core');
cljs.nodejs.require = require;
cljs.nodejs.process = process;
cljs.nodejs.enable_util_print_BANG_ = (function cljs$nodejs$enable_util_print_BANG_(){
cljs.core._STAR_print_newline_STAR_ = false;

return cljs.core._STAR_print_fn_STAR_ = (function() { 
var G__322__delegate = function (args){
return console.log.apply(console,cljs.core.into_array.call(null,args));
};
var G__322 = function (var_args){
var args = null;
if (arguments.length > 0) {
var G__323__i = 0, G__323__a = new Array(arguments.length -  0);
while (G__323__i < G__323__a.length) {G__323__a[G__323__i] = arguments[G__323__i + 0]; ++G__323__i;}
  args = new cljs.core.IndexedSeq(G__323__a,0);
} 
return G__322__delegate.call(this,args);};
G__322.cljs$lang$maxFixedArity = 0;
G__322.cljs$lang$applyTo = (function (arglist__324){
var args = cljs.core.seq(arglist__324);
return G__322__delegate(args);
});
G__322.cljs$core$IFn$_invoke$arity$variadic = G__322__delegate;
return G__322;
})()
;
});
