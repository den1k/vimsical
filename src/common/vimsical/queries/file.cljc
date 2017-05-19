(ns vimsical.queries.file
  (:require
   [vimsical.queries.compiler :as compiler]
   [vimsical.queries.snapshot :as snapshot]
   [vimsical.vcs.file :as file]))

(def pull-query
  [:db/uid
   ::file/name
   ::file/type
   ::file/sub-type
   ::file/compiler
   ::file/lang-version
   {::file/compiler compiler/pull-query}
   {::file/snapshot snapshot/pull-query}])
