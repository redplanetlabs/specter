// Compiled by ClojureScript 0.0-3308 {:target :nodejs}
goog.provide('com.rpl.specter');
goog.require('cljs.core');
goog.require('com.rpl.specter.protocols');
goog.require('com.rpl.specter.impl');
com.rpl.specter.comp_paths = (function com$rpl$specter$comp_paths(){
var argseq__3882__auto__ = ((((0) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(0)),(0))):null);
return com.rpl.specter.comp_paths.cljs$core$IFn$_invoke$arity$variadic(argseq__3882__auto__);
});

com.rpl.specter.comp_paths.cljs$core$IFn$_invoke$arity$variadic = (function (paths){
return com.rpl.specter.protocols.comp_paths_STAR_.call(null,cljs.core.vec.call(null,paths));
});

com.rpl.specter.comp_paths.cljs$lang$maxFixedArity = (0);

com.rpl.specter.comp_paths.cljs$lang$applyTo = (function (seq300){
return com.rpl.specter.comp_paths.cljs$core$IFn$_invoke$arity$variadic(cljs.core.seq.call(null,seq300));
});
/**
 * Version of select that takes in a selector pre-compiled with comp-paths
 */
com.rpl.specter.compiled_select = com.rpl.specter.impl.compiled_select_STAR_;
/**
 * Navigates to and returns a sequence of all the elements specified by the selector.
 */
com.rpl.specter.select = (function com$rpl$specter$select(selector,structure){
return com.rpl.specter.compiled_select.call(null,com.rpl.specter.impl.comp_unoptimal.call(null,selector),structure);
});
/**
 * Version of select-one that takes in a selector pre-compiled with comp-paths
 */
com.rpl.specter.compiled_select_one = (function com$rpl$specter$compiled_select_one(selector,structure){
var res = com.rpl.specter.compiled_select.call(null,selector,structure);
if((cljs.core.count.call(null,res) > (1))){
com.rpl.specter.impl.throw_illegal.call(null,"More than one element found for params: ",selector,structure);
} else {
}

return cljs.core.first.call(null,res);
});
/**
 * Like select, but returns either one element or nil. Throws exception if multiple elements found
 */
com.rpl.specter.select_one = (function com$rpl$specter$select_one(selector,structure){
return com.rpl.specter.compiled_select_one.call(null,com.rpl.specter.impl.comp_unoptimal.call(null,selector),structure);
});
/**
 * Version of select-one! that takes in a selector pre-compiled with comp-paths
 */
com.rpl.specter.compiled_select_one_BANG_ = (function com$rpl$specter$compiled_select_one_BANG_(selector,structure){
var res = com.rpl.specter.compiled_select_one.call(null,selector,structure);
if((res == null)){
com.rpl.specter.impl.throw_illegal.call(null,"No elements found for params: ",selector,structure);
} else {
}

return res;
});
/**
 * Returns exactly one element, throws exception if zero or multiple elements found
 */
com.rpl.specter.select_one_BANG_ = (function com$rpl$specter$select_one_BANG_(selector,structure){
return com.rpl.specter.compiled_select_one_BANG_.call(null,com.rpl.specter.impl.comp_unoptimal.call(null,selector),structure);
});
/**
 * Version of select-first that takes in a selector pre-compiled with comp-paths
 */
com.rpl.specter.compiled_select_first = (function com$rpl$specter$compiled_select_first(selector,structure){
return cljs.core.first.call(null,com.rpl.specter.compiled_select.call(null,selector,structure));
});
/**
 * Returns first element found. Not any more efficient than select, just a convenience
 */
com.rpl.specter.select_first = (function com$rpl$specter$select_first(selector,structure){
return com.rpl.specter.compiled_select_first.call(null,com.rpl.specter.impl.comp_unoptimal.call(null,selector),structure);
});
/**
 * Version of transform that takes in a selector pre-compiled with comp-paths
 */
