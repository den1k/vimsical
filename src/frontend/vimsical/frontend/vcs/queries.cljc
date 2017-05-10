(ns vimsical.frontend.vcs.queries
  (:require
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.file :as file]
   [vimsical.vcs.core :as vcs]))

(def file
  ['* {::file/compiler ['*]}])

(def branch
  ['* {::branch/files file} {::branch/libs ['*]}])

(def vims
  [:db/id {:vims/branches branch}])

(def vcs
  ['* {::vcs/branches branch}])

(def vims-vcs
  [:db/id {:vims/vcs vcs}])
