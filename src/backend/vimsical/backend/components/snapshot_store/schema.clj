(ns vimsical.backend.components.snapshot-store.schema
  (:require
   [clojure.spec :as s]
   [vimsical.backend.adapters.cassandra.cql :as cql]
   [vimsical.user :as user]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.delta :as delta]
   [vimsical.vcs.file :as file]
   [vimsical.vims :as vims]))

;;
;; * Spec
;;

(s/def ::user_uid ::user/uid)
(s/def ::vims_uid ::vims/uid)
(s/def ::branch_uid ::branch/uid)
(s/def ::file_uid ::file/uid)
(s/def ::delta_uid ::delta/uid)
(s/def ::text string?)

;;
;; * Schema
;;

(def schema
  [(cql/create-table
    :snapshot
    [[:user_uid    :uuid]
     [:vims_uid    :uuid]
     [:file_uid    :uuid]
     [:delta_uid   :uuid]
     [:text        :text]
     [:primary-key
      [[:user_uid  :vims_uid] :file_uid]]])])