com.rpl.specter.compiled_transform = com.rpl.specter.impl.compiled_transform_STAR_;
/**
 * Navigates to each value specified by the selector and replaces it by the result of running
 * the transform-fn on it
 */
com.rpl.specter.transform = (function com$rpl$specter$transform(selector,transform_fn,structure){
return com.rpl.specter.compiled_transform.call(null,com.rpl.specter.impl.comp_unoptimal.call(null,selector),transform_fn,structure);
});
/**
 * Version of setval that takes in a selector pre-compiled with comp-paths
 */
com.rpl.specter.compiled_setval = (function com$rpl$specter$compiled_setval(selector,val,structure){
return com.rpl.specter.compiled_transform.call(null,selector,(function (_){
return val;
}),structure);
});
/**
 * Navigates to each value specified by the selector and replaces it by val
 */
com.rpl.specter.setval = (function com$rpl$specter$setval(selector,val,structure){
return com.rpl.specter.compiled_setval.call(null,com.rpl.specter.impl.comp_unoptimal.call(null,selector),val,structure);
});
/**
 * Version of replace-in that takes in a selector pre-compiled with comp-paths
 */
com.rpl.specter.compiled_replace_in = (function com$rpl$specter$compiled_replace_in(){
var argseq__3882__auto__ = ((((3) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(3)),(0))):null);
return com.rpl.specter.compiled_replace_in.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),argseq__3882__auto__);
});

com.rpl.specter.compiled_replace_in.cljs$core$IFn$_invoke$arity$variadic = (function (selector,transform_fn,structure,p__305){
var map__306 = p__305;
var map__306__$1 = ((cljs.core.seq_QMARK_.call(null,map__306))?cljs.core.apply.call(null,cljs.core.hash_map,map__306):map__306);
var merge_fn = cljs.core.get.call(null,map__306__$1,new cljs.core.Keyword(null,"merge-fn","merge-fn",588067341),cljs.core.concat);
var state = com.rpl.specter.impl.mutable_cell.call(null,null);
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [com.rpl.specter.compiled_transform.call(null,selector,((function (state,map__306,map__306__$1,merge_fn){
return (function (e){
var res = transform_fn.call(null,e);
if(cljs.core.truth_(res)){
var vec__307 = res;
var ret = cljs.core.nth.call(null,vec__307,(0),null);
var user_ret = cljs.core.nth.call(null,vec__307,(1),null);
com.rpl.specter.impl.set_cell_BANG_.call(null,state,merge_fn.call(null,com.rpl.specter.impl.get_cell.call(null,state),user_ret));

return ret;
} else {
return e;
}
});})(state,map__306,map__306__$1,merge_fn))
,structure),com.rpl.specter.impl.get_cell.call(null,state)], null);
});

com.rpl.specter.compiled_replace_in.cljs$lang$maxFixedArity = (3);

com.rpl.specter.compiled_replace_in.cljs$lang$applyTo = (function (seq301){
var G__302 = cljs.core.first.call(null,seq301);
var seq301__$1 = cljs.core.next.call(null,seq301);
var G__303 = cljs.core.first.call(null,seq301__$1);
var seq301__$2 = cljs.core.next.call(null,seq301__$1);
var G__304 = cljs.core.first.call(null,seq301__$2);
var seq301__$3 = cljs.core.next.call(null,seq301__$2);
return com.rpl.specter.compiled_replace_in.cljs$core$IFn$_invoke$arity$variadic(G__302,G__303,G__304,seq301__$3);
});
/**
 * Similar to transform, except returns a pair of [transformd-structure sequence-of-user-ret].
 * The transform-fn in this case is expected to return [ret user-ret]. ret is
 * what's used to transform the data structure, while user-ret will be added to the user-ret sequence
 * in the final return. replace-in is useful for situations where you need to know the specific values
 * of what was transformd in the data structure.
 */
