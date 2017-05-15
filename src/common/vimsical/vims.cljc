(ns vimsical.vims
  (:require
   [clojure.spec :as s]
   [vimsical.vcs.branch :as branch]))

(s/def ::title string?)
(s/def ::cast boolean?)
;; NOTE circular dependency
(s/def :db/id uuid?)
(s/def ::owner (s/keys :req [:db/id]))
(s/def ::branches (s/every ::branch/branch.))
;; NOTE branch owner is not defined in vcs
(s/def ::branch/owner ::owner)
