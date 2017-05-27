(ns vimsical.backend.handlers.vims.commands
  (:require
   [clojure.core.async :as a]
   [clojure.spec :as s]
   [vimsical.backend.adapters.cassandra.protocol :refer [<?]]
   [vimsical.backend.components.datomic :as datomic]
   [vimsical.backend.components.server.interceptors.event-auth :as event-auth]
   [vimsical.backend.components.snapshot-store :as snapshot-store]
   [vimsical.backend.components.snapshot-store.protocol :as snapshot-store.protocol]
   [vimsical.backend.handlers.multi :as multi]
   [vimsical.remotes.backend.vims.commands :as commands]
   [vimsical.user :as user]))

;;
;; * Transaction helpers
;;

(defn transact-event-chan!
  [{:keys [datomic]} [_ vims]]
  (a/thread
    (try
      (do (deref (datomic/transact datomic vims)) nil)
      (catch Throwable t t))))

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
(defmethod multi/handle-event ::commands/new [context event]
  (a/go
    (try
      (multi/set-response
       context
       (<? (transact-event-chan! context event)))
      (catch Throwable t
        (multi/set-error context t)))))

;;
;; * Title
;;

(defmethod event-auth/require-auth? ::commands/title [_] true)
(defmethod multi/context-spec ::commands/title [_] ::datomic-context)
(defmethod multi/handle-event ::commands/title [context event]
  (a/go
    (try
      (multi/set-response
       context
       (<? (transact-event-chan! context event)))
      (catch Throwable t
        (multi/set-error context t)))))

;;
;; * Snapshots
;;

(defmethod event-auth/require-auth? ::commands/update-snapshots [_] true)
(defmethod multi/context-spec ::commands/update-snapshots [_] ::snapshot-context)
(defmethod multi/handle-event ::commands/update-snapshots
  [{:keys [snapshot-store user/uid] :as context} [_ snapshots :as event]]
  (a/go
    (try
      (multi/set-response
       context
       (<? (snapshot-store.protocol/insert-snapshots-chan snapshot-store snapshots)))
      (catch Throwable t
        (multi/set-error context t)))))
