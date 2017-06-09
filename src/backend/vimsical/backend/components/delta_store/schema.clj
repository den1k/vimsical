(ns vimsical.backend.components.delta-store.schema
  (:require
   [clojure.spec.alpha :as s]
   [vimsical.backend.adapters.cassandra.cql :as cql]
   [vimsical.user :as user]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.delta :as delta]
   [vimsical.vcs.file :as file])
  (:import
   qbits.alia.codec.nippy.NippySerializable))

;;
;; * Schema
;;

(def schema
  [(cql/create-table
    :delta
    [[:vims_uid    :uuid]
     [:user_uid    :uuid]
     [:branch_uid  :uuid]
     [:row_order   :bigint]
     [:file_uid    :uuid]
     [:uid         :uuid]
     [:prev_uid    :uuid]
     [:op          :blob]
     [:pad         :int]
     [:meta        :blob]
     [:primary-key [[:vims_uid :user_uid] :branch_uid :row_order]]])])

;;
;; * Specs
;;

;;
;; ** Columns
;;

;; *** External

(s/def ::uuid uuid?)
(s/def ::vims-uid ::uuid)
(s/def ::user-uid ::user/uid)
(s/def ::row-order nat-int?)
(s/def ::branch-uid ::branch/uid)
(s/def ::file-uid ::file/uid)
(s/def ::uid ::delta/uid)
(s/def ::prev-uid ::delta/prev-uid)
(s/def ::pad ::delta/pad)
(s/def ::op ::delta/op)
(s/def ::meta ::delta/meta)

;; *** Internal

(s/def ::vims_uid ::vims-uid)
(s/def ::user_uid ::user-uid)
(s/def ::branch_uid ::branch-uid)
(s/def ::row_order ::row-order)
(s/def ::file_uid ::file-uid)
(s/def ::prev_uid ::prev-uid)

(s/def ::serializable (fn [x] (and x (instance? NippySerializable x))))

(s/def :vimsical.backend.components.delta-store.schema.serializable/op ::serializable)
(s/def :vimsical.backend.components.delta-store.schema.serializable/meta ::serializable)

;;
;; ** Partition and primary keys
;;

(s/def ::partition-key (s/keys :req-un [::vims-uid ::user-uid]))
(s/def ::primary-key (s/merge ::partition-key (s/keys :req-un [::row-order ::branch-uid ::uid])))

;;
;; ** Tables
;;

(s/def ::select-row
  (s/keys :req-un [::branch-uid ::file-uid ::uid ::prev-uid ::pad ::op ::meta]))

(s/def ::insert-row
  (s/keys :req-un [::vims_uid ::user_uid ::branch_uid ::row_order ::file_uid ::uid ::prev_uid ::pad
                   :vimsical.backend.components.delta-store.schema.serializable/op
                   :vimsical.backend.components.delta-store.schema.serializable/meta]))
