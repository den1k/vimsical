(ns vimsical.backend.handlers.vims.commands
  (:require
   [clojure.spec :as s]
   [vimsical.backend.adapters.cassandra :as cassandra]
   [vimsical.backend.components.datomic :as datomic]
   [vimsical.backend.components.snapshot-store :as snapshot-store]
   [vimsical.backend.components.snapshot-store.protocol
    :as
    snapshot-store.protocol]
   [vimsical.backend.handlers.multi :as multi]
   [vimsical.backend.util.log :as log]
   [vimsical.remotes.backend.vims.commands :as commands]
   [vimsical.vcs.snapshot :as snapshot]))

;;
;; * Transaction helpers
;;

(defn- context->user-uid
  [context]
  (some-> context :request :session ::user/uid))

(defn transact-event!
  [{:keys [datomic] :as context} [_ vims :as event]]
  (try
    (do (deref (datomic/transact datomic vims)) nil)
    (catch Throwable t
      (log/error {:event event :ex t}))))

;;
;; * Context specs
;;

(s/def ::datomic-context  (s/keys :req-un [::datomic/datomic]))
(s/def ::snapshot-context (s/keys :req-un [::snapshot-store/snapshot-store]))

;;
;; * New
;;

(defmethod multi/context-spec ::commands/new [_] ::datomic-context)
(defmethod multi/handle-event ::commands/new [context event] (transact-event! context event))

;;
;; * Title
;;

(defmethod multi/context-spec ::commands/set-title! [_] ::datomic-context)
(defmethod multi/handle-event ::commands/set-title! [context event] (transact-event! context event))

;;
;; * Snapshots
;;

(defmethod multi/context-spec ::commands/update-snapshots [_] ::snapshot-context)
(defmethod multi/handle-event ::commands/update-snapshots
  [{:keys [snapshot-store] :as context} [_ snapshots :as event]]
  (letfn [(snapshots->vims-uid [snapshots]
            {:pre [(apply = (map ::snapshot/vims-uid snapshots))]}
            (-> snapshots first ::snapshot/vims-uid))]
    (if-some [user-uid (context->user-uid context)]
      (let [vims-uid (snapshots->vims-uid snapshots)
            options  {:channel (cassandra/new-error-chan)}]
        (snapshot-store.protocol/insert-snapshots-chan
         snapshot-store user-uid vims-uid snapshots options))
      (log/error "Unauthenticated" {:event event}))))
