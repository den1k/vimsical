(ns vimsical.queries.branch
  (:require
   [vimsical.queries.file :as file]
   [vimsical.queries.lib :as lib]
   [vimsical.queries.owner :as owner]
   [vimsical.vcs.branch :as branch]))

(def pull-query
  [:db/uid
   ::branch/name
   ::branch/start-delta-uid
   ::branch/branch-off-delta-uid
   ::branch/created-at
   {::branch/owner owner/pull-query}
   {::branch/parent '...}
   {::branch/files file/pull-query}
   {::branch/libs lib/pull-query}])

(def datomic-pull-query pull-query)
