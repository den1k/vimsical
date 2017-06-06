(ns vimsical.backend.handlers.vcs.commands
  (:require
   [clojure.spec.alpha :as s]
   [vimsical.backend.components.datomic :as datomic]
   [vimsical.backend.components.delta-store :as delta-store]
   [vimsical.backend.components.delta-store.protocol :as delta-store.protocol]
   [vimsical.backend.components.session-store.spec :as session-store.spec]
   [vimsical.vcs.validation :as vcs.validation]
   [vimsical.backend.handlers.multi :as multi]
   [vimsical.backend.util.async :refer [<?]]
   [vimsical.remotes.backend.vcs.commands :as commands]
   [vimsical.user :as user]))

;;
;; * Context specs
;;

(s/def ::datomic-context (s/keys :req-un [::datomic/datomic]))
(s/def ::deltas-context (s/keys :req-un [::delta-store/delta-store]))

;;
;; * Branch
;;

(defmethod multi/handle-event ::commands/add-branch
  [{:keys [datomic] :as context} [_ branch]]
  (multi/async
   context
   (multi/set-response context (<? (datomic/transact-chan datomic branch)))))

;;
;; * Deltas
;;

(defmethod multi/handle-event ::commands/add-deltas
  [{:as      context
    :keys    [delta-store session]
    user-uid ::user/uid}
   [_ vims-uid deltas :as event]]
  (let [deltas-by-branch-uid  (get-in session [::session-store.spec/sync-state vims-uid] )
        deltas-by-branch-uid' (vcs.validation/update-delta-by-branch-uid deltas-by-branch-uid deltas)
        insert-chan           (delta-store.protocol/insert-deltas-chan delta-store vims-uid deltas)]
    (multi/async
     context
     (-> context
         (multi/set-response (<? insert-chan))
         (multi/assoc-in-session [::session-store.spec/sync-state vims-uid] deltas-by-branch-uid')))))