com.rpl.specter.replace_in = (function com$rpl$specter$replace_in(){
var argseq__3882__auto__ = ((((3) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(3)),(0))):null);
return com.rpl.specter.replace_in.cljs$core$IFn$_invoke$arity$variadic((arguments[(0)]),(arguments[(1)]),(arguments[(2)]),argseq__3882__auto__);
});

com.rpl.specter.replace_in.cljs$core$IFn$_invoke$arity$variadic = (function (selector,transform_fn,structure,p__312){
var map__313 = p__312;
var map__313__$1 = ((cljs.core.seq_QMARK_.call(null,map__313))?cljs.core.apply.call(null,cljs.core.hash_map,map__313):map__313);
var merge_fn = cljs.core.get.call(null,map__313__$1,new cljs.core.Keyword(null,"merge-fn","merge-fn",588067341),cljs.core.concat);
return com.rpl.specter.compiled_replace_in.call(null,com.rpl.specter.impl.comp_unoptimal.call(null,selector),transform_fn,structure,new cljs.core.Keyword(null,"merge-fn","merge-fn",588067341),merge_fn);
});

com.rpl.specter.replace_in.cljs$lang$maxFixedArity = (3);

com.rpl.specter.replace_in.cljs$lang$applyTo = (function (seq308){
var G__309 = cljs.core.first.call(null,seq308);
var seq308__$1 = cljs.core.next.call(null,seq308);
var G__310 = cljs.core.first.call(null,seq308__$1);
var seq308__$2 = cljs.core.next.call(null,seq308__$1);
var G__311 = cljs.core.first.call(null,seq308__$2);
var seq308__$3 = cljs.core.next.call(null,seq308__$2);
return com.rpl.specter.replace_in.cljs$core$IFn$_invoke$arity$variadic(G__309,G__310,G__311,seq308__$3);
});
com.rpl.specter.ALL = com.rpl.specter.impl.__GT_AllStructurePath.call(null);
com.rpl.specter.VAL = com.rpl.specter.impl.__GT_ValCollect.call(null);
com.rpl.specter.LAST = com.rpl.specter.impl.__GT_LastStructurePath.call(null);
com.rpl.specter.FIRST = com.rpl.specter.impl.__GT_FirstStructurePath.call(null);
com.rpl.specter.srange_dynamic = (function com$rpl$specter$srange_dynamic(start_fn,end_fn){
return com.rpl.specter.impl.__GT_SRangePath.call(null,start_fn,end_fn);
});
com.rpl.specter.srange = (function com$rpl$specter$srange(start,end){
return com.rpl.specter.srange_dynamic.call(null,(function (_){
return start;
}),(function (_){
return end;
}));
});
com.rpl.specter.BEGINNING = com.rpl.specter.srange.call(null,(0),(0));
com.rpl.specter.END = com.rpl.specter.srange_dynamic.call(null,cljs.core.count,cljs.core.count);
com.rpl.specter.walker = (function com$rpl$specter$walker(afn){
return com.rpl.specter.impl.__GT_WalkerStructurePath.call(null,afn);
});
com.rpl.specter.codewalker = (function com$rpl$specter$codewalker(afn){
return com.rpl.specter.impl.__GT_CodeWalkerStructurePath.call(null,afn);
});
com.rpl.specter.filterer = (function com$rpl$specter$filterer(){
var argseq__3882__auto__ = ((((0) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(0)),(0))):null);
return com.rpl.specter.filterer.cljs$core$IFn$_invoke$arity$variadic(argseq__3882__auto__);
});

com.rpl.specter.filterer.cljs$core$IFn$_invoke$arity$variadic = (function (path){
return com.rpl.specter.impl.__GT_FilterStructurePath.call(null,com.rpl.specter.protocols.comp_paths_STAR_.call(null,path));
});

com.rpl.specter.filterer.cljs$lang$maxFixedArity = (0);

