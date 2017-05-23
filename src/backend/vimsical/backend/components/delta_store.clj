(ns vimsical.backend.components.delta-store
  (:require
   [com.stuartsierra.component :as cp]
   [vimsical.backend.adapters.cassandra.protocol :as cassandra]
   [vimsical.backend.components.delta-store.protocol :as protocol]
   [vimsical.backend.components.delta-store.schema :as schema]
   [vimsical.backend.components.delta-store.queries :as queries]
   [vimsical.backend.components.delta-store.validation :as validation]
   [clojure.spec :as s]
   [net.cgrand.xforms :as x]
   [clojure.core.async :as a]))

;;
;; * Queries
;;

(def ^:private queries
  {::select-deltas               queries/select-deltas
   ::insert-delta                queries/insert-delta
   ::select-deltas-by-branch-uid queries/select-deltas-by-branch-uid})

;;
;; * Commands
;;

(defn- select-deltas-command [vims-uid]
  [::select-deltas [vims-uid]])

(defn- insert-deltas-commands [vims-uid deltas]
  (mapv
   (fn [delta]
     [::insert-delta (queries/delta->insert-values vims-uid delta)])
   deltas))

(defn- select-deltas-by-branch-uid-command [vims-uid]
  [::select-deltas-by-branch-uid [vims-uid]])

;;
;; * Result sets helpers
;;



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
  (select-deltas-async [_ vims-uid success error]
    (let [command (select-deltas-command vims-uid)]
      (cassandra/execute-async cassandra command success error)))
  (insert-deltas-async [_ vims-uid deltas success error]
    (let [commands (insert-deltas-commands vims-uid deltas)]
      (cassandra/execute-batch-async cassandra commands :unlogged success error)))
  (select-deltas-by-branch-uid-async [_ vims-uid success error]
    (let [command  (select-deltas-by-branch-uid-command vims-uid)
          group    (fn [deltas]
                     (transduce
                      (validation/group-by-branch-uid-xf)
                      (validation/group-by-branch-uid-rf)
                      deltas))
          success' (comp success group)]
      (cassandra/execute-async cassandra command success' error)))

  protocol/IDeltaStoreChan
  (select-deltas-chan [_ vims-uid]
    (let [command (select-deltas-command vims-uid)]
      (cassandra/execute-chan cassandra command)))
  (insert-deltas-chan [_ vims-uid deltas]
    (let [commands (insert-deltas-commands vims-uid deltas)]
      (cassandra/execute-batch-chan cassandra commands :unlogged)))
  (select-deltas-by-branch-uid-chan [_ vims-uid]
    (let [command (select-deltas-by-branch-uid-command vims-uid)
          channel (a/chan 1024 (validation/group-by-branch-uid-rf))
          options {:channel channel}]
      (cassandra/execute-chan cassandra command options))))

(defn ->delta-store [] (map->DeltaStore {}))

(s/def ::delta-store (fn [x] (and x (instance? DeltaStore x))))
