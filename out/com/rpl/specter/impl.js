// Compiled by ClojureScript 0.0-3308 {:target :nodejs}
goog.provide('com.rpl.specter.impl');
goog.require('cljs.core');
goog.require('com.rpl.specter.protocols');
goog.require('clojure.walk');
goog.require('clojure.core.reducers');
com.rpl.specter.impl.throw_illegal = (function com$rpl$specter$impl$throw_illegal(){
var argseq__3882__auto__ = ((((0) < arguments.length))?(new cljs.core.IndexedSeq(Array.prototype.slice.call(arguments,(0)),(0))):null);
return com.rpl.specter.impl.throw_illegal.cljs$core$IFn$_invoke$arity$variadic(argseq__3882__auto__);
});

com.rpl.specter.impl.throw_illegal.cljs$core$IFn$_invoke$arity$variadic = (function (args){
throw (new Error(cljs.core.apply.call(null,cljs.core.str,args)));
});

com.rpl.specter.impl.throw_illegal.cljs$lang$maxFixedArity = (0);

com.rpl.specter.impl.throw_illegal.cljs$lang$applyTo = (function (seq272){
return com.rpl.specter.impl.throw_illegal.cljs$core$IFn$_invoke$arity$variadic(cljs.core.seq.call(null,seq272));
});
com.rpl.specter.impl.benchmark = (function com$rpl$specter$impl$benchmark(iters,afn){
var start__3804__auto__ = (new Date()).getTime();
var ret__3805__auto__ = (function (){var n__3791__auto__ = iters;
var _ = (0);
while(true){
if((_ < n__3791__auto__)){
afn.call(null);

var G__273 = (_ + (1));
_ = G__273;
continue;
} else {
return null;
}
break;
}
})();
cljs.core.prn.call(null,[cljs.core.str("Elapsed time: "),cljs.core.str(((new Date()).getTime() - start__3804__auto__)),cljs.core.str(" msecs")].join(''));

return ret__3805__auto__;
});

/**
* @constructor
*/
com.rpl.specter.impl.ExecutorFunctions = (function (type,select_executor,transform_executor){
this.type = type;
this.select_executor = select_executor;
this.transform_executor = transform_executor;
})

com.rpl.specter.impl.ExecutorFunctions.getBasis = (function (){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"type","type",-1480165421,null),new cljs.core.Symbol(null,"select-executor","select-executor",140452237,null),new cljs.core.Symbol(null,"transform-executor","transform-executor",-31221519,null)], null);
});

com.rpl.specter.impl.ExecutorFunctions.cljs$lang$type = true;

com.rpl.specter.impl.ExecutorFunctions.cljs$lang$ctorStr = "com.rpl.specter.impl/ExecutorFunctions";

com.rpl.specter.impl.ExecutorFunctions.cljs$lang$ctorPrWriter = (function (this__3585__auto__,writer__3586__auto__,opt__3587__auto__){
return cljs.core._write.call(null,writer__3586__auto__,"com.rpl.specter.impl/ExecutorFunctions");
});

com.rpl.specter.impl.__GT_ExecutorFunctions = (function com$rpl$specter$impl$__GT_ExecutorFunctions(type,select_executor,transform_executor){
return (new com.rpl.specter.impl.ExecutorFunctions(type,select_executor,transform_executor));
});

com.rpl.specter.impl.StructureValsPathExecutor = com.rpl.specter.impl.__GT_ExecutorFunctions.call(null,new cljs.core.Keyword(null,"svalspath","svalspath",-2129986746),(function (selector,structure){
return selector.call(null,cljs.core.PersistentVector.EMPTY,structure,(function (vals,structure__$1){
if(!(cljs.core.empty_QMARK_.call(null,vals))){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.conj.call(null,vals,structure__$1)], null);
} else {
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [structure__$1], null);
}
}));
}),(function (transformer,transform_fn,structure){
return transformer.call(null,cljs.core.PersistentVector.EMPTY,structure,(function (vals,structure__$1){
if(cljs.core.empty_QMARK_.call(null,vals)){
return transform_fn.call(null,structure__$1);
} else {
return cljs.core.apply.call(null,transform_fn,cljs.core.conj.call(null,vals,structure__$1));
}
}));
}));
com.rpl.specter.impl.StructurePathExecutor = com.rpl.specter.impl.__GT_ExecutorFunctions.call(null,new cljs.core.Keyword(null,"spath","spath",-1857758005),(function (selector,structure){
return selector.call(null,structure,(function (structure__$1){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [structure__$1], null);
}));
}),(function (transformer,transform_fn,structure){
return transformer.call(null,structure,transform_fn);
}));

/**
* @constructor
*/
com.rpl.specter.impl.TransformFunctions = (function (executors,selector,transformer){
this.executors = executors;
this.selector = selector;
this.transformer = transformer;
})

com.rpl.specter.impl.TransformFunctions.getBasis = (function (){
return new cljs.core.PersistentVector(null, 3, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"executors","executors",1309458124,null),new cljs.core.Symbol(null,"selector","selector",-1891906903,null),new cljs.core.Symbol(null,"transformer","transformer",147060907,null)], null);
});

com.rpl.specter.impl.TransformFunctions.cljs$lang$type = true;

com.rpl.specter.impl.TransformFunctions.cljs$lang$ctorStr = "com.rpl.specter.impl/TransformFunctions";

com.rpl.specter.impl.TransformFunctions.cljs$lang$ctorPrWriter = (function (this__3585__auto__,writer__3586__auto__,opt__3587__auto__){
return cljs.core._write.call(null,writer__3586__auto__,"com.rpl.specter.impl/TransformFunctions");
});

com.rpl.specter.impl.__GT_TransformFunctions = (function com$rpl$specter$impl$__GT_TransformFunctions(executors,selector,transformer){
return (new com.rpl.specter.impl.TransformFunctions(executors,selector,transformer));
});


com.rpl.specter.impl.CoerceTransformFunctions = (function (){var obj275 = {};
return obj275;
})();

com.rpl.specter.impl.coerce_path = (function com$rpl$specter$impl$coerce_path(this$){
if((function (){var and__3362__auto__ = this$;
if(and__3362__auto__){
return this$.com$rpl$specter$impl$CoerceTransformFunctions$coerce_path$arity$1;
} else {
return and__3362__auto__;
}
})()){
return this$.com$rpl$specter$impl$CoerceTransformFunctions$coerce_path$arity$1(this$);
} else {
var x__3634__auto__ = (((this$ == null))?null:this$);
return (function (){var or__3370__auto__ = (com.rpl.specter.impl.coerce_path[goog.typeOf(x__3634__auto__)]);
if(or__3370__auto__){
return or__3370__auto__;
} else {
var or__3370__auto____$1 = (com.rpl.specter.impl.coerce_path["_"]);
if(or__3370__auto____$1){
return or__3370__auto____$1;
} else {
throw cljs.core.missing_protocol.call(null,"CoerceTransformFunctions.coerce-path",this$);
}
}
})().call(null,this$);
}
});

