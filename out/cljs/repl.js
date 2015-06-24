// Compiled by ClojureScript 0.0-3308 {:target :nodejs}
goog.provide('cljs.repl');
goog.require('cljs.core');
cljs.repl.print_doc = (function cljs$repl$print_doc(m){
cljs.core.println.call(null,"-------------------------");

cljs.core.println.call(null,[cljs.core.str((function (){var temp__4423__auto__ = new cljs.core.Keyword(null,"ns","ns",441598760).cljs$core$IFn$_invoke$arity$1(m);
if(cljs.core.truth_(temp__4423__auto__)){
var ns = temp__4423__auto__;
return [cljs.core.str(ns),cljs.core.str("/")].join('');
} else {
return null;
}
})()),cljs.core.str(new cljs.core.Keyword(null,"name","name",1843675177).cljs$core$IFn$_invoke$arity$1(m))].join(''));

if(cljs.core.truth_(new cljs.core.Keyword(null,"protocol","protocol",652470118).cljs$core$IFn$_invoke$arity$1(m))){
cljs.core.println.call(null,"Protocol");
} else {
}

if(cljs.core.truth_(new cljs.core.Keyword(null,"forms","forms",2045992350).cljs$core$IFn$_invoke$arity$1(m))){
var seq__1866_1878 = cljs.core.seq.call(null,new cljs.core.Keyword(null,"forms","forms",2045992350).cljs$core$IFn$_invoke$arity$1(m));
var chunk__1867_1879 = null;
var count__1868_1880 = (0);
var i__1869_1881 = (0);
while(true){
if((i__1869_1881 < count__1868_1880)){
var f_1882 = cljs.core._nth.call(null,chunk__1867_1879,i__1869_1881);
cljs.core.println.call(null,"  ",f_1882);

var G__1883 = seq__1866_1878;
var G__1884 = chunk__1867_1879;
var G__1885 = count__1868_1880;
var G__1886 = (i__1869_1881 + (1));
seq__1866_1878 = G__1883;
chunk__1867_1879 = G__1884;
count__1868_1880 = G__1885;
i__1869_1881 = G__1886;
continue;
} else {
var temp__4423__auto___1887 = cljs.core.seq.call(null,seq__1866_1878);
if(temp__4423__auto___1887){
var seq__1866_1888__$1 = temp__4423__auto___1887;
if(cljs.core.chunked_seq_QMARK_.call(null,seq__1866_1888__$1)){
var c__3739__auto___1889 = cljs.core.chunk_first.call(null,seq__1866_1888__$1);
var G__1890 = cljs.core.chunk_rest.call(null,seq__1866_1888__$1);
var G__1891 = c__3739__auto___1889;
var G__1892 = cljs.core.count.call(null,c__3739__auto___1889);
var G__1893 = (0);
seq__1866_1878 = G__1890;
chunk__1867_1879 = G__1891;
count__1868_1880 = G__1892;
i__1869_1881 = G__1893;
continue;
} else {
var f_1894 = cljs.core.first.call(null,seq__1866_1888__$1);
cljs.core.println.call(null,"  ",f_1894);

var G__1895 = cljs.core.next.call(null,seq__1866_1888__$1);
var G__1896 = null;
var G__1897 = (0);
var G__1898 = (0);
seq__1866_1878 = G__1895;
chunk__1867_1879 = G__1896;
count__1868_1880 = G__1897;
i__1869_1881 = G__1898;
continue;
}
} else {
}
}
break;
}
} else {
if(cljs.core.truth_(new cljs.core.Keyword(null,"arglists","arglists",1661989754).cljs$core$IFn$_invoke$arity$1(m))){
var arglists_1899 = new cljs.core.Keyword(null,"arglists","arglists",1661989754).cljs$core$IFn$_invoke$arity$1(m);
if(cljs.core.truth_((function (){var or__3370__auto__ = new cljs.core.Keyword(null,"macro","macro",-867863404).cljs$core$IFn$_invoke$arity$1(m);
if(cljs.core.truth_(or__3370__auto__)){
return or__3370__auto__;
} else {
return new cljs.core.Keyword(null,"repl-special-function","repl-special-function",1262603725).cljs$core$IFn$_invoke$arity$1(m);
}
})())){
cljs.core.prn.call(null,arglists_1899);
} else {
cljs.core.prn.call(null,((cljs.core._EQ_.call(null,new cljs.core.Symbol(null,"quote","quote",1377916282,null),cljs.core.first.call(null,arglists_1899)))?cljs.core.second.call(null,arglists_1899):arglists_1899));
}
} else {
}
}

if(cljs.core.truth_(new cljs.core.Keyword(null,"special-form","special-form",-1326536374).cljs$core$IFn$_invoke$arity$1(m))){
cljs.core.println.call(null,"Special Form");

cljs.core.println.call(null," ",new cljs.core.Keyword(null,"doc","doc",1913296891).cljs$core$IFn$_invoke$arity$1(m));

if(cljs.core.contains_QMARK_.call(null,m,new cljs.core.Keyword(null,"url","url",276297046))){
if(cljs.core.truth_(new cljs.core.Keyword(null,"url","url",276297046).cljs$core$IFn$_invoke$arity$1(m))){
return cljs.core.println.call(null,[cljs.core.str("\n  Please see http://clojure.org/"),cljs.core.str(new cljs.core.Keyword(null,"url","url",276297046).cljs$core$IFn$_invoke$arity$1(m))].join(''));
} else {
return null;
}
} else {
return cljs.core.println.call(null,[cljs.core.str("\n  Please see http://clojure.org/special_forms#"),cljs.core.str(new cljs.core.Keyword(null,"name","name",1843675177).cljs$core$IFn$_invoke$arity$1(m))].join(''));
}
} else {
if(cljs.core.truth_(new cljs.core.Keyword(null,"macro","macro",-867863404).cljs$core$IFn$_invoke$arity$1(m))){
cljs.core.println.call(null,"Macro");
} else {
}

if(cljs.core.truth_(new cljs.core.Keyword(null,"repl-special-function","repl-special-function",1262603725).cljs$core$IFn$_invoke$arity$1(m))){
cljs.core.println.call(null,"REPL Special Function");
} else {
}

cljs.core.println.call(null," ",new cljs.core.Keyword(null,"doc","doc",1913296891).cljs$core$IFn$_invoke$arity$1(m));

if(cljs.core.truth_(new cljs.core.Keyword(null,"protocol","protocol",652470118).cljs$core$IFn$_invoke$arity$1(m))){
var seq__1870 = cljs.core.seq.call(null,new cljs.core.Keyword(null,"methods","methods",453930866).cljs$core$IFn$_invoke$arity$1(m));
var chunk__1871 = null;
var count__1872 = (0);
var i__1873 = (0);
while(true){
if((i__1873 < count__1872)){
var vec__1874 = cljs.core._nth.call(null,chunk__1871,i__1873);
var name = cljs.core.nth.call(null,vec__1874,(0),null);
var map__1875 = cljs.core.nth.call(null,vec__1874,(1),null);
var map__1875__$1 = ((cljs.core.seq_QMARK_.call(null,map__1875))?cljs.core.apply.call(null,cljs.core.hash_map,map__1875):map__1875);
var doc = cljs.core.get.call(null,map__1875__$1,new cljs.core.Keyword(null,"doc","doc",1913296891));
var arglists = cljs.core.get.call(null,map__1875__$1,new cljs.core.Keyword(null,"arglists","arglists",1661989754));
cljs.core.println.call(null);

cljs.core.println.call(null," ",name);

cljs.core.println.call(null," ",arglists);

if(cljs.core.truth_(doc)){
cljs.core.println.call(null," ",doc);
} else {
}

var G__1900 = seq__1870;
var G__1901 = chunk__1871;
var G__1902 = count__1872;
var G__1903 = (i__1873 + (1));
seq__1870 = G__1900;
chunk__1871 = G__1901;
count__1872 = G__1902;
i__1873 = G__1903;
continue;
} else {
var temp__4423__auto__ = cljs.core.seq.call(null,seq__1870);
if(temp__4423__auto__){
var seq__1870__$1 = temp__4423__auto__;
if(cljs.core.chunked_seq_QMARK_.call(null,seq__1870__$1)){
var c__3739__auto__ = cljs.core.chunk_first.call(null,seq__1870__$1);
var G__1904 = cljs.core.chunk_rest.call(null,seq__1870__$1);
var G__1905 = c__3739__auto__;
var G__1906 = cljs.core.count.call(null,c__3739__auto__);
var G__1907 = (0);
seq__1870 = G__1904;
chunk__1871 = G__1905;
count__1872 = G__1906;
i__1873 = G__1907;
continue;
} else {
var vec__1876 = cljs.core.first.call(null,seq__1870__$1);
var name = cljs.core.nth.call(null,vec__1876,(0),null);
var map__1877 = cljs.core.nth.call(null,vec__1876,(1),null);
var map__1877__$1 = ((cljs.core.seq_QMARK_.call(null,map__1877))?cljs.core.apply.call(null,cljs.core.hash_map,map__1877):map__1877);
var doc = cljs.core.get.call(null,map__1877__$1,new cljs.core.Keyword(null,"doc","doc",1913296891));
var arglists = cljs.core.get.call(null,map__1877__$1,new cljs.core.Keyword(null,"arglists","arglists",1661989754));
cljs.core.println.call(null);

cljs.core.println.call(null," ",name);

cljs.core.println.call(null," ",arglists);

if(cljs.core.truth_(doc)){
cljs.core.println.call(null," ",doc);
} else {
}

var G__1908 = cljs.core.next.call(null,seq__1870__$1);
var G__1909 = null;
var G__1910 = (0);
var G__1911 = (0);
seq__1870 = G__1908;
chunk__1871 = G__1909;
count__1872 = G__1910;
i__1873 = G__1911;
continue;
}
} else {
return null;
}
}
break;
}
} else {
return null;
}
}
});

//# sourceMappingURL=repl.js.map