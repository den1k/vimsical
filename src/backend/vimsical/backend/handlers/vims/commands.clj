(ns vimsical.backend.handlers.vims.commands
  (:require
   [clojure.spec :as s]
   [vimsical.backend.components.datomic :as datomic]
   [vimsical.backend.components.server.interceptors.event-auth :as event-auth]
   [vimsical.backend.components.snapshot-store :as snapshot-store]
   [vimsical.backend.components.snapshot-store.protocol :as snapshot-store.protocol]
   [vimsical.backend.handlers.multi :as multi]
   [vimsical.backend.util.async :refer [<?]]
   [vimsical.remotes.backend.vims.commands :as commands]
   [vimsical.user :as user]))

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
(defmethod multi/handle-event ::commands/new
  [{::user/keys [uid]
    :keys       [datomic] :as context} [_ vims]]
  (let [tx {:db/uid uid ::user/vimsae [vims]}]
    (multi/async
     context
     (multi/set-response
      context
      (<? (datomic/transact-chan datomic tx))))))

;;
;; * Title
;;

(defmethod event-auth/require-auth? ::commands/title [_] true)
(defmethod multi/context-spec ::commands/title [_] ::datomic-context)
(defmethod multi/handle-event ::commands/title
  [{:keys [datomic] :as context} [_ vims-title]]
  (multi/async
   context
   (multi/set-response
    context
    (<? (datomic/transact-chan datomic vims-title)))))

;;
;; * Snapshots
;;

(defmethod event-auth/require-auth? ::commands/update-snapshots [_] true)
(defmethod multi/context-spec ::commands/update-snapshots [_] ::snapshot-context)
(defmethod multi/handle-event ::commands/update-snapshots
  [{:keys [snapshot-store user/uid] :as context} [_ snapshots :as event]]
  (multi/async
   context
   (multi/set-response
    context
    (<? (snapshot-store.protocol/insert-snapshots-chan snapshot-store snapshots)))))
