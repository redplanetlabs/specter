goog.provide('cljs.nodejs');
goog.require('cljs.core');
cljs.nodejs.require = require;
cljs.nodejs.process = process;
cljs.nodejs.enable_util_print_BANG_ = (function cljs$nodejs$enable_util_print_BANG_(){
cljs.core._STAR_print_newline_STAR_ = false;

return cljs.core._STAR_print_fn_STAR_ = (function() { 
var G__516__delegate = function (args){
return console.log.apply(console,cljs.core.into_array.call(null,args));
};
var G__516 = function (var_args){
var args = null;
if (arguments.length > 0) {
var G__517__i = 0, G__517__a = new Array(arguments.length -  0);
while (G__517__i < G__517__a.length) {G__517__a[G__517__i] = arguments[G__517__i + 0]; ++G__517__i;}
  args = new cljs.core.IndexedSeq(G__517__a,0);
} 
return G__516__delegate.call(this,args);};
G__516.cljs$lang$maxFixedArity = 0;
G__516.cljs$lang$applyTo = (function (arglist__518){
var args = cljs.core.seq(arglist__518);
return G__516__delegate(args);
});
G__516.cljs$core$IFn$_invoke$arity$variadic = G__516__delegate;
return G__516;
})()
;
});