com.rpl.specter.impl.no_prot_error_str = (function com$rpl$specter$impl$no_prot_error_str(obj){
return [cljs.core.str("Protocol implementation cannot be found for object.\n        Extending Specter protocols should not be done inline in a deftype definition\n        because that prevents Specter from finding the protocol implementations for\n        optimized performance. Instead, you should extend the protocols via an\n        explicit extend-protocol call. \n"),cljs.core.str(obj)].join('');
});
com.rpl.specter.impl.find_protocol_impl_BANG_ = (function com$rpl$specter$impl$find_protocol_impl_BANG_(prot,obj){
var ret = com.rpl.specter.impl.find_protocol_impl.call(null,prot,obj);
if(cljs.core._EQ_.call(null,ret,obj)){
return com.rpl.specter.impl.throw_illegal.call(null,com.rpl.specter.impl.no_prot_error_str.call(null,obj));
} else {
return ret;
}
});
com.rpl.specter.impl.coerce_structure_vals_path = (function com$rpl$specter$impl$coerce_structure_vals_path(this$){
var pimpl = com.rpl.specter.impl.find_protocol_impl_BANG_.call(null,com.rpl.specter.protocols.StructureValsPath,this$);
var selector = new cljs.core.Keyword(null,"select-full*","select-full*",-101641297).cljs$core$IFn$_invoke$arity$1(pimpl);
var transformer = new cljs.core.Keyword(null,"transform-full*","transform-full*",-616664586).cljs$core$IFn$_invoke$arity$1(pimpl);
return com.rpl.specter.impl.__GT_TransformFunctions.call(null,com.rpl.specter.impl.StructureValsPathExecutor,((function (pimpl,selector,transformer){
return (function (vals,structure,next_fn){
return selector.call(null,this$,vals,structure,next_fn);
});})(pimpl,selector,transformer))
,((function (pimpl,selector,transformer){
return (function (vals,structure,next_fn){
return transformer.call(null,this$,vals,structure,next_fn);
});})(pimpl,selector,transformer))
);
});
com.rpl.specter.impl.coerce_collector = (function com$rpl$specter$impl$coerce_collector(this$){
var cfn = new cljs.core.Keyword(null,"collect-val","collect-val",801894069).cljs$core$IFn$_invoke$arity$1(com.rpl.specter.impl.find_protocol_impl_BANG_.call(null,com.rpl.specter.protocols.Collector,this$));
var afn = ((function (cfn){
return (function (vals,structure,next_fn){
return next_fn.call(null,cljs.core.conj.call(null,vals,cfn.call(null,this$,structure)),structure);
});})(cfn))
;
return com.rpl.specter.impl.__GT_TransformFunctions.call(null,com.rpl.specter.impl.StructureValsPathExecutor,afn,afn);
});
com.rpl.specter.impl.structure_path_impl = (function com$rpl$specter$impl$structure_path_impl(this$){
if(cljs.core.fn_QMARK_.call(null,this$)){
return cljs.core.get.call(null,new cljs.core.Keyword(null,"impls","impls",-1314014853).cljs$core$IFn$_invoke$arity$1(com.rpl.specter.protocols.StructurePath),clojure.lang.AFn);
} else {
return com.rpl.specter.impl.find_protocol_impl_BANG_.call(null,com.rpl.specter.protocols.StructurePath,this$);
}
});
com.rpl.specter.impl.coerce_structure_path = (function com$rpl$specter$impl$coerce_structure_path(this$){
var pimpl = com.rpl.specter.impl.structure_path_impl.call(null,this$);
var selector = new cljs.core.Keyword(null,"select*","select*",-1829914060).cljs$core$IFn$_invoke$arity$1(pimpl);
var transformer = new cljs.core.Keyword(null,"transform*","transform*",-1613794522).cljs$core$IFn$_invoke$arity$1(pimpl);
return com.rpl.specter.impl.__GT_TransformFunctions.call(null,com.rpl.specter.impl.StructurePathExecutor,((function (pimpl,selector,transformer){
return (function (structure,next_fn){
return selector.call(null,this$,structure,next_fn);
});})(pimpl,selector,transformer))
,((function (pimpl,selector,transformer){
return (function (structure,next_fn){
return transformer.call(null,this$,structure,next_fn);
});})(pimpl,selector,transformer))
);
});
com.rpl.specter.impl.coerce_structure_path_direct = (function com$rpl$specter$impl$coerce_structure_path_direct(this$){
var pimpl = com.rpl.specter.impl.structure_path_impl.call(null,this$);
var selector = new cljs.core.Keyword(null,"select*","select*",-1829914060).cljs$core$IFn$_invoke$arity$1(pimpl);
var transformer = new cljs.core.Keyword(null,"transform*","transform*",-1613794522).cljs$core$IFn$_invoke$arity$1(pimpl);
return com.rpl.specter.impl.__GT_TransformFunctions.call(null,com.rpl.specter.impl.StructureValsPathExecutor,((function (pimpl,selector,transformer){
return (function (vals,structure,next_fn){
return selector.call(null,this$,structure,((function (pimpl,selector,transformer){
return (function (structure__$1){
return next_fn.call(null,vals,structure__$1);
});})(pimpl,selector,transformer))
);
});})(pimpl,selector,transformer))
,((function (pimpl,selector,transformer){
return (function (vals,structure,next_fn){
return transformer.call(null,this$,structure,((function (pimpl,selector,transformer){
return (function (structure__$1){
return next_fn.call(null,vals,structure__$1);
});})(pimpl,selector,transformer))
);
});})(pimpl,selector,transformer))
);
});
com.rpl.specter.impl.obj_extends_QMARK_ = (function com$rpl$specter$impl$obj_extends_QMARK_(prot,obj){
return !((com.rpl.specter.impl.find_protocol_impl.call(null,prot,obj) == null));
});
com.rpl.specter.impl.structure_path_QMARK_ = (function com$rpl$specter$impl$structure_path_QMARK_(obj){
var or__3370__auto__ = cljs.core.fn_QMARK_.call(null,obj);
if(or__3370__auto__){
return or__3370__auto__;
} else {
return com.rpl.specter.impl.obj_extends_QMARK_.call(null,com.rpl.specter.protocols.StructurePath,obj);
}
});
(com.rpl.specter.impl.CoerceTransformFunctions["null"] = true);

(com.rpl.specter.impl.coerce_path["null"] = (function (this$){
return com.rpl.specter.impl.coerce_structure_path.call(null,null);
}));

com.rpl.specter.impl.TransformFunctions.prototype.com$rpl$specter$impl$CoerceTransformFunctions$ = true;

com.rpl.specter.impl.TransformFunctions.prototype.com$rpl$specter$impl$CoerceTransformFunctions$coerce_path$arity$1 = (function (this$){
var this$__$1 = this;
return this$__$1;
});

cljs.core.PersistentVector.prototype.com$rpl$specter$impl$CoerceTransformFunctions$ = true;

cljs.core.PersistentVector.prototype.com$rpl$specter$impl$CoerceTransformFunctions$coerce_path$arity$1 = (function (this$){
var this$__$1 = this;
return com.rpl.specter.protocols.comp_paths_STAR_.call(null,this$__$1);
});

Object.prototype.com$rpl$specter$impl$CoerceTransformFunctions$ = true;

