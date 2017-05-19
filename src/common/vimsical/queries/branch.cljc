(ns vimsical.queries.branch
  (:require
   [vimsical.queries.file :as file]
   [vimsical.queries.lib :as lib]
   [vimsical.vcs.branch :as branch]))

(def pull-query
  [:db/uid
   ::branch/name
   ::branch/owner [:db/uid]
   ::branch/start-delta-uid
   ::branch/branch-off-delta-uid
   ::branch/created-at
   {::branch/parent '...}
   {::branch/files file/pull-query}
   {::branch/libs lib/pull-query}])
