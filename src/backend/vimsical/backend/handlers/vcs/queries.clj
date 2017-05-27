(ns vimsical.backend.handlers.vcs.queries
  (:require
   [clojure.core.async :as a]
   [clojure.spec :as s]
   [vimsical.backend.adapters.cassandra.protocol :refer [<?]]
   [vimsical.backend.components.delta-store :as delta-store]
   [vimsical.backend.components.delta-store.protocol :as delta-store.protocol]
   [vimsical.backend.components.delta-store.validation :as delta-store.validation]
   [vimsical.backend.handlers.multi :as multi]
   [vimsical.remotes.backend.vcs.queries :as queries]))

;;
;; * Context specs
;;

(s/def ::deltas-context (s/keys :req-un [::delta-store/delta-store]))

;;
;; * Deltas by branch-uid
;;

(defmethod multi/context-spec ::queries/deltas-by-branch-uid [_] ::deltas-context)
(defmethod multi/handle-event ::queries/deltas-by-branch-uid
  [{:keys [delta-store] :as context} [_ vims-uid :as event]]
  (a/go
    (try
      (let [query-chan           (delta-store.protocol/select-deltas-by-branch-uid-chan delta-store vims-uid)
            deltas-by-branch-uid (<? query-chan)]
        (-> context
            (multi/set-response deltas-by-branch-uid)
            (multi/assoc-session ::delta-store.validation/deltas-by-branch-uid  deltas-by-branch-uid)))
      (catch Throwable t
        (multi/set-error context t)))))