Object.prototype.com$rpl$specter$impl$CoerceTransformFunctions$coerce_path$arity$1 = (function (this$){
var this$__$1 = this;
if(cljs.core.truth_(com.rpl.specter.impl.structure_path_QMARK_.call(null,this$__$1))){
return com.rpl.specter.impl.coerce_structure_path.call(null,this$__$1);
} else {
if(cljs.core.truth_(com.rpl.specter.impl.obj_extends_QMARK_.call(null,com.rpl.specter.protocols.Collector,this$__$1))){
return com.rpl.specter.impl.coerce_collector.call(null,this$__$1);
} else {
if(cljs.core.truth_(com.rpl.specter.impl.obj_extends_QMARK_.call(null,com.rpl.specter.protocols.StructureValsPath,this$__$1))){
return com.rpl.specter.impl.coerce_structure_vals_path.call(null,this$__$1);
} else {
return com.rpl.specter.impl.throw_illegal.call(null,com.rpl.specter.impl.no_prot_error_str.call(null,this$__$1));

}
}
}
});
com.rpl.specter.impl.extype = (function com$rpl$specter$impl$extype(f){
var exs = f.executors();
return exs.type();
});
com.rpl.specter.impl.combine_same_types = (function com$rpl$specter$impl$combine_same_types(p__276){
var vec__278 = p__276;
var f = cljs.core.nth.call(null,vec__278,(0),null);
var _ = cljs.core.nthnext.call(null,vec__278,(1));
var all = vec__278;
if(cljs.core.empty_QMARK_.call(null,all)){
return com.rpl.specter.impl.coerce_path.call(null,null);
} else {
var exs = f.executors();
var t = exs.type();
var combiner = ((cljs.core._EQ_.call(null,t,new cljs.core.Keyword(null,"svalspath","svalspath",-2129986746)))?((function (exs,t,vec__278,f,_,all){
return (function (curr,next){
return ((function (exs,t,vec__278,f,_,all){
return (function (vals,structure,next_fn){
return curr.call(null,vals,structure,((function (exs,t,vec__278,f,_,all){
return (function (vals_next,structure_next){
return next.call(null,vals_next,structure_next,next_fn);
});})(exs,t,vec__278,f,_,all))
);
});
;})(exs,t,vec__278,f,_,all))
});})(exs,t,vec__278,f,_,all))
:((function (exs,t,vec__278,f,_,all){
return (function (curr,next){
return ((function (exs,t,vec__278,f,_,all){
return (function (structure,next_fn){
return curr.call(null,structure,((function (exs,t,vec__278,f,_,all){
return (function (structure__$1){
return next.call(null,structure__$1,next_fn);
});})(exs,t,vec__278,f,_,all))
);
});
;})(exs,t,vec__278,f,_,all))
});})(exs,t,vec__278,f,_,all))
);
return cljs.core.reduce.call(null,((function (exs,t,combiner,vec__278,f,_,all){
return (function (curr,next){
return com.rpl.specter.impl.__GT_TransformFunctions.call(null,exs,combiner.call(null,curr.selector(),next.selector()),combiner.call(null,curr.transformer(),next.transformer()));
});})(exs,t,combiner,vec__278,f,_,all))
,all);
}
});
com.rpl.specter.impl.coerce_structure_vals = (function com$rpl$specter$impl$coerce_structure_vals(tfns){
if(cljs.core._EQ_.call(null,com.rpl.specter.impl.extype.call(null,tfns),new cljs.core.Keyword(null,"svalspath","svalspath",-2129986746))){
return tfns;
} else {
var selector = tfns.selector();
var transformer = tfns.transformer();
return com.rpl.specter.impl.__GT_TransformFunctions.call(null,com.rpl.specter.impl.StructureValsPathExecutor,((function (selector,transformer){
return (function (vals,structure,next_fn){
return selector.call(null,structure,((function (selector,transformer){
return (function (structure__$1){
return next_fn.call(null,vals,structure__$1);
});})(selector,transformer))
);
});})(selector,transformer))
,((function (selector,transformer){
return (function (vals,structure,next_fn){
return transformer.call(null,structure,((function (selector,transformer){
return (function (structure__$1){
return next_fn.call(null,vals,structure__$1);
});})(selector,transformer))
);
});})(selector,transformer))
);
}
});
(com.rpl.specter.protocols.StructureValsPathComposer["null"] = true);

(com.rpl.specter.protocols.comp_paths_STAR_["null"] = (function (sp){
return com.rpl.specter.impl.coerce_path.call(null,sp);
}));

Object.prototype.com$rpl$specter$protocols$StructureValsPathComposer$ = true;

Object.prototype.com$rpl$specter$protocols$StructureValsPathComposer$comp_paths_STAR_$arity$1 = (function (sp){
var sp__$1 = this;
return com.rpl.specter.impl.coerce_path.call(null,sp__$1);
});

cljs.core.PersistentVector.prototype.com$rpl$specter$protocols$StructureValsPathComposer$ = true;

cljs.core.PersistentVector.prototype.com$rpl$specter$protocols$StructureValsPathComposer$comp_paths_STAR_$arity$1 = (function (structure_paths){
var structure_paths__$1 = this;
var combined = cljs.core.map.call(null,com.rpl.specter.impl.combine_same_types,cljs.core.partition_by.call(null,com.rpl.specter.impl.extype,cljs.core.map.call(null,com.rpl.specter.impl.coerce_path,structure_paths__$1)));
if(cljs.core._EQ_.call(null,(1),cljs.core.count.call(null,combined))){
return cljs.core.first.call(null,combined);
} else {
return com.rpl.specter.impl.combine_same_types.call(null,cljs.core.map.call(null,com.rpl.specter.impl.coerce_structure_vals,combined));
}
});
com.rpl.specter.impl.coerce_structure_vals_direct = (function com$rpl$specter$impl$coerce_structure_vals_direct(this$){
if(cljs.core.truth_(com.rpl.specter.impl.structure_path_QMARK_.call(null,this$))){
return com.rpl.specter.impl.coerce_structure_path_direct.call(null,this$);
} else {
if(cljs.core.truth_(com.rpl.specter.impl.obj_extends_QMARK_.call(null,com.rpl.specter.protocols.Collector,this$))){
return com.rpl.specter.impl.coerce_collector.call(null,this$);
} else {
if(cljs.core.truth_(com.rpl.specter.impl.obj_extends_QMARK_.call(null,com.rpl.specter.protocols.StructureValsPath,this$))){
return com.rpl.specter.impl.coerce_structure_vals_path.call(null,this$);
} else {
if((this$ instanceof com.rpl.specter.impl.TransformFunctions)){
return com.rpl.specter.impl.coerce_structure_vals.call(null,this$);
} else {
return com.rpl.specter.impl.throw_illegal.call(null,com.rpl.specter.impl.no_prot_error_str.call(null,this$));

}
}
}
}
});
com.rpl.specter.impl.comp_unoptimal = (function com$rpl$specter$impl$comp_unoptimal(sp){
if((sp instanceof cljs.core.PersistentVector)){
return com.rpl.specter.impl.combine_same_types.call(null,cljs.core.map.call(null,com.rpl.specter.impl.coerce_structure_vals_direct,sp));
} else {
return com.rpl.specter.impl.coerce_path.call(null,sp);
}
});

com.rpl.specter.impl.PMutableCell = (function (){var obj280 = {};
return obj280;
})();

com.rpl.specter.impl.get_cell = (function com$rpl$specter$impl$get_cell(cell){
if((function (){var and__3362__auto__ = cell;
if(and__3362__auto__){
return cell.com$rpl$specter$impl$PMutableCell$get_cell$arity$1;
} else {
return and__3362__auto__;
}
})()){
return cell.com$rpl$specter$impl$PMutableCell$get_cell$arity$1(cell);
} else {
var x__3634__auto__ = (((cell == null))?null:cell);
return (function (){var or__3370__auto__ = (com.rpl.specter.impl.get_cell[goog.typeOf(x__3634__auto__)]);
if(or__3370__auto__){
return or__3370__auto__;
} else {
var or__3370__auto____$1 = (com.rpl.specter.impl.get_cell["_"]);
if(or__3370__auto____$1){
return or__3370__auto____$1;
} else {
throw cljs.core.missing_protocol.call(null,"PMutableCell.get_cell",cell);
}
}
})().call(null,cell);
}
});

