(ns vimsical.frontend.vcs.queries
  (:require
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.core :as vcs]))

(def branch
  ['* {::branch/files ['*]}])

(def vims
  [:db/id {:vims/branches branch}])

(def vcs
  ['* {::vcs/branches branch}])

(def vims-vcs
  [:db/id {:vims/vcs vcs}])
