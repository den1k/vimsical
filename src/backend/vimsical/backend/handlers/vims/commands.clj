(ns vimsical.backend.handlers.vims.commands
  (:require
   [clojure.spec :as s]
   [vimsical.backend.adapters.cassandra :as cassandra]
   [vimsical.backend.components.datomic :as datomic]
   [vimsical.backend.components.server.interceptors.event-auth :as event-auth]
   [vimsical.backend.components.snapshot-store :as snapshot-store]
   [vimsical.backend.components.snapshot-store.protocol :as snapshot-store.protocol]
   [vimsical.backend.handlers.multi :as multi]
   [vimsical.backend.util.log :as log]
   [vimsical.remotes.backend.vims.commands :as commands]
   [vimsical.vcs.snapshot :as snapshot]
   [vimsical.user :as user]))

;;
;; * Transaction helpers
;;

(defn transact-event!
  [{:keys [datomic] :as context} [_ vims :as event]]
  (try
    (do (deref (datomic/transact datomic vims)) {})
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

(defmethod event-auth/require-auth? ::commands/new [_] true)
(defmethod multi/context-spec ::commands/new [_] ::datomic-context)
(defmethod multi/handle-event ::commands/new [context event] (transact-event! context event))

;;
;; * Title
;;

(defmethod event-auth/require-auth? ::commands/title [_] true)
(defmethod multi/context-spec ::commands/title [_] ::datomic-context)
(defmethod multi/handle-event ::commands/title [context event] (transact-event! context event))

;;
;; * Snapshots
;;

(defmethod event-auth/require-auth? ::commands/update-snapshots [_] true)
(defmethod multi/context-spec ::commands/update-snapshots [_] ::snapshot-context)
(defmethod multi/handle-event ::commands/update-snapshots
  [{:keys [snapshot-store user/uid] :as context} [_ snapshots :as event]]
  (letfn [(snapshots->vims-uid [snapshots]
            (-> snapshots first ::snapshot/vims-uid))]
    (let [vims-uid (snapshots->vims-uid snapshots)
          options  {:channel (cassandra/new-error-chan)}]
      (snapshot-store.protocol/insert-snapshots-chan
       snapshot-store uid vims-uid snapshots options))
    (log/error "Unauthenticated" {:event event})))
