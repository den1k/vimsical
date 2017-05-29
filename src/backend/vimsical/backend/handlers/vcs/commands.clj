(ns vimsical.backend.handlers.vcs.commands
  (:require
   [clojure.spec :as s]
   [vimsical.backend.components.datomic :as datomic]
   [vimsical.backend.components.delta-store :as delta-store]
   [vimsical.backend.components.delta-store.protocol :as delta-store.protocol]
   [vimsical.vcs.validation :as vcs.validation]
   [vimsical.backend.handlers.multi :as multi]
   [vimsical.backend.util.async :refer [<?]]
   [vimsical.remotes.backend.vcs.commands :as commands]))

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
  [{:keys [delta-store session] :as context} [_ deltas :as event]]
  (let [deltas-by-branch-uid  (get session ::vcs.validation/delta-by-branch-uid)
        deltas-by-branch-uid' (vcs.validation/update-delta-by-branch-uid deltas-by-branch-uid deltas)
        vims-uid              (-> deltas first :vims-uid)
        insert-chan           (delta-store.protocol/insert-deltas-chan delta-store vims-uid deltas)]
    (multi/async
     context
     (-> context
         (multi/set-response (<? insert-chan))
         (multi/assoc-session ::vcs.validation/delta-by-branch-uid deltas-by-branch-uid')))))
