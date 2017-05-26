(ns vimsical.queries.vims
  (:require
   [vimsical.vims :as vims]
   [vimsical.queries.branch :as branch]
   [vimsical.vcs.core :as vcs]))

(def pull-query
  [:db/uid
   ::vims/title
   ::vims/cast
   {::vims/owner [:db/uid]}
   {::vims/branches branch/pull-query}])

(def frontend-pull-query
  [:db/uid
   ::vims/title
   ::vims/cast
   {::vims/vcs   ['* {::vcs/branches branch/pull-query}]}
   {::vims/owner [:db/uid]}
   {::vims/branches branch/pull-query}])

(def datomic-pull-query
  [:db/uid
   ::vims/title
   ::vims/cast
   {::vims/owner [:db/uid]}
   {::vims/branches branch/datomic-pull-query}])
