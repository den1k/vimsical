(ns vimsical.backend.handlers.vcs.queries
  (:require
   [clojure.spec :as s]
   [vimsical.backend.components.delta-store :as delta-store]
   [vimsical.backend.components.delta-store.protocol :as delta-store.protocol]
   [vimsical.vcs.validation :as vcs.validation]
   [vimsical.backend.components.session-store :as session-store]
   [vimsical.backend.handlers.multi :as multi]
   [vimsical.backend.util.async :refer [<?]]
   [vimsical.remotes.backend.vcs.queries :as queries]))

;;
;; * Context specs
;;

(s/def ::deltas-context (s/keys :req-un [::delta-store/delta-store ::session-store/session-store]))

;;
;; * Deltas by branch-uid
;;

(defmethod multi/context-spec ::queries/deltas-by-branch-uid [_] ::deltas-context)
(defmethod multi/handle-event ::queries/deltas-by-branch-uid
  [{:keys [delta-store] :as context} [_ vims-uid :as event]]
  (multi/async
   context
   (let [deltas-by-branch-uid (<? (delta-store.protocol/select-deltas-by-branch-uid-chan delta-store vims-uid))]
     (-> context
         (multi/set-response deltas-by-branch-uid)
         (multi/assoc-session ::vcs.validation/delta-by-branch-uid deltas-by-branch-uid)))))
