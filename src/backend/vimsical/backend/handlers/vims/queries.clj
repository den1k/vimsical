(ns vimsical.backend.handlers.vims.queries
  (:require
   [clojure.spec :as s]
   [vimsical.backend.components.datomic :as datomic]
   [vimsical.backend.components.delta-store :as delta-store]
   [vimsical.backend.components.delta-store.protocol :as delta-store.protocol]
   [vimsical.backend.handlers.multi :as multi]
   [vimsical.queries.vims :as queries.vims]
   [vimsical.remotes.backend.vims.queries :as queries]))

;;
;; * Vims
;;

(defmethod multi/context-spec ::queries/vims [_] (s/keys :req-un [::datomic/datomic]))
(defmethod multi/handle-event ::queries/vims
  [{:keys [datomic] :as context} [_ vims-uid]]
  (multi/set-response
   context
   (datomic/pull datomic queries.vims/pull-query [:db/uid vims-uid])))

;;
;; * Deltas
;;

(defmethod multi/context-spec ::queries/deltas [_] (s/keys :req-un [::delta-store/delta-store]))
(defmethod multi/handle-event ::queries/deltas
  [{:keys [delta-store] :as context} [_ vims-uid :as event]]
  ;; Set a chan as the response, the transit middleware will turn it into a
  ;; ReadableByteChan
  (multi/set-response
   context
   (delta-store.protocol/select-deltas-chan delta-store vims-uid)))
