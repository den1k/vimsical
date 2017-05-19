(ns  vimsical.backend.handlers.vcs.commands
  (:require
   [clojure.core.async :as a]
   [clojure.spec :as s]
   [vimsical.backend.components.datomic :as datomic]
   [vimsical.backend.components.snapshot-store :as snapshot-store]
   [vimsical.backend.components.snapshot-store.protocol
    :as
    snapshot-store.protocol]
   [vimsical.backend.handlers.multi :as multi]
   [vimsical.backend.util.log :as log]
   [vimsical.remotes.backend.vcs.commands :as commands]
   [vimsical.user :as user]
   [vimsical.vcs.snapshot :as snapshot]
   [vimsical.remotes.event :as event]))

;;
;; * Session helpers
;;

(defn context->user-uid [context] (some-> context :request :session ::user/uid))

;;
;; * Transaction helpers
;;

(defn transact-event!
  [{:keys [datomic] :as context} [vims :as event]]
  (try
    (do (deref (datomic/transact datomic vims)) {})
    (catch Throwable t
      (log/error {:event event :ex t}))))

;;
;; * Context specs
;;

(s/def ::datomic-context (s/keys :req-un [::datomic/datomic]))
(s/def ::snapshot-context (s/keys :req-un [::snapshot-store/snapshot-store]))


;;
;; * Branch
;;

(defmethod multi/handle-event ::commands/add-branch! [context event] (transact-event! context event))

;;
;; * Deltas
;;

(defmethod multi/handle-event ::commands/add-deltas! [context event])