com.rpl.specter.impl.set_cell = (function com$rpl$specter$impl$set_cell(cell,x){
if((function (){var and__3362__auto__ = cell;
if(and__3362__auto__){
return cell.com$rpl$specter$impl$PMutableCell$set_cell$arity$2;
} else {
return and__3362__auto__;
}
})()){
return cell.com$rpl$specter$impl$PMutableCell$set_cell$arity$2(cell,x);
} else {
var x__3634__auto__ = (((cell == null))?null:cell);
return (function (){var or__3370__auto__ = (com.rpl.specter.impl.set_cell[goog.typeOf(x__3634__auto__)]);
if(or__3370__auto__){
return or__3370__auto__;
} else {
var or__3370__auto____$1 = (com.rpl.specter.impl.set_cell["_"]);
if(or__3370__auto____$1){
return or__3370__auto____$1;
} else {
throw cljs.core.missing_protocol.call(null,"PMutableCell.set_cell",cell);
}
}
})().call(null,cell,x);
}
});


/**
* @constructor
*/
com.rpl.specter.impl.MutableCell = (function (q){
this.q = q;
})
com.rpl.specter.impl.MutableCell.prototype.com$rpl$specter$impl$PMutableCell$ = true;

com.rpl.specter.impl.MutableCell.prototype.com$rpl$specter$impl$PMutableCell$get_cell$arity$1 = (function (this$){
var self__ = this;
var this$__$1 = this;
return self__.q;
});

com.rpl.specter.impl.MutableCell.prototype.com$rpl$specter$impl$PMutableCell$set_cell$arity$2 = (function (this$,x){
var self__ = this;
var this$__$1 = this;
return self__.q = x;
});

com.rpl.specter.impl.MutableCell.getBasis = (function (){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"q","q",-1965434072,null)], null);
});

com.rpl.specter.impl.MutableCell.cljs$lang$type = true;

com.rpl.specter.impl.MutableCell.cljs$lang$ctorStr = "com.rpl.specter.impl/MutableCell";

com.rpl.specter.impl.MutableCell.cljs$lang$ctorPrWriter = (function (this__3585__auto__,writer__3586__auto__,opt__3587__auto__){
return cljs.core._write.call(null,writer__3586__auto__,"com.rpl.specter.impl/MutableCell");
});

com.rpl.specter.impl.__GT_MutableCell = (function com$rpl$specter$impl$__GT_MutableCell(q){
return (new com.rpl.specter.impl.MutableCell(q));
});

com.rpl.specter.impl.mutable_cell = (function com$rpl$specter$impl$mutable_cell(){
var G__282 = arguments.length;
switch (G__282) {
case 0:
return com.rpl.specter.impl.mutable_cell.cljs$core$IFn$_invoke$arity$0();

break;
case 1:
return com.rpl.specter.impl.mutable_cell.cljs$core$IFn$_invoke$arity$1((arguments[(0)]));

break;
default:
throw (new Error([cljs.core.str("Invalid arity: "),cljs.core.str(arguments.length)].join('')));

}
});

com.rpl.specter.impl.mutable_cell.cljs$core$IFn$_invoke$arity$0 = (function (){
return com.rpl.specter.impl.mutable_cell.call(null,null);
});

com.rpl.specter.impl.mutable_cell.cljs$core$IFn$_invoke$arity$1 = (function (init){
return (new com.rpl.specter.impl.MutableCell(init));
});

com.rpl.specter.impl.mutable_cell.cljs$lang$maxFixedArity = 1;
com.rpl.specter.impl.set_cell_BANG_ = (function com$rpl$specter$impl$set_cell_BANG_(cell,val){
return com.rpl.specter.impl.set_cell.call(null,cell,val);
});
com.rpl.specter.impl.get_cell = (function com$rpl$specter$impl$get_cell(cell){
return com.rpl.specter.impl.get_cell.call(null,cell);
});
com.rpl.specter.impl.update_cell_BANG_ = (function com$rpl$specter$impl$update_cell_BANG_(cell,afn){
var ret = afn.call(null,com.rpl.specter.impl.get_cell.call(null,cell));
com.rpl.specter.impl.set_cell_BANG_.call(null,cell,ret);

return ret;
});
com.rpl.specter.impl.append = (function com$rpl$specter$impl$append(coll,elem){
return cljs.core.conj.call(null,cljs.core.vec.call(null,coll),elem);
});

com.rpl.specter.impl.SetExtremes = (function (){var obj285 = {};
return obj285;
})();

com.rpl.specter.impl.set_first = (function com$rpl$specter$impl$set_first(s,val){
if((function (){var and__3362__auto__ = s;
if(and__3362__auto__){
return s.com$rpl$specter$impl$SetExtremes$set_first$arity$2;
} else {
return and__3362__auto__;
}
})()){
return s.com$rpl$specter$impl$SetExtremes$set_first$arity$2(s,val);
} else {
var x__3634__auto__ = (((s == null))?null:s);
return (function (){var or__3370__auto__ = (com.rpl.specter.impl.set_first[goog.typeOf(x__3634__auto__)]);
if(or__3370__auto__){
return or__3370__auto__;
} else {
var or__3370__auto____$1 = (com.rpl.specter.impl.set_first["_"]);
if(or__3370__auto____$1){
return or__3370__auto____$1;
} else {
throw cljs.core.missing_protocol.call(null,"SetExtremes.set-first",s);
}
}
})().call(null,s,val);
}
});

com.rpl.specter.impl.set_last = (function com$rpl$specter$impl$set_last(s,val){
if((function (){var and__3362__auto__ = s;
if(and__3362__auto__){
return s.com$rpl$specter$impl$SetExtremes$set_last$arity$2;
} else {
return and__3362__auto__;
}
})()){
return s.com$rpl$specter$impl$SetExtremes$set_last$arity$2(s,val);
} else {
var x__3634__auto__ = (((s == null))?null:s);
return (function (){var or__3370__auto__ = (com.rpl.specter.impl.set_last[goog.typeOf(x__3634__auto__)]);
if(or__3370__auto__){
return or__3370__auto__;
} else {
var or__3370__auto____$1 = (com.rpl.specter.impl.set_last["_"]);
if(or__3370__auto____$1){
return or__3370__auto____$1;
} else {
throw cljs.core.missing_protocol.call(null,"SetExtremes.set-last",s);
}
}
})().call(null,s,val);
}
});

com.rpl.specter.impl.set_first_list = (function com$rpl$specter$impl$set_first_list(l,v){
return cljs.core.cons.call(null,v,cljs.core.rest.call(null,l));
});
com.rpl.specter.impl.set_last_list = (function com$rpl$specter$impl$set_last_list(l,v){
return com.rpl.specter.impl.append.call(null,cljs.core.butlast.call(null,l),v);
});
cljs.core.PersistentVector.prototype.com$rpl$specter$impl$SetExtremes$ = true;

cljs.core.PersistentVector.prototype.com$rpl$specter$impl$SetExtremes$set_first$arity$2 = (function (v,val){
var v__$1 = this;
return cljs.core.assoc.call(null,v__$1,(0),val);
});

cljs.core.PersistentVector.prototype.com$rpl$specter$impl$SetExtremes$set_last$arity$2 = (function (v,val){
var v__$1 = this;
return cljs.core.assoc.call(null,v__$1,(cljs.core.count.call(null,v__$1) - (1)),val);
});

Object.prototype.com$rpl$specter$impl$SetExtremes$ = true;