com.rpl.specter.filterer.cljs$lang$applyTo = (function (seq314){
return com.rpl.specter.filterer.cljs$core$IFn$_invoke$arity$variadic(cljs.core.seq.call(null,seq314));
});
com.rpl.specter.keypath = (function com$rpl$specter$keypath(akey){
return com.rpl.specter.impl.__GT_KeyPath.call(null,akey);
});
com.rpl.specter.view = (function com$rpl$specter$view(afn){
return com.rpl.specter.impl.__GT_ViewPath.call(null,afn);
});
/**
 * Filters the current value based on whether a selector finds anything.
 * e.g. (selected? :vals ALL even?) keeps the current element only if an
 * even number exists for the :vals key
 */
com.rpl.specter.selected_QMARK_ = (function com$rpl$specter$selected_QMARK_(){
var argseq__3882__auto__ = ((((0) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(0)),(0))):null);
return com.rpl.specter.selected_QMARK_.cljs$core$IFn$_invoke$arity$variadic(argseq__3882__auto__);
});

com.rpl.specter.selected_QMARK_.cljs$core$IFn$_invoke$arity$variadic = (function (selectors){
var s = com.rpl.specter.protocols.comp_paths_STAR_.call(null,selectors);
return ((function (s){
return (function (structure){
return !(cljs.core.empty_QMARK_.call(null,com.rpl.specter.select.call(null,s,structure)));
});
;})(s))
});

com.rpl.specter.selected_QMARK_.cljs$lang$maxFixedArity = (0);

com.rpl.specter.selected_QMARK_.cljs$lang$applyTo = (function (seq315){
return com.rpl.specter.selected_QMARK_.cljs$core$IFn$_invoke$arity$variadic(cljs.core.seq.call(null,seq315));
});
cljs.core.Keyword.prototype.com$rpl$specter$protocols$StructurePath$ = true;

cljs.core.Keyword.prototype.com$rpl$specter$protocols$StructurePath$select_STAR_$arity$3 = (function (kw,structure,next_fn){
var kw__$1 = this;
return next_fn.call(null,cljs.core.get.call(null,structure,kw__$1));
});

cljs.core.Keyword.prototype.com$rpl$specter$protocols$StructurePath$transform_STAR_$arity$3 = (function (kw,structure,next_fn){
var kw__$1 = this;
return cljs.core.assoc.call(null,structure,kw__$1,next_fn.call(null,cljs.core.get.call(null,structure,kw__$1)));
});
Function.prototype.com$rpl$specter$protocols$StructurePath$ = true;

Function.prototype.com$rpl$specter$protocols$StructurePath$select_STAR_$arity$3 = (function (afn,structure,next_fn){
var afn__$1 = this;
if(cljs.core.truth_(afn__$1.call(null,structure))){
return next_fn.call(null,structure);
} else {
return null;
}
});

Function.prototype.com$rpl$specter$protocols$StructurePath$transform_STAR_$arity$3 = (function (afn,structure,next_fn){
var afn__$1 = this;
if(cljs.core.truth_(afn__$1.call(null,structure))){
return next_fn.call(null,structure);
} else {
return structure;
}
});
com.rpl.specter.collect = (function com$rpl$specter$collect(){
var argseq__3882__auto__ = ((((0) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(0)),(0))):null);
return com.rpl.specter.collect.cljs$core$IFn$_invoke$arity$variadic(argseq__3882__auto__);
});

com.rpl.specter.collect.cljs$core$IFn$_invoke$arity$variadic = (function (selector){
return com.rpl.specter.impl.__GT_SelectCollector.call(null,com.rpl.specter.select,com.rpl.specter.protocols.comp_paths_STAR_.call(null,selector));
});

com.rpl.specter.collect.cljs$lang$maxFixedArity = (0);

