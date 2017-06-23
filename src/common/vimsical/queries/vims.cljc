(ns vimsical.queries.vims
  (:require
   [vimsical.queries.branch :as branch]
   [vimsical.queries.owner :as owner]
   [vimsical.queries.snapshot :as snapshot]
   [vimsical.vcs.core :as vcs]
   [vimsical.vims :as vims]))

(def pull-query
  [:db/uid
   ::vims/created-at
   ::vims/title
   ::vims/cast
   {::vims/owner owner/pull-query}
   {::vims/branches branch/pull-query}])

(def frontend-pull-query
  [:db/uid
   ::vims/created-at
   ::vims/title
   ::vims/cast
   {::vims/vcs ['* {::vcs/branches branch/pull-query}]}
   {::vims/owner owner/pull-query}
   {::vims/snapshots snapshot/pull-query}
   {::vims/branches branch/pull-query}])

(def datomic-pull-query
  [:db/uid
   ::vims/created-at
   ::vims/title
   ::vims/cast
   {::vims/owner owner/pull-query}
   {::vims/branches branch/datomic-pull-query}])
