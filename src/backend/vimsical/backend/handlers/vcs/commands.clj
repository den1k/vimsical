(ns vimsical.backend.handlers.vcs.commands
  (:require
   [clojure.core.async :as a]
   [clojure.spec :as s]
   [vimsical.backend.adapters.cassandra.protocol :refer [<?]]
   [vimsical.backend.components.datomic :as datomic]
   [vimsical.backend.components.delta-store :as delta-store]
   [vimsical.backend.components.delta-store.protocol
    :as
    delta-store.protocol]
   [vimsical.backend.components.delta-store.validation
    :as
    delta-store.validation]
   [vimsical.backend.handlers.multi :as multi]
   [vimsical.backend.util.log :as log]
   [vimsical.remotes.backend.vcs.commands :as commands]
   [vimsical.user :as user]
   [vimsical.vcs.core :as vcs.core]))

;;
;; * Session helpers
;;

(defn context->user-uid [context] (some-> context :request :session ::user/uid))

;;
;; * Transaction helpers
;;

(defn transact-event!
  [{:keys [datomic]} [_ vims :as event]]
  (try
    (do (deref (datomic/transact datomic vims)) {})
    (catch Throwable t
      (log/error {:event event :ex t}))))

;;
;; * Context specs
;;

(s/def ::datomic-context (s/keys :req-un [::datomic/datomic]))
(s/def ::deltas-context (s/keys :req-un [::delta-store/delta-store]))


;;
;; * Branch
;;

(defmethod multi/handle-event ::commands/add-branch! [context event] (transact-event! context event))

;;
;; * Deltas
;;

(defmethod multi/handle-event ::commands/add-deltas!
  [{:keys [delta-store session] :as context} [_ deltas :as event]]
  (let [deltas-by-branch-uid  (get-in context [:request :session ::delta-store.validation/deltas-by-branch-uid])
        deltas-by-branch-uid' (delta-store.validation/update-deltas-by-branch-uid deltas-by-branch-uid deltas)
        vims-uid              (-> deltas first :vims-uid)]
    (a/go
      (try
        (<? (delta-store.protocol/insert-deltas-chan delta-store vims-uid deltas))
        (assoc-in context [:response :session ::delta-store.validation/deltas-by-branch-uid] deltas-by-branch-uid')
        (catch Throwable t t)))))
