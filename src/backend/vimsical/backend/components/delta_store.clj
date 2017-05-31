(ns vimsical.backend.components.delta-store
  (:require
   [clojure.core.async :as a]
   [clojure.core.async.impl.protocols :as ap]
   [clojure.spec :as s]
   [com.stuartsierra.component :as cp]
   [vimsical.backend.adapters.cassandra.protocol :as cassandra]
   [vimsical.backend.components.delta-store.protocol :as protocol]
   [vimsical.backend.components.delta-store.queries :as queries]
   [vimsical.backend.components.delta-store.schema :as schema]
   [vimsical.vcs.validation :as validation]
   [vimsical.vims :as vims]
   [vimsical.vcs.delta :as delta]))

;;
;; * Queries
;;

(def ^:private queries
  {::select-deltas              queries/select-deltas
   ::insert-delta               queries/insert-delta
   ::select-delta-by-branch-uid queries/select-delta-by-branch-uid})

;;
;; * Commands
;;

(s/fdef select-deltas-command :args (s/cat :vims-uid ::vims/uid))
(defn-  select-deltas-command [vims-uid]
  [::select-deltas [vims-uid]])

(s/fdef insert-deltas-command :args (s/cat :vims-uid ::vims/uid :deltas (s/every ::delta/delta)))
(defn-  insert-deltas-commands [vims-uid deltas]
  (mapv
   (fn [delta]
     [::insert-delta (queries/delta->insert-values vims-uid delta)])
   deltas))

(s/fdef select-delta-by-branch-uid-command :args (s/cat :vims-uid ::vims/uid))
(defn-  select-delta-by-branch-uid-command [vims-uid]
  [::select-delta-by-branch-uid [vims-uid]])

;;
;; * Result sets helpers
;;

(defn group-by-branch-uid-chan
  [buf-or-n]
  (let [in  (a/chan buf-or-n (validation/group-by-branch-uid-xf) identity)
        out (a/reduce (validation/group-by-branch-uid-rf) {} in)]
    (reify
      ap/ReadPort
      (take! [_ handler]
        (ap/take! out handler))
      ap/WritePort
      (put! [_ value handler]
        (ap/put! in value handler))
      ap/Channel
      (ap/close! [_]
        (a/close! in))
      (ap/closed? [_]
        (ap/closed? in)))))

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
  (select-delta-by-branch-uid-async [_ vims-uid success error]
    (let [command  (select-delta-by-branch-uid-command vims-uid)
          group    (fn [deltas]
                     (transduce
                      (validation/group-by-branch-uid-xf)
                      (validation/group-by-branch-uid-rf)
                      {} deltas))
          success' (comp success group)]
      (cassandra/execute-async cassandra command success' error)))

  protocol/IDeltaStoreChan
  (select-deltas-chan [_ vims-uid]
    (let [command (select-deltas-command vims-uid)]
      (cassandra/execute-chan cassandra command)))
  (insert-deltas-chan [_ vims-uid deltas]
    (let [commands (insert-deltas-commands vims-uid deltas)]
      (cassandra/execute-batch-chan cassandra commands :unlogged)))
  (select-delta-by-branch-uid-chan [{{buf :default-fetch-size} :cassandra} vims-uid]
    (let [command (select-delta-by-branch-uid-command vims-uid)
          channel (group-by-branch-uid-chan buf)]
      (cassandra/execute-chan cassandra command {:channel channel}))))

(defn ->delta-store [] (map->DeltaStore {}))

(s/def ::delta-store (fn [x] (and x (instance? DeltaStore x))))