Object.prototype.com$rpl$specter$impl$SetExtremes$set_first$arity$2 = (function (l,val){
var l__$1 = this;
return com.rpl.specter.impl.set_first_list.call(null,l__$1,val);
});

Object.prototype.com$rpl$specter$impl$SetExtremes$set_last$arity$2 = (function (l,val){
var l__$1 = this;
return com.rpl.specter.impl.set_last_list.call(null,l__$1,val);
});
com.rpl.specter.impl.walk_until = (function com$rpl$specter$impl$walk_until(pred,on_match_fn,structure){
if(cljs.core.truth_(pred.call(null,structure))){
return on_match_fn.call(null,structure);
} else {
return clojure.walk.walk.call(null,cljs.core.partial.call(null,com$rpl$specter$impl$walk_until,pred,on_match_fn),cljs.core.identity,structure);
}
});
com.rpl.specter.impl.fn_invocation_QMARK_ = (function com$rpl$specter$impl$fn_invocation_QMARK_(f){
return ((f instanceof clojure.lang.Cons)) || ((f instanceof clojure.lang.LazySeq)) || (cljs.core.list_QMARK_.call(null,f));
});
com.rpl.specter.impl.codewalk_until = (function com$rpl$specter$impl$codewalk_until(pred,on_match_fn,structure){
if(cljs.core.truth_(pred.call(null,structure))){
return on_match_fn.call(null,structure);
} else {
var ret = clojure.walk.walk.call(null,cljs.core.partial.call(null,com$rpl$specter$impl$codewalk_until,pred,on_match_fn),cljs.core.identity,structure);
if(cljs.core.truth_((function (){var and__3362__auto__ = com.rpl.specter.impl.fn_invocation_QMARK_.call(null,structure);
if(cljs.core.truth_(and__3362__auto__)){
return com.rpl.specter.impl.fn_invocation_QMARK_.call(null,ret);
} else {
return and__3362__auto__;
}
})())){
return cljs.core.with_meta.call(null,ret,cljs.core.meta.call(null,structure));
} else {
return ret;
}
}
});
com.rpl.specter.impl.conj_all_BANG_ = (function com$rpl$specter$impl$conj_all_BANG_(cell,elems){
return com.rpl.specter.impl.set_cell_BANG_.call(null,cell,cljs.core.concat.call(null,com.rpl.specter.impl.get_cell.call(null,cell),elems));
});
com.rpl.specter.impl.compiled_select_STAR_ = (function com$rpl$specter$impl$compiled_select_STAR_(tfns,structure){
var ex = tfns.executors();
return ex.select_executor().call(null,tfns.selector(),structure);
});
com.rpl.specter.impl.compiled_transform_STAR_ = (function com$rpl$specter$impl$compiled_transform_STAR_(tfns,transform_fn,structure){
var ex = tfns.executors();
return ex.transform_executor().call(null,tfns.transformer(),transform_fn,structure);
});
com.rpl.specter.impl.selected_QMARK__STAR_ = (function com$rpl$specter$impl$selected_QMARK__STAR_(compiled_path,structure){
return !(cljs.core.empty_QMARK_.call(null,com.rpl.specter.impl.compiled_select_STAR_.call(null,compiled_path,structure)));
});
com.rpl.specter.impl.walk_select = (function com$rpl$specter$impl$walk_select(pred,continue_fn,structure){
var ret = com.rpl.specter.impl.mutable_cell.call(null,cljs.core.PersistentVector.EMPTY);
var walker = ((function (ret){
return (function com$rpl$specter$impl$walk_select_$_this(structure__$1){
if(cljs.core.truth_(pred.call(null,structure__$1))){
return com.rpl.specter.impl.conj_all_BANG_.call(null,ret,continue_fn.call(null,structure__$1));
} else {
return clojure.walk.walk.call(null,com$rpl$specter$impl$walk_select_$_this,cljs.core.identity,structure__$1);
}
});})(ret))
;
walker.call(null,structure);

return com.rpl.specter.impl.get_cell.call(null,ret);
});
com.rpl.specter.impl.filter_PLUS_ancestry = (function com$rpl$specter$impl$filter_PLUS_ancestry(path,aseq){
var aseq__$1 = cljs.core.vec.call(null,aseq);
return cljs.core.reduce.call(null,((function (aseq__$1){
return (function (p__288,i){
var vec__289 = p__288;
var s = cljs.core.nth.call(null,vec__289,(0),null);
var m = cljs.core.nth.call(null,vec__289,(1),null);
var orig = vec__289;
var e = cljs.core.get.call(null,aseq__$1,i);
var pos = cljs.core.count.call(null,s);
if(cljs.core.truth_(com.rpl.specter.impl.selected_QMARK__STAR_.call(null,path,e))){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.conj.call(null,s,e),cljs.core.assoc.call(null,m,pos,i)], null);
} else {
return orig;
}
});})(aseq__$1))
,new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [cljs.core.PersistentVector.EMPTY,cljs.core.PersistentArrayMap.EMPTY], null),cljs.core.range.call(null,cljs.core.count.call(null,aseq__$1)));
});
com.rpl.specter.impl.key_select = (function com$rpl$specter$impl$key_select(akey,structure,next_fn){
return next_fn.call(null,cljs.core.get.call(null,structure,akey));
});
com.rpl.specter.impl.key_transform = (function com$rpl$specter$impl$key_transform(akey,structure,next_fn){
return cljs.core.assoc.call(null,structure,akey,next_fn.call(null,cljs.core.get.call(null,structure,akey)));
});

/**
* @constructor
*/
com.rpl.specter.impl.AllStructurePath = (function (){
})

com.rpl.specter.impl.AllStructurePath.getBasis = (function (){
return cljs.core.PersistentVector.EMPTY;
});

com.rpl.specter.impl.AllStructurePath.cljs$lang$type = true;

com.rpl.specter.impl.AllStructurePath.cljs$lang$ctorStr = "com.rpl.specter.impl/AllStructurePath";

com.rpl.specter.impl.AllStructurePath.cljs$lang$ctorPrWriter = (function (this__3585__auto__,writer__3586__auto__,opt__3587__auto__){
return cljs.core._write.call(null,writer__3586__auto__,"com.rpl.specter.impl/AllStructurePath");
});

com.rpl.specter.impl.__GT_AllStructurePath = (function com$rpl$specter$impl$__GT_AllStructurePath(){
return (new com.rpl.specter.impl.AllStructurePath());
});

com.rpl.specter.impl.AllStructurePath.prototype.com$rpl$specter$protocols$StructurePath$ = true;

com.rpl.specter.impl.AllStructurePath.prototype.com$rpl$specter$protocols$StructurePath$select_STAR_$arity$3 = (function (this$,structure,next_fn){
var this$__$1 = this;
return cljs.core.into.call(null,cljs.core.PersistentVector.EMPTY,clojure.core.reducers.mapcat.call(null,next_fn,structure));
});

com.rpl.specter.impl.AllStructurePath.prototype.com$rpl$specter$protocols$StructurePath$transform_STAR_$arity$3 = (function (this$,structure,next_fn){
var this$__$1 = this;
var empty_structure = cljs.core.empty.call(null,structure);
if(cljs.core.list_QMARK_.call(null,empty_structure)){
return cljs.core.doall.call(null,cljs.core.map.call(null,next_fn,structure));
} else {
return cljs.core.into.call(null,empty_structure,clojure.core.reducers.map.call(null,next_fn,structure));
}
});

/**
* @constructor
*/
com.rpl.specter.impl.ValCollect = (function (){
})

com.rpl.specter.impl.ValCollect.getBasis = (function (){
return cljs.core.PersistentVector.EMPTY;
});