com.rpl.specter.collect.cljs$lang$applyTo = (function (seq316){
return com.rpl.specter.collect.cljs$core$IFn$_invoke$arity$variadic(cljs.core.seq.call(null,seq316));
});
com.rpl.specter.collect_one = (function com$rpl$specter$collect_one(){
var argseq__3882__auto__ = ((((0) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(0)),(0))):null);
return com.rpl.specter.collect_one.cljs$core$IFn$_invoke$arity$variadic(argseq__3882__auto__);
});

com.rpl.specter.collect_one.cljs$core$IFn$_invoke$arity$variadic = (function (selector){
return com.rpl.specter.impl.__GT_SelectCollector.call(null,com.rpl.specter.select_one,com.rpl.specter.protocols.comp_paths_STAR_.call(null,selector));
});

com.rpl.specter.collect_one.cljs$lang$maxFixedArity = (0);

com.rpl.specter.collect_one.cljs$lang$applyTo = (function (seq317){
return com.rpl.specter.collect_one.cljs$core$IFn$_invoke$arity$variadic(cljs.core.seq.call(null,seq317));
});
/**
 * Adds an external value to the collected vals. Useful when additional arguments
 * are required to the transform function that would otherwise require partial
 * application or a wrapper function.
 * 
 * e.g., incrementing val at path [:a :b] by 3:
 * (transform [:a :b (putval 3)] + some-map)
 */
com.rpl.specter.putval = (function com$rpl$specter$putval(val){
return com.rpl.specter.impl.__GT_PutValCollector.call(null,val);
});
/**
 * Takes in alternating cond-path selector cond-path selector...
 * Tests the structure if selecting with cond-path returns anything.
 * If so, it uses the following selector for this portion of the navigation.
 * Otherwise, it tries the next cond-path. If nothing matches, then the structure
 * is not selected.
 */
com.rpl.specter.cond_path = (function com$rpl$specter$cond_path(){
var argseq__3882__auto__ = ((((0) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(0)),(0))):null);
return com.rpl.specter.cond_path.cljs$core$IFn$_invoke$arity$variadic(argseq__3882__auto__);
});

com.rpl.specter.cond_path.cljs$core$IFn$_invoke$arity$variadic = (function (conds){
return com.rpl.specter.impl.__GT_ConditionalPath.call(null,cljs.core.doall.call(null,cljs.core.map.call(null,(function (p__319){
var vec__320 = p__319;
var c = cljs.core.nth.call(null,vec__320,(0),null);
var p = cljs.core.nth.call(null,vec__320,(1),null);
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [com.rpl.specter.protocols.comp_paths_STAR_.call(null,c),com.rpl.specter.protocols.comp_paths_STAR_.call(null,p)], null);
}),cljs.core.partition.call(null,(2),conds))));
});

com.rpl.specter.cond_path.cljs$lang$maxFixedArity = (0);

com.rpl.specter.cond_path.cljs$lang$applyTo = (function (seq318){
return com.rpl.specter.cond_path.cljs$core$IFn$_invoke$arity$variadic(cljs.core.seq.call(null,seq318));
});
/**
 * Like cond-path, but with if semantics.
 */
com.rpl.specter.if_path = (function com$rpl$specter$if_path(){
var G__322 = arguments.length;
switch (G__322) {
case 2:
return com.rpl.specter.if_path.cljs$core$IFn$_invoke$arity$2((arguments[(0)]),(arguments[(1)]));

break;
case 3:
return com.rpl.specter.if_path.cljs$core$IFn$_invoke$arity$3((arguments[(0)]),(arguments[(1)]),(arguments[(2)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

com.rpl.specter.if_path.cljs$core$IFn$_invoke$arity$2 = (function (cond_fn,if_path){
return com.rpl.specter.cond_path.call(null,cond_fn,if_path);
});

com.rpl.specter.if_path.cljs$core$IFn$_invoke$arity$3 = (function (cond_fn,if_path,else_path){
return com.rpl.specter.cond_path.call(null,cond_fn,if_path,null,else_path);
});

com.rpl.specter.if_path.cljs$lang$maxFixedArity = 3;

//# sourceMappingURL=specter.js.map