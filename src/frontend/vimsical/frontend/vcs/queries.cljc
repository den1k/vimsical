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
  [:db/uid {:vims/branches branch}])

(def vcs
  ['* {::vcs/branches branch}])

(def vims-vcs
  [:db/uid {:vims/vcs vcs}])
