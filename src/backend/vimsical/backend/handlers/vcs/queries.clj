(ns vimsical.backend.handlers.vcs.queries
  (:require
   [clojure.spec.alpha :as s]
   [vimsical.backend.components.delta-store :as delta-store]
   [vimsical.backend.components.delta-store.protocol :as delta-store.protocol]
   [vimsical.vcs.validation :as vcs.validation]
   [vimsical.backend.components.session-store :as session-store]
   [vimsical.backend.components.session-store.spec :as session-store.spec]

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

(defmethod multi/context-spec ::queries/delta-by-branch-uid [_] ::deltas-context)
(defmethod multi/handle-event ::queries/delta-by-branch-uid
  [{:keys [delta-store] :as context} [_ vims-uid :as event]]
  (multi/async
   context
   (let [delta-by-branch-uid (<? (delta-store.protocol/select-delta-by-branch-uid-chan delta-store vims-uid))]
     (-> context
         (multi/set-response delta-by-branch-uid)
         (multi/assoc-in-session [::session-store.spec/sync-state vims-uid] delta-by-branch-uid)))))