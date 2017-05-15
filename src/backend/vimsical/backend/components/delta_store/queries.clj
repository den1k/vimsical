(ns vimsical.backend.components.delta-store.queries
  (:require
   [qbits.alia.codec :as codec]
   [qbits.alia.codec.nippy :as nippy]
   [qbits.hayt :as cql]
   [vimsical.backend.adapters.cassandra.util :as util]))

;;
;; * Deltas
;;

;;
;; ** Queries
;;

(def select-deltas
  (cql/select
   :delta
   (cql/columns :branch_id :file_id :id :prev_id :pad :op :meta)
   (cql/where
    [[= (cql/token :user_id :vims_id) (cql/token cql/? cql/?)]])))

(assert (string? (pr-str (cql/->raw select-deltas))))

(def insert-delta
  (cql/insert
   :delta
   (cql/values
    {"user_id"   :user_id
     "vims_id"   :vims_id
     "ts"        (cql/now)
     "branch_id" :branch_id
     "file_id"   :file_id
     "id"        :id
     "prev_id"   :prev_id
     "op"        :op
     "pad"       :pad
     "meta"      :meta})))

(assert (string? (cql/->raw insert-delta)))

;;
;; ** Values
;;

(defn- delta->serializable-delta
  [delta]
  (-> delta
      (update :op nippy/serializable!)
      (update :meta nippy/serializable!)))

(defn- serializable-delta->values
  [delta user-id vims-id]
  (merge
   (util/hyphens->underscores delta)
   {:user_id user-id :vims_id vims-id}))

(defn delta->insert-values
  ([user-id vims-id]
   (fn [delta]
     (-> delta
         (delta->serializable-delta)
         (serializable-delta->values user-id vims-id))))
  ([user-id vims-id delta]
   (-> delta
       (delta->serializable-delta)
       (serializable-delta->values user-id vims-id))))

;;
;; * Files previews
;;

(defn insert-file-preview
  [file-preview]
  (cql/insert :file_preview (cql/values file-preview)))

(defn select-file-preview
  [user-id vims-id]
  (cql/select
   :file_preview
   (cql/where
    [['= (cql/token :user_id :vims_id) (cql/token user-id vims-id)]])))
