(ns vimsical.backend.handlers.vcs.queries
  (:require
   [clojure.spec.alpha :as s]
   [vimsical.backend.components.delta-store :as delta-store]
   [vimsical.backend.components.delta-store.protocol :as delta-store.protocol]
   [vimsical.backend.components.server.interceptors.event-auth :as event-auth]
   [vimsical.backend.components.session-store :as session-store]
   [vimsical.backend.handlers.multi :as multi]
   [vimsical.backend.util.async :refer [<?]]
   [vimsical.remotes.backend.vcs.queries :as queries]
   [vimsical.vcs.validation :as vcs.validation]
   [vimsical.user :as user]))

;;
;; * Context specs
;;

(s/def ::deltas-context (s/keys :req-un [::delta-store/delta-store ::session-store/session-store]))

;;
;; * Deltas by branch-uid
;;

(defmethod event-auth/require-auth? ::queries/delta-by-branch-uid [_] true)
(defmethod multi/context-spec ::queries/delta-by-branch-uid [_] ::deltas-context)
(defmethod multi/handle-event ::queries/delta-by-branch-uid
  [{:as         context
    ::user/keys [uid]
    :keys       [delta-store]} [_ vims-uid :as event]]
  (multi/async
   context
   (let [{::vcs.validation/keys [delta-by-branch-uid] :as vims-session}
         (<? (delta-store.protocol/select-vims-session-chan delta-store vims-uid uid))
         vims-session-path (session-store/vims-session-path vims-uid)]
     (-> context
         (multi/set-response delta-by-branch-uid)
         (multi/assoc-in-session vims-session-path vims-session)))))
