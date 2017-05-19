(ns vimsical.queries.compiler
  (:require
   [vimsical.vcs.compiler :as compiler]))

(def pull-query
  [:db/uid
   ::compiler/type
   ::compiler/sub-type
   ::compiler/to-sub-type
   ::compiler/name])
