(ns vimsical.backend.components.delta-store
  (:require
   [com.stuartsierra.component :as cp]
   [vimsical.backend.adapters.cassandra.protocol :as cassandra]
   [vimsical.backend.components.delta-store.protocol :as protocol]
   [vimsical.backend.components.delta-store.schema :as schema]
   [vimsical.backend.components.delta-store.queries :as queries]
   [clojure.spec :as s]))

;;
;; * Queries
;;

(def ^:private queries
  {::select-deltas queries/select-deltas
   ::insert-delta  queries/insert-delta})

;;
;; * Commands
;;

(defn- select-deltas-command [user-id vims-id]
  [::select-deltas [user-id vims-id]])

(defn- insert-deltas-commands [user-id vims-id deltas]
  (mapv
   (fn [delta]
     [::insert-delta (queries/delta->insert-values user-id vims-id delta)])
   deltas))

;;
;; * Component
;;

(defrecord DeltaStore [cassandra]
  cp/Lifecycle
  (start [this]
    (do
      ;; Idempotent operation, useful to do this on startup so we don't have to
      ;; worry about installing the schema in ops or testing
      (cassandra/create-schema! cassandra schema/schema)
      ;; Update cassandra with prepared queries
      (update this :cassandra cassandra/prepare-queries queries)))
  (stop  [this] this)

  protocol/IDeltaStoreAsync
  (select-deltas-async [_ user-id vims-id success error]
    (let [command (select-deltas-command user-id vims-id)]
      (cassandra/execute-async cassandra command success error)))
  (insert-deltas-async [_ user-id vims-id deltas success error]
    (let [commands (insert-deltas-commands user-id vims-id deltas)]
      (cassandra/execute-batch-async cassandra commands :unlogged success error)))

  protocol/IDeltaStoreChan
  (select-deltas-chan [_ user-id vims-id]
    (let [command (select-deltas-command user-id vims-id)]
      (cassandra/execute-chan cassandra command)))
  (insert-deltas-chan [_ user-id vims-id deltas]
    (let [commands (insert-deltas-commands user-id vims-id deltas)]
      (cassandra/execute-batch-chan cassandra commands :unlogged))))

(defn ->delta-store [] (map->DeltaStore {}))

(s/def ::delta-store (fn [x] (and x (instance? DeltaStore x))))
