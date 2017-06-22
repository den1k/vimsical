(ns vimsical.queries.vims
  (:require
   [vimsical.queries.branch :as branch]
   [vimsical.queries.snapshot :as snapshot]
   [vimsical.vcs.core :as vcs]
   [vimsical.vims :as vims]
   [vimsical.user :as user]))

(def owner-query
  [:db/uid
   ::user/first-name
   ::user/last-name
   ::user/email])

(def pull-query
  [:db/uid
   ::vims/created-at
   ::vims/title
   ::vims/cast
   {::vims/owner owner-query}
   {::vims/branches branch/pull-query}])

(def frontend-pull-query
  [:db/uid
   ::vims/created-at
   ::vims/title
   ::vims/cast
   {::vims/vcs ['* {::vcs/branches branch/pull-query}]}
   {::vims/owner owner-query}
   {::vims/snapshots snapshot/pull-query}
   {::vims/branches branch/pull-query}])

(def datomic-pull-query
  [:db/uid
   ::vims/created-at
   ::vims/title
   ::vims/cast
   {::vims/owner owner-query}
   {::vims/branches branch/datomic-pull-query}])