com.rpl.specter.impl.ValCollect.cljs$lang$type = true;

com.rpl.specter.impl.ValCollect.cljs$lang$ctorStr = "com.rpl.specter.impl/ValCollect";

com.rpl.specter.impl.ValCollect.cljs$lang$ctorPrWriter = (function (this__3585__auto__,writer__3586__auto__,opt__3587__auto__){
return cljs.core._write.call(null,writer__3586__auto__,"com.rpl.specter.impl/ValCollect");
});

com.rpl.specter.impl.__GT_ValCollect = (function com$rpl$specter$impl$__GT_ValCollect(){
return (new com.rpl.specter.impl.ValCollect());
});

com.rpl.specter.impl.ValCollect.prototype.com$rpl$specter$protocols$Collector$ = true;

com.rpl.specter.impl.ValCollect.prototype.com$rpl$specter$protocols$Collector$collect_val$arity$2 = (function (this$,structure){
var this$__$1 = this;
return structure;
});

/**
* @constructor
*/
com.rpl.specter.impl.LastStructurePath = (function (){
})

com.rpl.specter.impl.LastStructurePath.getBasis = (function (){
return cljs.core.PersistentVector.EMPTY;
});

com.rpl.specter.impl.LastStructurePath.cljs$lang$type = true;

com.rpl.specter.impl.LastStructurePath.cljs$lang$ctorStr = "com.rpl.specter.impl/LastStructurePath";

com.rpl.specter.impl.LastStructurePath.cljs$lang$ctorPrWriter = (function (this__3585__auto__,writer__3586__auto__,opt__3587__auto__){
return cljs.core._write.call(null,writer__3586__auto__,"com.rpl.specter.impl/LastStructurePath");
});

com.rpl.specter.impl.__GT_LastStructurePath = (function com$rpl$specter$impl$__GT_LastStructurePath(){
return (new com.rpl.specter.impl.LastStructurePath());
});

com.rpl.specter.impl.LastStructurePath.prototype.com$rpl$specter$protocols$StructurePath$ = true;

com.rpl.specter.impl.LastStructurePath.prototype.com$rpl$specter$protocols$StructurePath$select_STAR_$arity$3 = (function (this$,structure,next_fn){
var this$__$1 = this;
return next_fn.call(null,cljs.core.last.call(null,structure));
});

com.rpl.specter.impl.LastStructurePath.prototype.com$rpl$specter$protocols$StructurePath$transform_STAR_$arity$3 = (function (this$,structure,next_fn){
var this$__$1 = this;
return com.rpl.specter.impl.set_last.call(null,structure,next_fn.call(null,cljs.core.last.call(null,structure)));
});

/**
* @constructor
*/
com.rpl.specter.impl.FirstStructurePath = (function (){
})

com.rpl.specter.impl.FirstStructurePath.getBasis = (function (){
return cljs.core.PersistentVector.EMPTY;
});

com.rpl.specter.impl.FirstStructurePath.cljs$lang$type = true;

com.rpl.specter.impl.FirstStructurePath.cljs$lang$ctorStr = "com.rpl.specter.impl/FirstStructurePath";

com.rpl.specter.impl.FirstStructurePath.cljs$lang$ctorPrWriter = (function (this__3585__auto__,writer__3586__auto__,opt__3587__auto__){
return cljs.core._write.call(null,writer__3586__auto__,"com.rpl.specter.impl/FirstStructurePath");
});

com.rpl.specter.impl.__GT_FirstStructurePath = (function com$rpl$specter$impl$__GT_FirstStructurePath(){
return (new com.rpl.specter.impl.FirstStructurePath());
});

com.rpl.specter.impl.FirstStructurePath.prototype.com$rpl$specter$protocols$StructurePath$ = true;

com.rpl.specter.impl.FirstStructurePath.prototype.com$rpl$specter$protocols$StructurePath$select_STAR_$arity$3 = (function (this$,structure,next_fn){
var this$__$1 = this;
return next_fn.call(null,cljs.core.first.call(null,structure));
});

com.rpl.specter.impl.FirstStructurePath.prototype.com$rpl$specter$protocols$StructurePath$transform_STAR_$arity$3 = (function (this$,structure,next_fn){
var this$__$1 = this;
return com.rpl.specter.impl.set_first.call(null,structure,next_fn.call(null,cljs.core.first.call(null,structure)));
});

/**
* @constructor
*/
com.rpl.specter.impl.WalkerStructurePath = (function (afn){
this.afn = afn;
})

com.rpl.specter.impl.WalkerStructurePath.getBasis = (function (){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"afn","afn",216963467,null)], null);
});

com.rpl.specter.impl.WalkerStructurePath.cljs$lang$type = true;

com.rpl.specter.impl.WalkerStructurePath.cljs$lang$ctorStr = "com.rpl.specter.impl/WalkerStructurePath";

com.rpl.specter.impl.WalkerStructurePath.cljs$lang$ctorPrWriter = (function (this__3585__auto__,writer__3586__auto__,opt__3587__auto__){
return cljs.core._write.call(null,writer__3586__auto__,"com.rpl.specter.impl/WalkerStructurePath");
});

com.rpl.specter.impl.__GT_WalkerStructurePath = (function com$rpl$specter$impl$__GT_WalkerStructurePath(afn){
return (new com.rpl.specter.impl.WalkerStructurePath(afn));
});

com.rpl.specter.impl.WalkerStructurePath.prototype.com$rpl$specter$protocols$StructurePath$ = true;

com.rpl.specter.impl.WalkerStructurePath.prototype.com$rpl$specter$protocols$StructurePath$select_STAR_$arity$3 = (function (this$,structure,next_fn){
var this$__$1 = this;
return com.rpl.specter.impl.walk_select.call(null,this$__$1.afn(),next_fn,structure);
});

com.rpl.specter.impl.WalkerStructurePath.prototype.com$rpl$specter$protocols$StructurePath$transform_STAR_$arity$3 = (function (this$,structure,next_fn){
var this$__$1 = this;
return com.rpl.specter.impl.walk_until.call(null,this$__$1.afn(),next_fn,structure);
});

/**
* @constructor
*/
com.rpl.specter.impl.CodeWalkerStructurePath = (function (afn){
this.afn = afn;
})

com.rpl.specter.impl.CodeWalkerStructurePath.getBasis = (function (){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"afn","afn",216963467,null)], null);
});

com.rpl.specter.impl.CodeWalkerStructurePath.cljs$lang$type = true;

com.rpl.specter.impl.CodeWalkerStructurePath.cljs$lang$ctorStr = "com.rpl.specter.impl/CodeWalkerStructurePath";

com.rpl.specter.impl.CodeWalkerStructurePath.cljs$lang$ctorPrWriter = (function (this__3585__auto__,writer__3586__auto__,opt__3587__auto__){
return cljs.core._write.call(null,writer__3586__auto__,"com.rpl.specter.impl/CodeWalkerStructurePath");
});

com.rpl.specter.impl.__GT_CodeWalkerStructurePath = (function com$rpl$specter$impl$__GT_CodeWalkerStructurePath(afn){
return (new com.rpl.specter.impl.CodeWalkerStructurePath(afn));
});

com.rpl.specter.impl.CodeWalkerStructurePath.prototype.com$rpl$specter$protocols$StructurePath$ = true;

com.rpl.specter.impl.CodeWalkerStructurePath.prototype.com$rpl$specter$protocols$StructurePath$select_STAR_$arity$3 = (function (this$,structure,next_fn){
var this$__$1 = this;
return com.rpl.specter.impl.walk_select.call(null,this$__$1.afn(),next_fn,structure);
});

