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
    [[:user_id     :uuid]
     [:vims_id     :uuid]
     [:branch_id   :uuid]
     [:ts          :timeuuid]
     [:file_id     :uuid]
     [:id          :uuid]
     [:prev_id     :uuid]
     [:op          :blob]
     [:pad         :bigint]
     [:meta        :blob]
     [:primary-key
      [[:user_id :vims_id] :ts :branch_id :id]]])
   (cql/create-table
    :file_preview
    [[:user_id     :uuid]
     [:vims_id     :uuid]
     [:branch_id   :uuid]
     [:file_id     :uuid]
     [:text        :text]
     [:primary-key
      [[:user_id :vims_id] :branch_id :file_id]]])])

;;
;; * Specs
;;

;;
;; ** Columns
;;

;; *** External

(s/def ::uuid uuid?)
(s/def ::user-id ::uuid)
(s/def ::vims-id ::uuid)
(s/def ::branch-id ::branch/id)
(s/def ::file-id ::file/id)
(s/def ::id ::delta/id)
(s/def ::prev-id ::delta/prev-id)
(s/def ::pad ::delta/pad)
(s/def ::op ::delta/op)
(s/def ::meta ::delta/meta)

;; *** Internal

(s/def ::user_id ::user-id)
(s/def ::vims_id ::vims-id)
(s/def ::branch_id ::branch-id)
(s/def ::file_id ::file-id)
(s/def ::prev_id ::prev-id)

;;
;; ** Partition and primary keys
;;

(s/def ::partition-key (s/keys :req-un [::user-id ::vims-id]))
(s/def ::primary-key (s/merge ::partition-key (s/keys :req-un [::branch-id ::id])))

;;
;; ** Tables
;;

(s/def ::select-row (s/keys :req-un [::branch-id ::file-id ::id ::prev-id ::prev-same-id ::pad ::op ::meta]))
(s/def ::insert-row (s/merge (s/keys :req-un [::user-id ::vims-id]) ::select-row))
