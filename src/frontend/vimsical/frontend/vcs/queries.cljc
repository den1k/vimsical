(ns vimsical.frontend.vcs.queries
  (:require
   [vimsical.vims :as vims]
   [vimsical.vcs.core :as vcs]
   [vimsical.queries.branch :as branch]
   [vimsical.queries.file :as file]))

(def file file/pull-query)

(def vims
  [:db/uid
   {::vims/owner [:db/uid]}
   {::vims/branches branch/pull-query}])

(def vcs
  ['* {::vcs/branches branch/pull-query}])

(def vims-vcs
  [:db/uid {::vims/vcs vcs}])