com.rpl.specter.impl.CodeWalkerStructurePath.prototype.com$rpl$specter$protocols$StructurePath$transform_STAR_$arity$3 = (function (this$,structure,next_fn){
var this$__$1 = this;
return com.rpl.specter.impl.codewalk_until.call(null,this$__$1.afn(),next_fn,structure);
});

/**
* @constructor
*/
com.rpl.specter.impl.FilterStructurePath = (function (path){
this.path = path;
})

com.rpl.specter.impl.FilterStructurePath.getBasis = (function (){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"path","path",1452340359,null)], null);
});

com.rpl.specter.impl.FilterStructurePath.cljs$lang$type = true;

com.rpl.specter.impl.FilterStructurePath.cljs$lang$ctorStr = "com.rpl.specter.impl/FilterStructurePath";

com.rpl.specter.impl.FilterStructurePath.cljs$lang$ctorPrWriter = (function (this__3585__auto__,writer__3586__auto__,opt__3587__auto__){
return cljs.core._write.call(null,writer__3586__auto__,"com.rpl.specter.impl/FilterStructurePath");
});

com.rpl.specter.impl.__GT_FilterStructurePath = (function com$rpl$specter$impl$__GT_FilterStructurePath(path){
return (new com.rpl.specter.impl.FilterStructurePath(path));
});

com.rpl.specter.impl.FilterStructurePath.prototype.com$rpl$specter$protocols$StructurePath$ = true;

com.rpl.specter.impl.FilterStructurePath.prototype.com$rpl$specter$protocols$StructurePath$select_STAR_$arity$3 = (function (this$,structure,next_fn){
var this$__$1 = this;
return next_fn.call(null,cljs.core.doall.call(null,cljs.core.filter.call(null,((function (this$__$1){
return (function (p1__290_SHARP_){
return com.rpl.specter.impl.selected_QMARK__STAR_.call(null,this$__$1.path(),p1__290_SHARP_);
});})(this$__$1))
,structure)));
});

com.rpl.specter.impl.FilterStructurePath.prototype.com$rpl$specter$protocols$StructurePath$transform_STAR_$arity$3 = (function (this$,structure,next_fn){
var this$__$1 = this;
var vec__291 = com.rpl.specter.impl.filter_PLUS_ancestry.call(null,this$__$1.path(),structure);
var filtered = cljs.core.nth.call(null,vec__291,(0),null);
var ancestry = cljs.core.nth.call(null,vec__291,(1),null);
var next = cljs.core.vec.call(null,next_fn.call(null,filtered));
return cljs.core.reduce.call(null,((function (vec__291,filtered,ancestry,next,this$__$1){
return (function (curr,p__292){
var vec__293 = p__292;
var newi = cljs.core.nth.call(null,vec__293,(0),null);
var oldi = cljs.core.nth.call(null,vec__293,(1),null);
return cljs.core.assoc.call(null,curr,oldi,cljs.core.get.call(null,next,newi));
});})(vec__291,filtered,ancestry,next,this$__$1))
,cljs.core.vec.call(null,structure),ancestry);
});

/**
* @constructor
*/
com.rpl.specter.impl.KeyPath = (function (akey){
this.akey = akey;
})

com.rpl.specter.impl.KeyPath.getBasis = (function (){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"akey","akey",-1227743693,null)], null);
});

com.rpl.specter.impl.KeyPath.cljs$lang$type = true;

com.rpl.specter.impl.KeyPath.cljs$lang$ctorStr = "com.rpl.specter.impl/KeyPath";

com.rpl.specter.impl.KeyPath.cljs$lang$ctorPrWriter = (function (this__3585__auto__,writer__3586__auto__,opt__3587__auto__){
return cljs.core._write.call(null,writer__3586__auto__,"com.rpl.specter.impl/KeyPath");
});

com.rpl.specter.impl.__GT_KeyPath = (function com$rpl$specter$impl$__GT_KeyPath(akey){
return (new com.rpl.specter.impl.KeyPath(akey));
});

com.rpl.specter.impl.KeyPath.prototype.com$rpl$specter$protocols$StructurePath$ = true;

com.rpl.specter.impl.KeyPath.prototype.com$rpl$specter$protocols$StructurePath$select_STAR_$arity$3 = (function (this$,structure,next_fn){
var this$__$1 = this;
return com.rpl.specter.impl.key_select.call(null,this$__$1.akey(),structure,next_fn);
});

com.rpl.specter.impl.KeyPath.prototype.com$rpl$specter$protocols$StructurePath$transform_STAR_$arity$3 = (function (this$,structure,next_fn){
var this$__$1 = this;
return com.rpl.specter.impl.key_transform.call(null,this$__$1.akey(),structure,next_fn);
});

/**
* @constructor
*/
com.rpl.specter.impl.SelectCollector = (function (sel_fn,selector){
this.sel_fn = sel_fn;
this.selector = selector;
})

com.rpl.specter.impl.SelectCollector.getBasis = (function (){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"sel-fn","sel-fn",-1690237013,null),new cljs.core.Symbol(null,"selector","selector",-1891906903,null)], null);
});

com.rpl.specter.impl.SelectCollector.cljs$lang$type = true;

com.rpl.specter.impl.SelectCollector.cljs$lang$ctorStr = "com.rpl.specter.impl/SelectCollector";

com.rpl.specter.impl.SelectCollector.cljs$lang$ctorPrWriter = (function (this__3585__auto__,writer__3586__auto__,opt__3587__auto__){
return cljs.core._write.call(null,writer__3586__auto__,"com.rpl.specter.impl/SelectCollector");
});

com.rpl.specter.impl.__GT_SelectCollector = (function com$rpl$specter$impl$__GT_SelectCollector(sel_fn,selector){
return (new com.rpl.specter.impl.SelectCollector(sel_fn,selector));
});

com.rpl.specter.impl.SelectCollector.prototype.com$rpl$specter$protocols$Collector$ = true;

com.rpl.specter.impl.SelectCollector.prototype.com$rpl$specter$protocols$Collector$collect_val$arity$2 = (function (this$,structure){
var this$__$1 = this;
return this$__$1.sel_fn().call(null,this$__$1.selector(),structure);
});

/**
* @constructor
*/
com.rpl.specter.impl.SRangePath = (function (start_fn,end_fn){
this.start_fn = start_fn;
this.end_fn = end_fn;
})

com.rpl.specter.impl.SRangePath.getBasis = (function (){
return new cljs.core.PersistentVector(null, 2, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"start-fn","start-fn",-1617360859,null),new cljs.core.Symbol(null,"end-fn","end-fn",1694587211,null)], null);
});

com.rpl.specter.impl.SRangePath.cljs$lang$type = true;

com.rpl.specter.impl.SRangePath.cljs$lang$ctorStr = "com.rpl.specter.impl/SRangePath";

com.rpl.specter.impl.SRangePath.cljs$lang$ctorPrWriter = (function (this__3585__auto__,writer__3586__auto__,opt__3587__auto__){
return cljs.core._write.call(null,writer__3586__auto__,"com.rpl.specter.impl/SRangePath");
});

com.rpl.specter.impl.__GT_SRangePath = (function com$rpl$specter$impl$__GT_SRangePath(start_fn,end_fn){
return (new com.rpl.specter.impl.SRangePath(start_fn,end_fn));
});

com.rpl.specter.impl.SRangePath.prototype.com$rpl$specter$protocols$StructurePath$ = true;

