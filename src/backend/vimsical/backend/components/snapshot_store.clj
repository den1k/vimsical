(ns vimsical.backend.components.snapshot-store
  (:require
   [clojure.spec :as s]
   [com.stuartsierra.component :as cp]
   [vimsical.backend.adapters.cassandra.protocol :as cassandra]
   [vimsical.backend.adapters.cassandra.util :as cassandra.util]
   [vimsical.backend.components.snapshot-store.protocol :as protocol]
   [vimsical.backend.components.snapshot-store.queries :as queries]
   [vimsical.backend.components.snapshot-store.schema :as schema]
   [vimsical.common.util.core :as util]
   [vimsical.vcs.snapshot :as snapshot]))

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

(defn- insert-snapshots-commands [snapshots]
  (mapv
   (fn [snapshot]
     [::insert-snapshot (queries/snapshot->insert-values snapshot)])
   snapshots))

;;
;; * Results
;;

(def result-xf
  (comp
   (map cassandra.util/underscores->hyphens)
   (map (util/qualify-keys (namespace ::snapshot/snapshot)))))

(def default-options
  {:result-set-fn #(into [] result-xf %)})

(defn new-options [options] (merge default-options options))

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
      (cassandra/execute-async cassandra command success error default-options)))
  (insert-snapshots-async [_ snapshots success error]
    (let [commands (insert-snapshots-commands snapshots)]
      (cassandra/execute-batch-async cassandra commands :unlogged success error default-options)))

  protocol/ISnapshotStoreChan
  (select-snapshots-chan [this user-uid vims-uid]
    (protocol/select-snapshots-chan this user-uid vims-uid nil))
  (select-snapshots-chan [_ user-uid vims-uid options]
    (let [command  (select-snapshots-command user-uid vims-uid)
          options' (new-options options)]
      (cassandra/execute-chan cassandra command options')))

  (insert-snapshots-chan [this snapshots]
    (protocol/insert-snapshots-chan this snapshots nil))
  (insert-snapshots-chan [_ snapshots options]
    (let [commands (insert-snapshots-commands snapshots)
          options' (new-options options)]
      (cassandra/execute-batch-chan cassandra commands :unlogged options'))))

(defn ->snapshot-store [] (map->SnapshotStore {}))

(s/def ::snapshot-store (fn [x] (and x (instance? SnapshotStore x))))
