(ns vimsical.backend.components.snapshot-store
  (:require
   [com.stuartsierra.component :as cp]
   [vimsical.backend.adapters.cassandra.protocol :as cassandra]
   [vimsical.backend.components.snapshot-store.protocol :as protocol]
   [vimsical.backend.components.snapshot-store.schema :as schema]
   [vimsical.backend.components.snapshot-store.queries :as queries]
   [vimsical.common.util.core :as util]
   [vimsical.vcs.snapshot :as snapshot]
   [vimsical.backend.adapters.cassandra.util :as cassandra.util]
   [clojure.spec :as s]))

;;
;; * Queries
;;

(def ^:private queries
  {::select-snapshots queries/select-snapshots
   ::insert-snapshot  queries/insert-snapshot})

;;
;; * Commands
;;

(defn- select-snapshots-command [user-uid vims-uid]
  [::select-snapshots [user-uid vims-uid]])

(defn- insert-snapshots-commands [user-uid vims-uid snapshots]
  (mapv
   (fn [snapshot]
     [::insert-snapshot (queries/snapshot->insert-values user-uid vims-uid snapshot)])
   snapshots))

;;
;; * Results
;;

(def result-xf
  (comp
   (map cassandra.util/underscores->hyphens)
   (map (util/qualify-keys (namespace ::snapshot/snapshot)))))

(def options
  {:result-set-fn #(into [] result-xf %)})

;;
;; * Component
;;

(defrecord SnapshotStore [cassandra]
  cp/Lifecycle
  (start [this]
    (do
      ;; Idempotent operation, useful to do this on startup so we don't have to
      ;; worry about installing the schema in ops or testing
      (cassandra/create-schema! cassandra schema/schema)
      ;; Update cassandra with prepared queries
      (update this :cassandra cassandra/prepare-queries queries)))
  (stop  [this] this)

  protocol/ISnapshotStoreAsync
  (select-snapshots-async [_ user-uid vims-uid success error]
    (let [command (select-snapshots-command user-uid vims-uid)]
      (cassandra/execute-async cassandra command success error options)))
  (insert-snapshots-async [_ user-uid vims-uid snapshots success error]
    (let [commands (insert-snapshots-commands user-uid vims-uid snapshots)]
      (cassandra/execute-batch-async cassandra commands :unlogged success error options)))

  protocol/ISnapshotStoreChan
  (select-snapshots-chan [_ user-uid vims-uid]
    (let [command (select-snapshots-command user-uid vims-uid)]
      (cassandra/execute-chan cassandra command options)))
  (insert-snapshots-chan [_ user-uid vims-uid snapshots]
    (let [commands (insert-snapshots-commands user-uid vims-uid snapshots)]
      (cassandra/execute-batch-chan cassandra commands :unlogged options))))

(defn ->snapshot-store [] (map->SnapshotStore {}))

(s/def ::snapshot-store (fn [x] (and x (instance? SnapshotStore x))))