com.rpl.specter.impl.SRangePath.prototype.com$rpl$specter$protocols$StructurePath$select_STAR_$arity$3 = (function (this$,structure,next_fn){
var this$__$1 = this;
var start = this$__$1.start_fn().call(null,structure);
var end = this$__$1.end_fn().call(null,structure);
return next_fn.call(null,cljs.core.subvec.call(null,cljs.core.vec.call(null,structure),start,end));
});

com.rpl.specter.impl.SRangePath.prototype.com$rpl$specter$protocols$StructurePath$transform_STAR_$arity$3 = (function (this$,structure,next_fn){
var this$__$1 = this;
var start = this$__$1.start_fn().call(null,structure);
var end = this$__$1.end_fn().call(null,structure);
var structurev = cljs.core.vec.call(null,structure);
var newpart = next_fn.call(null,cljs.core.subvec.call(null,structurev,start,end));
var res = cljs.core.concat.call(null,cljs.core.subvec.call(null,structurev,(0),start),newpart,cljs.core.subvec.call(null,structurev,end,cljs.core.count.call(null,structure)));
if(cljs.core.vector_QMARK_.call(null,structure)){
return cljs.core.vec.call(null,res);
} else {
return res;
}
});

/**
* @constructor
*/
com.rpl.specter.impl.ViewPath = (function (view_fn){
this.view_fn = view_fn;
})

com.rpl.specter.impl.ViewPath.getBasis = (function (){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"view-fn","view-fn",-500169128,null)], null);
});

com.rpl.specter.impl.ViewPath.cljs$lang$type = true;

com.rpl.specter.impl.ViewPath.cljs$lang$ctorStr = "com.rpl.specter.impl/ViewPath";

com.rpl.specter.impl.ViewPath.cljs$lang$ctorPrWriter = (function (this__3585__auto__,writer__3586__auto__,opt__3587__auto__){
return cljs.core._write.call(null,writer__3586__auto__,"com.rpl.specter.impl/ViewPath");
});

com.rpl.specter.impl.__GT_ViewPath = (function com$rpl$specter$impl$__GT_ViewPath(view_fn){
return (new com.rpl.specter.impl.ViewPath(view_fn));
});

com.rpl.specter.impl.ViewPath.prototype.com$rpl$specter$protocols$StructurePath$ = true;

com.rpl.specter.impl.ViewPath.prototype.com$rpl$specter$protocols$StructurePath$select_STAR_$arity$3 = (function (this$,structure,next_fn){
var this$__$1 = this;
return next_fn.call(null,this$__$1.view_fn().call(null,structure));
});

com.rpl.specter.impl.ViewPath.prototype.com$rpl$specter$protocols$StructurePath$transform_STAR_$arity$3 = (function (this$,structure,next_fn){
var this$__$1 = this;
return next_fn.call(null,this$__$1.view_fn().call(null,structure));
});

/**
* @constructor
*/
com.rpl.specter.impl.PutValCollector = (function (val){
this.val = val;
})

com.rpl.specter.impl.PutValCollector.getBasis = (function (){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"val","val",1769233139,null)], null);
});

com.rpl.specter.impl.PutValCollector.cljs$lang$type = true;

com.rpl.specter.impl.PutValCollector.cljs$lang$ctorStr = "com.rpl.specter.impl/PutValCollector";

com.rpl.specter.impl.PutValCollector.cljs$lang$ctorPrWriter = (function (this__3585__auto__,writer__3586__auto__,opt__3587__auto__){
return cljs.core._write.call(null,writer__3586__auto__,"com.rpl.specter.impl/PutValCollector");
});

com.rpl.specter.impl.__GT_PutValCollector = (function com$rpl$specter$impl$__GT_PutValCollector(val){
return (new com.rpl.specter.impl.PutValCollector(val));
});

com.rpl.specter.impl.PutValCollector.prototype.com$rpl$specter$protocols$Collector$ = true;

com.rpl.specter.impl.PutValCollector.prototype.com$rpl$specter$protocols$Collector$collect_val$arity$2 = (function (this$,structure){
var this$__$1 = this;
return this$__$1.val();
});
(com.rpl.specter.protocols.StructurePath["null"] = true);

(com.rpl.specter.protocols.select_STAR_["null"] = (function (this$,structure,next_fn){
return next_fn.call(null,structure);
}));

(com.rpl.specter.protocols.transform_STAR_["null"] = (function (this$,structure,next_fn){
return next_fn.call(null,structure);
}));

/**
* @constructor
*/
com.rpl.specter.impl.ConditionalPath = (function (cond_pairs){
this.cond_pairs = cond_pairs;
})

com.rpl.specter.impl.ConditionalPath.getBasis = (function (){
return new cljs.core.PersistentVector(null, 1, 5, cljs.core.PersistentVector.EMPTY_NODE, [new cljs.core.Symbol(null,"cond-pairs","cond-pairs",11901227,null)], null);
});

com.rpl.specter.impl.ConditionalPath.cljs$lang$type = true;

com.rpl.specter.impl.ConditionalPath.cljs$lang$ctorStr = "com.rpl.specter.impl/ConditionalPath";

com.rpl.specter.impl.ConditionalPath.cljs$lang$ctorPrWriter = (function (this__3585__auto__,writer__3586__auto__,opt__3587__auto__){
return cljs.core._write.call(null,writer__3586__auto__,"com.rpl.specter.impl/ConditionalPath");
});

com.rpl.specter.impl.__GT_ConditionalPath = (function com$rpl$specter$impl$__GT_ConditionalPath(cond_pairs){
return (new com.rpl.specter.impl.ConditionalPath(cond_pairs));
});

com.rpl.specter.impl.retrieve_selector = (function com$rpl$specter$impl$retrieve_selector(cond_pairs,structure){
return cljs.core.second.call(null,cljs.core.first.call(null,cljs.core.drop_while.call(null,(function (p__296){
var vec__297 = p__296;
var c_selector = cljs.core.nth.call(null,vec__297,(0),null);
var _ = cljs.core.nth.call(null,vec__297,(1),null);
return cljs.core.empty_QMARK_.call(null,com.rpl.specter.impl.compiled_select_STAR_.call(null,c_selector,structure));
}),cond_pairs)));
});
com.rpl.specter.impl.ConditionalPath.prototype.com$rpl$specter$protocols$StructurePath$ = true;

com.rpl.specter.impl.ConditionalPath.prototype.com$rpl$specter$protocols$StructurePath$select_STAR_$arity$3 = (function (this$,structure,next_fn){
var this$__$1 = this;
var temp__4421__auto__ = com.rpl.specter.impl.retrieve_selector.call(null,this$__$1.cond_pairs(),structure);
if(cljs.core.truth_(temp__4421__auto__)){
var selector = temp__4421__auto__;
return cljs.core.doall.call(null,cljs.core.mapcat.call(null,next_fn,com.rpl.specter.impl.compiled_select_STAR_.call(null,selector,structure)));
} else {
return null;
}
});

com.rpl.specter.impl.ConditionalPath.prototype.com$rpl$specter$protocols$StructurePath$transform_STAR_$arity$3 = (function (this$,structure,next_fn){
var this$__$1 = this;
var temp__4421__auto__ = com.rpl.specter.impl.retrieve_selector.call(null,this$__$1.cond_pairs(),structure);
if(cljs.core.truth_(temp__4421__auto__)){
var selector = temp__4421__auto__;
return com.rpl.specter.impl.compiled_transform_STAR_.call(null,selector,next_fn,structure);
} else {
return structure;
}
});

//# sourceMappingURL=impl.js.map