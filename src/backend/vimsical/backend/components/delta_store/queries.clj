(ns vimsical.backend.components.delta-store.queries
  (:require
   [qbits.alia.codec.nippy :as nippy]
   [qbits.hayt :as cql]
   [vimsical.backend.adapters.cassandra.util :as util]))

;;
;; * Values
;;

(defn- delta->serializable-delta
  [delta]
  (-> delta
      (update :op nippy/serializable!)
      (update :meta nippy/serializable!)))

(defn- serializable-delta->values
  [delta vims-uid]
  (merge
   (util/hyphens->underscores delta)
   {:vims_uid vims-uid}))

(defn delta->insert-values
  ([vims-uid]
   (fn [delta]
     (delta->insert-values vims-uid delta)))
  ([vims-uid delta]
   (-> delta
       (delta->serializable-delta)
       (serializable-delta->values vims-uid))))

;;
;; * Queries
;;

(def select-deltas
  (cql/select
   :delta
   (cql/columns :branch_uid :file_uid :uid :prev_uid :pad :op :meta)
   (cql/where
    [[= (cql/token :vims_uid) (cql/token cql/?)]])))

(assert (string? (pr-str (cql/->raw select-deltas))))

(def select-deltas-by-branch-uid
  (cql/select
   :delta
   (cql/columns :branch_uid :uid :prev_uid)
   (cql/where
    [[= (cql/token :vims_uid) (cql/token cql/?)]])))

;;
;; * Commands
;;

(def insert-delta
  (cql/insert
   :delta
   (cql/values
    {"vims_uid"   :vims_uid
     "ts"         (cql/now)
     "branch_uid" :branch_uid
     "file_uid"   :file_uid
     "uid"        :uid
     "prev_uid"   :prev_uid
     "op"         :op
     "pad"        :pad
     "meta"       :meta})))

(assert (string? (cql/->raw insert-delta)))

