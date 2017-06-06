(ns vimsical.backend.components.snapshot-store
  (:require
   [clojure.spec.alpha :as s]
   [com.stuartsierra.component :as cp]
   [vimsical.backend.adapters.cassandra.protocol :as cassandra]
   [vimsical.backend.adapters.cassandra.util :as cassandra.util]
   [vimsical.backend.components.snapshot-store.protocol :as protocol]
   [vimsical.backend.components.snapshot-store.queries :as queries]
   [vimsical.backend.components.snapshot-store.schema :as schema]
   [vimsical.common.util.core :as util]
   [vimsical.vcs.snapshot :as snapshot]
   [vimsical.user :as user]
   [vimsical.vims :as vims]))

;;
;; * Queries
;;

(def ^:private queries
  {::select-user-snapshots queries/select-user-snapshots
   ::select-vims-snapshots queries/select-vims-snapshots
   ::insert-snapshot       queries/insert-snapshot})

;;
;; * Commands
;;

(s/fdef select-user-snapshots-command :args (s/cat :user-uid ::user/uid))
(defn-  select-user-snapshots-command [user-uid] [::select-user-snapshots [user-uid]])

(s/fdef select-vims-snapshots-command :args (s/cat :user-uid ::user/uid :vims-user ::vims/uid))
(defn-  select-vims-snapshots-command [user-uid vims-uid] [::select-vims-snapshots [user-uid vims-uid]])

(s/fdef insert-snapshots-commands :args (s/cat :snapshots (s/every ::snapshot/snapshot)))
(defn-  insert-snapshots-commands [snapshots]
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
  (select-user-snapshots-async [_ user-uid success error]
    (let [command (select-user-snapshots-command user-uid)]
      (cassandra/execute-async cassandra command success error default-options)))
  (select-vims-snapshots-async [_ user-uid vims-uid success error]
    (let [command (select-vims-snapshots-command user-uid vims-uid)]
      (cassandra/execute-async cassandra command success error default-options)))
  (insert-snapshots-async [_ snapshots success error]
    (let [commands (insert-snapshots-commands snapshots)]
      (cassandra/execute-batch-async cassandra commands :unlogged success error default-options)))

  protocol/ISnapshotStoreChan
  (select-user-snapshots-chan [this user-uid]
    (protocol/select-user-snapshots-chan this user-uid nil))
  (select-user-snapshots-chan [_ user-uid options]
    (let [command  (select-user-snapshots-command user-uid)
          options' (new-options options)]
      (cassandra/execute-chan cassandra command options')))

  (select-vims-snapshots-chan [this user-uid vims-uid]
    (protocol/select-vims-snapshots-chan this user-uid vims-uid nil))
  (select-vims-snapshots-chan [_ user-uid vims-uid options]
    (let [command  (select-vims-snapshots-command user-uid vims-uid)
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
