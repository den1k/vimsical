(ns vimsical.backend.components.delta-store.schema
  (:require
   [clojure.spec :as s]
   [vimsical.backend.adapters.cassandra.cql :as cql]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.delta :as delta]
   [vimsical.vcs.file :as file]))

;;
;; * Schema
;;

(def schema
  [(cql/create-table
    :delta
    [[:vims_uid    :uuid]
     [:branch_uid  :uuid]
     [:ts          :timeuuid]
     [:file_uid    :uuid]
     [:uid         :uuid]
     [:prev_uid    :uuid]
     [:op          :blob]
     [:pad         :bigint]
     [:meta        :blob]
     [:primary-key [[:vims_uid] :ts :branch_uid :uid]]])])

;;
;; * Specs
;;

;;
;; ** Columns
;;

;; *** External

(s/def ::uuid uuid?)
(s/def ::vims-uid ::uuid)
(s/def ::branch-uid ::branch/uid)
(s/def ::file-uid ::file/uid)
(s/def ::uid ::delta/uid)
(s/def ::prev-uid ::delta/prev-uid)
(s/def ::pad ::delta/pad)
(s/def ::op ::delta/op)
(s/def ::meta ::delta/meta)

;; *** Internal

(s/def ::vims_uid ::vims-uid)
(s/def ::branch_uid ::branch-uid)
(s/def ::file_uid ::file-uid)
(s/def ::prev_uid ::prev-uid)

;;
;; ** Partition and primary keys
;;

(s/def ::partition-key (s/keys :req-un [::vims-uid]))
(s/def ::primary-key (s/merge ::partition-key (s/keys :req-un [::branch-uid ::uid])))

;;
;; ** Tables
;;

(s/def ::select-row (s/keys :req-un [::branch-uid ::file-uid ::uid ::prev-uid ::pad ::op ::meta]))
(s/def ::insert-row (s/merge (s/keys :req-un [::vims-uid]) ::select-row))