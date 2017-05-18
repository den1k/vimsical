(ns vimsical.vims
  (:require
   [clojure.spec :as s]
   [vimsical.vcs.branch :as branch]))

(s/def ::uid uuid?)
(s/def :db/uid ::uid)
(s/def ::title string?)
(s/def ::cast boolean?)
;; NOTE branch owner is not defined in vcs
(s/def ::owner (s/keys :req [:db/uid]))
(s/def ::branch/owner ::owner)
(s/def ::branch (s/merge ::branch/branch (s/keys :req [::branch/owner])))
(s/def ::branches (s/every ::branch))

(s/def ::vims
  (s/keys :req [:db/uid ::owner ::branches] :opt [::title ::cast]))
