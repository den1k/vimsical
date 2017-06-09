(ns vimsical.backend.components.delta-store.queries
  (:require
   [clojure.spec.alpha :as s]
   [qbits.alia.codec.nippy :as nippy]
   [qbits.hayt :as cql]
   [vimsical.backend.adapters.cassandra.util :as util]
   [vimsical.backend.components.delta-store.schema :as schema]
   [vimsical.vcs.delta :as delta]))

;;
;; * Values
;;

(defn- delta->serializable-delta
  [delta]
  (-> delta
      (update :op nippy/serializable!)
      (update :meta nippy/serializable!)))

(defn- serializable-delta->values
  [delta vims-uid user-uid row-order]
  (-> delta
      (update :pad int)
      (util/hyphens->underscores)
      (merge {:vims_uid vims-uid :user_uid user-uid :row_order row-order})))

(s/fdef delta->insert-row
        :args (s/cat
               :vims-uid ::schema/vims-uid
               :user-uid ::schema/user-uid
               :row-order ::schema/row-order
               :delta ::delta/delta)
        :ret  ::schema/insert-row)

(defn delta->insert-row
  [vims-uid user-uid row-order delta]
  (-> delta
      (delta->serializable-delta)
      (serializable-delta->values vims-uid user-uid row-order)))

;;
;; * Queries
;;

(def select-deltas
  (cql/select
   :delta
   (cql/columns :branch_uid :file_uid :uid :prev_uid :pad :op :meta)
   (cql/where [[= :vims_uid cql/?]])
   (cql/allow-filtering)))

(assert (string? (pr-str (cql/->raw select-deltas))))

(def select-vims-session
  (cql/select
   :delta
   (cql/columns :branch_uid :uid :prev_uid)
   (cql/where
    [[= (cql/token :vims_uid :user_uid) (cql/token cql/? cql/?)]])))

(assert (string? (pr-str (cql/->raw select-vims-session))))

;;
;; * Commands
;;

(def insert-delta
  (cql/insert
   :delta
   (cql/values
    {"vims_uid"   :vims_uid
     "user_uid"   :user_uid
     "row_order"  :row_order
     "branch_uid" :branch_uid
     "file_uid"   :file_uid
     "uid"        :uid
     "prev_uid"   :prev_uid
     "op"         :op
     "pad"        :pad
     "meta"       :meta})))

(assert (string? (cql/->raw insert-delta)))

