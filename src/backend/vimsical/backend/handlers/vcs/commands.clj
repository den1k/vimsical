(ns vimsical.backend.handlers.vcs.commands
  (:require
   [clojure.core.async :as a]
   [clojure.spec :as s]
   [vimsical.backend.adapters.cassandra.protocol :refer [<?]]
   [vimsical.backend.components.datomic :as datomic]
   [vimsical.backend.components.delta-store :as delta-store]
   [vimsical.backend.components.delta-store.protocol :as delta-store.protocol]
   [vimsical.backend.components.delta-store.validation :as delta-store.validation]
   [vimsical.backend.handlers.multi :as multi]
   [vimsical.remotes.backend.vcs.commands :as commands]))

;;
;; * Transaction helpers
;;

(defn transact-event-chan
  [{:keys [datomic]} [_ vims]]
  (a/thread
    (try
      (do (deref (datomic/transact datomic vims)) nil)
      (catch Throwable t t))))

;;
;; * Context specs
;;

(s/def ::datomic-context (s/keys :req-un [::datomic/datomic]))
(s/def ::deltas-context (s/keys :req-un [::delta-store/delta-store]))

;;
;; * Branch
;;

(defmethod multi/handle-event ::commands/add-branch! [context event]
  (a/go
    (try
      (multi/set-response context (<? (transact-event-chan context event)))
      (catch Throwable t
        (multi/set-error t)))))

;;
;; * Deltas
;;

(defmethod multi/handle-event ::commands/add-deltas!
  [{:keys [delta-store session] :as context} [_ deltas :as event]]
  (let [deltas-by-branch-uid  (get session ::delta-store.validation/deltas-by-branch-uid)
        deltas-by-branch-uid' (delta-store.validation/update-deltas-by-branch-uid deltas-by-branch-uid deltas)
        vims-uid              (-> deltas first :vims-uid)]
    (a/go
      (try
        (let [insert-chan (delta-store.protocol/insert-deltas-chan delta-store vims-uid deltas)
              response    (<? insert-chan)]
          (-> context
              (multi/set-response response)
              (multi/assoc-session ::delta-store.validation/deltas-by-branch-uid deltas-by-branch-uid')))
        (catch Throwable t
          (multi/set-error t))))))
