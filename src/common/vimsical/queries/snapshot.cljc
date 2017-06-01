(ns vimsical.queries.snapshot
  (:require
   [vimsical.vcs.snapshot :as snapshot]))

(def datomic-pull-query
  [::snapshot/user-uid
   ::snapshot/vims-uid
   ::snapshot/file-uid
   ::snapshot/text
   ::snapshot/delta-uid])

(def pull-query
  (into
   datomic-pull-query
   [:db/uid
    ::snapshot/type
    ::snapshot/sub-type]))
