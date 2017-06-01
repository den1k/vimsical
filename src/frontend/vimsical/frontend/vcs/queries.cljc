(ns vimsical.frontend.vcs.queries
  ;; TODO remove these and user vimsical.queries/...
  (:require
   [vimsical.vims :as vims]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.file :as file]
   [vimsical.vcs.core :as vcs]))

(def file
  ['* {::file/compiler ['*]}])

(def branch
  ['*
   {::branch/files file}
   {::branch/libs ['*]}
   {::branch/owner [:db/uid]}
   ;; XXX need recursive queries implemented in mapgraph
   {::branch/parent ['*
                     {::branch/files file}
                     {::branch/libs ['*]}]}])

(def vims
  [:db/uid
   {::vims/owner [:db/uid]}
   {::vims/branches branch}])

(def vcs
  ['* {::vcs/branches branch}])

(def vims-vcs
  [:db/uid {::vims/vcs vcs}])
