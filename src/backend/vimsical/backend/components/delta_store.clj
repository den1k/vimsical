(ns vimsical.backend.components.delta-store
  (:require
   [clojure.core.async :as a]
   [clojure.core.async.impl.protocols :as ap]
   [clojure.spec.alpha :as s]
   [vimsical.backend.util.log :as log]
   [com.stuartsierra.component :as cp]
   [vimsical.backend.adapters.cassandra :as cassandra]
   [vimsical.backend.adapters.cassandra.protocol :as cassandra.protocol]
   [vimsical.backend.components.delta-store.protocol :as protocol]
   [vimsical.backend.components.delta-store.queries :as queries]
   [vimsical.backend.components.delta-store.schema :as schema]
   [vimsical.backend.components.session-store :as session-store]
   [vimsical.vcs.delta :as delta]
   [vimsical.vcs.validation :as validation]))

;;
;; * Queries
;;

(def ^:private queries
  {::select-deltas            queries/select-deltas
   ::insert-delta             queries/insert-delta
   ::select-vims-session-chan queries/select-vims-session})

;;
;; * Validation
;;

(defn validate-deltas!
  [{::validation/keys [delta-by-branch-uid] :as session} deltas]
  (some? (into [] (validation/validate-deltas-xf delta-by-branch-uid) deltas)))

;;
;; * Commands
;;

(s/fdef select-deltas-command
        :args (s/cat :vims-uid ::schema/vims-uid)
        :ret  ::cassandra/command)

(defn-  select-deltas-command [vims-uid]
  [::select-deltas [vims-uid]])


(s/fdef insert-deltas-commands
        :args (s/cat :vims-uid ::schema/vims-uid
                     :user-uid ::schema/user-uid
                     :vims-session ::session-store/vims-session
                     :deltas (s/every ::delta/delta))
        :ret  (s/every ::cassandra/command))

(defn-  insert-deltas-commands
  [vims-uid
   user-uid
   {::validation/keys [order-by-branch-uid]}
   deltas]
  ;; Group the deltas by branch-uid and flatten each branch into a vector of
  ;; commands. The order of the resulting commands should match the on-disk
  ;; clustering since we iterate over the deltas in the order of the
  ;; partitioning/clustering keys
  (persistent!
   (reduce-kv
    (fn [commands! branch-uid deltas]
      (let [order  (get order-by-branch-uid branch-uid -1)
            norder (inc order)
            orders (range norder (+ norder (count deltas)))]
        (reduce-kv
         (fn [commands! order delta]
           (conj!
            commands!
            [::insert-delta (queries/delta->insert-row vims-uid user-uid order delta)]))
         commands! (zipmap orders deltas))))
    (transient [])
    (into (sorted-map) (group-by :branch-uid deltas)))))


(s/fdef select-vims-session-command
        :args (s/cat :vims-uid ::schema/vims-uid :user-uid ::schema/user-uid)
        :ret  ::cassandra/command)

(defn- select-vims-session-command [vims-uid user-uid]
  [::select-vims-session-chan [vims-uid user-uid]])

;;
;; * Session management
;;

(s/fdef session-xf :args (s/cat :session ::session-store/vims-session))

(defn session-xf [{::validation/keys [delta-by-branch-uid] :as session}]
  (validation/validate-deltas-xf delta-by-branch-uid))

(defn- session-rf []
  (completing
   (fn [m {:keys [branch-uid] :as delta}]
     (-> m
         (assoc-in  [::validation/delta-by-branch-uid branch-uid] delta)
         (update-in [::validation/order-by-branch-uid branch-uid] (fnil inc -1))))))

(s/fdef vims-session
        :args (s/cat :session ::session-store/vims-session :deltas (s/nilable (s/every ::validation/delta)))
        :ret  ::session-store/vims-session)

(defn vims-session
  [session deltas]
  (transduce
   (session-xf session)
   (session-rf)
   session deltas))

(defn vims-session-chan
  [buf-or-n]
  (let [xf  (session-xf {})
        rf  (session-rf)
        in  (a/chan buf-or-n xf identity)
        out (a/reduce rf {} in)]
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
      (cassandra.protocol/create-schema! cassandra schema/schema)
      ;; Update cassandra with prepared queries
      (update this :cassandra cassandra.protocol/prepare-queries queries)))
  (stop  [this] this)

  protocol/IDeltaStoreChan
  (select-deltas-chan [_ vims-uid]
    (let [command (select-deltas-command vims-uid)]
      (cassandra.protocol/execute-chan cassandra command)))
  (insert-deltas-chan [_ vims-uid user-uid vims-session deltas]
    (do (log/debug "Insert deltas" {:vims-uid vims-uid :user-uid user-uid :session vims-session :deltas deltas})
        (validate-deltas! vims-session deltas)
        (let [commands (insert-deltas-commands vims-uid user-uid vims-session deltas)]
          (cassandra.protocol/execute-batch-chan cassandra commands :unlogged))))
  (select-vims-session-chan [{{buf :default-fetch-size} :cassandra} vims-uid user-uid]
    (let [command (select-vims-session-command vims-uid user-uid)
          channel (vims-session-chan buf)]
      (cassandra.protocol/execute-chan cassandra command {:channel channel}))))

(defn ->delta-store [] (map->DeltaStore {}))

(s/def ::delta-store (fn [x] (and x (instance? DeltaStore x))))
