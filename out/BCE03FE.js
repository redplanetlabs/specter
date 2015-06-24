goog.provide('cljs.nodejscli');
goog.require('cljs.core');
goog.require('cljs.nodejs');
if(COMPILED){
goog.global = global;
} else {
}
if(((cljs.core._STAR_main_cli_fn_STAR_ == null)) || (!(cljs.core.fn_QMARK_.call(null,cljs.core._STAR_main_cli_fn_STAR_)))){
throw (new Error("cljs.core/*main-cli-fn* not set"));
} else {
cljs.core.apply.call(null,cljs.core._STAR_main_cli_fn_STAR_,cljs.core.drop.call(null,(2),cljs.nodejs.process.argv));
}
