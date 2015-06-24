// Compiled by ClojureScript 0.0-3308 {:target :nodejs}
goog.provide('com.rpl.specter.protocols');
goog.require('cljs.core');

com.rpl.specter.protocols.StructureValsPath = (function (){var obj263 = {};
return obj263;
})();

com.rpl.specter.protocols.select_full_STAR_ = (function com$rpl$specter$protocols$select_full_STAR_(this$,vals,structure,next_fn){
if((function (){var and__3362__auto__ = this$;
if(and__3362__auto__){
return this$.com$rpl$specter$protocols$StructureValsPath$select_full_STAR_$arity$4;
} else {
return and__3362__auto__;
}
})()){
return this$.com$rpl$specter$protocols$StructureValsPath$select_full_STAR_$arity$4(this$,vals,structure,next_fn);
} else {
var x__3634__auto__ = (((this$ == null))?null:this$);
return (function (){var or__3370__auto__ = (com.rpl.specter.protocols.select_full_STAR_[goog.typeOf(x__3634__auto__)]);
if(or__3370__auto__){
return or__3370__auto__;
} else {
var or__3370__auto____$1 = (com.rpl.specter.protocols.select_full_STAR_["_"]);
if(or__3370__auto____$1){
return or__3370__auto____$1;
} else {
throw cljs.core.missing_protocol.call(null,"StructureValsPath.select-full*",this$);
}
}
})().call(null,this$,vals,structure,next_fn);
}
});

com.rpl.specter.protocols.transform_full_STAR_ = (function com$rpl$specter$protocols$transform_full_STAR_(this$,vals,structure,next_fn){
if((function (){var and__3362__auto__ = this$;
if(and__3362__auto__){
return this$.com$rpl$specter$protocols$StructureValsPath$transform_full_STAR_$arity$4;
} else {
return and__3362__auto__;
}
})()){
return this$.com$rpl$specter$protocols$StructureValsPath$transform_full_STAR_$arity$4(this$,vals,structure,next_fn);
} else {
var x__3634__auto__ = (((this$ == null))?null:this$);
return (function (){var or__3370__auto__ = (com.rpl.specter.protocols.transform_full_STAR_[goog.typeOf(x__3634__auto__)]);
if(or__3370__auto__){
return or__3370__auto__;
} else {
var or__3370__auto____$1 = (com.rpl.specter.protocols.transform_full_STAR_["_"]);
if(or__3370__auto____$1){
return or__3370__auto____$1;
} else {
throw cljs.core.missing_protocol.call(null,"StructureValsPath.transform-full*",this$);
}
}
})().call(null,this$,vals,structure,next_fn);
}
});


com.rpl.specter.protocols.StructurePath = (function (){var obj265 = {};
return obj265;
})();

com.rpl.specter.protocols.select_STAR_ = (function com$rpl$specter$protocols$select_STAR_(this$,structure,next_fn){
if((function (){var and__3362__auto__ = this$;
if(and__3362__auto__){
return this$.com$rpl$specter$protocols$StructurePath$select_STAR_$arity$3;
} else {
return and__3362__auto__;
}
})()){
return this$.com$rpl$specter$protocols$StructurePath$select_STAR_$arity$3(this$,structure,next_fn);
} else {
var x__3634__auto__ = (((this$ == null))?null:this$);
return (function (){var or__3370__auto__ = (com.rpl.specter.protocols.select_STAR_[goog.typeOf(x__3634__auto__)]);
if(or__3370__auto__){
return or__3370__auto__;
} else {
var or__3370__auto____$1 = (com.rpl.specter.protocols.select_STAR_["_"]);
if(or__3370__auto____$1){
return or__3370__auto____$1;
} else {
throw cljs.core.missing_protocol.call(null,"StructurePath.select*",this$);
}
}
})().call(null,this$,structure,next_fn);
}
});

com.rpl.specter.protocols.transform_STAR_ = (function com$rpl$specter$protocols$transform_STAR_(this$,structure,next_fn){
if((function (){var and__3362__auto__ = this$;
if(and__3362__auto__){
return this$.com$rpl$specter$protocols$StructurePath$transform_STAR_$arity$3;
} else {
return and__3362__auto__;
}
})()){
return this$.com$rpl$specter$protocols$StructurePath$transform_STAR_$arity$3(this$,structure,next_fn);
} else {
var x__3634__auto__ = (((this$ == null))?null:this$);
return (function (){var or__3370__auto__ = (com.rpl.specter.protocols.transform_STAR_[goog.typeOf(x__3634__auto__)]);
if(or__3370__auto__){
return or__3370__auto__;
} else {
var or__3370__auto____$1 = (com.rpl.specter.protocols.transform_STAR_["_"]);
if(or__3370__auto____$1){
return or__3370__auto____$1;
} else {
throw cljs.core.missing_protocol.call(null,"StructurePath.transform*",this$);
}
}
})().call(null,this$,structure,next_fn);
}
});


com.rpl.specter.protocols.Collector = (function (){var obj267 = {};
return obj267;
})();

com.rpl.specter.protocols.collect_val = (function com$rpl$specter$protocols$collect_val(this$,structure){
if((function (){var and__3362__auto__ = this$;
if(and__3362__auto__){
return this$.com$rpl$specter$protocols$Collector$collect_val$arity$2;
} else {
return and__3362__auto__;
}
})()){
return this$.com$rpl$specter$protocols$Collector$collect_val$arity$2(this$,structure);
} else {
var x__3634__auto__ = (((this$ == null))?null:this$);
return (function (){var or__3370__auto__ = (com.rpl.specter.protocols.collect_val[goog.typeOf(x__3634__auto__)]);
if(or__3370__auto__){
return or__3370__auto__;
} else {
var or__3370__auto____$1 = (com.rpl.specter.protocols.collect_val["_"]);
if(or__3370__auto____$1){
return or__3370__auto____$1;
} else {
throw cljs.core.missing_protocol.call(null,"Collector.collect-val",this$);
}
}
})().call(null,this$,structure);
}
});


com.rpl.specter.protocols.StructureValsPathComposer = (function (){var obj269 = {};
return obj269;
})();

com.rpl.specter.protocols.comp_paths_STAR_ = (function com$rpl$specter$protocols$comp_paths_STAR_(paths){
if((function (){var and__3362__auto__ = paths;
if(and__3362__auto__){
return paths.com$rpl$specter$protocols$StructureValsPathComposer$comp_paths_STAR_$arity$1;
} else {
return and__3362__auto__;
}
})()){
return paths.com$rpl$specter$protocols$StructureValsPathComposer$comp_paths_STAR_$arity$1(paths);
} else {
var x__3634__auto__ = (((paths == null))?null:paths);
return (function (){var or__3370__auto__ = (com.rpl.specter.protocols.comp_paths_STAR_[goog.typeOf(x__3634__auto__)]);
if(or__3370__auto__){
return or__3370__auto__;
} else {
var or__3370__auto____$1 = (com.rpl.specter.protocols.comp_paths_STAR_["_"]);
if(or__3370__auto____$1){
return or__3370__auto____$1;
} else {
throw cljs.core.missing_protocol.call(null,"StructureValsPathComposer.comp-paths*",paths);
}
}
})().call(null,paths);
}
});


//# sourceMappingURL=protocols.js.map