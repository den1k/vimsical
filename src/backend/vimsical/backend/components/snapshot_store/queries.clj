(ns vimsical.backend.components.snapshot-store.queries
  (:require
   [qbits.hayt :as cql]
   [vimsical.backend.adapters.cassandra.util :as util]
   [vimsical.common.util.core :as common.util]))

;;
;; * Values
;;

(defn snapshot->insert-values
  [snapshot]
  (-> snapshot (common.util/unqualify-keys) (util/hyphens->underscores)))

;;
;; * Queries
;;

(def select-snapshots
  (cql/select
   :snapshot
   (cql/columns :user_uid :vims_uid :file_uid :delta_uid :text)
   (cql/where
    [[= (cql/token :user_uid :vims_uid) (cql/token cql/? cql/?)]])))

(assert (string? (pr-str (cql/->raw select-snapshots))))

(def insert-snapshot
  (cql/insert
   :snapshot
   (cql/values
    {"user_uid"  :user_uid
     "vims_uid"  :vims_uid
     "file_uid"  :file_uid
     "delta_uid" :delta_uid
     "text"      :text})))

(assert (string? (cql/->raw insert-snapshot)))
