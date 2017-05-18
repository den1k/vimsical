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
   (cql/columns :branch_uid :file_uid :uid :prev_uid :pad :op :meta)
   (cql/where
    [[= (cql/token :user_uid :vims_uid) (cql/token cql/? cql/?)]])))

(assert (string? (pr-str (cql/->raw select-deltas))))

(def insert-delta
  (cql/insert
   :delta
   (cql/values
    {"user_uid"   :user_uid
     "vims_uid"   :vims_uid
     "ts"         (cql/now)
     "branch_uid" :branch_uid
     "file_uid"   :file_uid
     "uid"        :uid
     "prev_uid"   :prev_uid
     "op"         :op
     "pad"        :pad
     "meta"       :meta})))

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
  [delta user-uid vims-uid]
  (merge
   (util/hyphens->underscores delta)
   {:user_uid user-uid :vims_uid vims-uid}))

(defn delta->insert-values
  ([user-uid vims-uid]
   (fn [delta]
     (-> delta
         (delta->serializable-delta)
         (serializable-delta->values user-uid vims-uid))))
  ([user-uid vims-uid delta]
   (-> delta
       (delta->serializable-delta)
       (serializable-delta->values user-uid vims-uid))))

;;
;; * Files previews
;;

(defn insert-file-preview
  [file-preview]
  (cql/insert :file_preview (cql/values file-preview)))

(defn select-file-preview
  [user-uid vims-uid]
  (cql/select
   :file_preview
   (cql/where
    [['= (cql/token :user_uid :vims_uid) (cql/token user-uid vims-uid)]])))
