(ns vimsical.queries.snapshot
  (:require
   [vimsical.vcs.snapshot :as snapshot]))

(def pull-query
  [::snapshot/user-uid
   ::snapshot/vims-uid
   ::snapshot/file-uid
   ::snapshot/text
   ::snapshot/delta-uid])
