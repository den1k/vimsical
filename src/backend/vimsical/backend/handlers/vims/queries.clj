(ns vimsical.backend.handlers.vims.queries
  (:require
   [clojure.core.async :as a]
   [clojure.spec.alpha :as s]
   [vimsical.backend.components.datomic :as datomic]
   [vimsical.backend.components.delta-store :as delta-store]
   [vimsical.backend.components.delta-store.protocol :as delta-store.protocol]
   [vimsical.backend.components.snapshot-store.protocol :as snapshot-store.protocol]
   [vimsical.backend.handlers.multi :as multi]
   [vimsical.backend.util.async :as async :refer [<?]]
   [vimsical.common.uuid :refer [uuid]]
   [vimsical.queries.vims :as queries.vims]
   [vimsical.remotes.backend.vims.queries :as queries]
   [vimsical.vcs.branch :as vcs.branch]
   [vimsical.vcs.snapshot :as snapshot]
   [vimsical.vims :as vims]))

;;
;; * Vims
;;

;; XXX Duplication from vimsical.backend.handlers.user.queries
(defn- vims+snapshots-chan
  [snapshot-store uuid-fn user-uid {vims-uid :db/uid :as vims}]
  (a/go
    (let [snapshots-chan           (snapshot-store.protocol/select-vims-snapshots-chan snapshot-store user-uid vims-uid)
          snapshots                (a/<! (a/into [] snapshots-chan))
          snapshots-by-file-uid    (group-by ::snapshot/file-uid snapshots)
          vims-master-branch-files (-> vims vims/master-branch ::vcs.branch/files)]
      (letfn [(file->frontend-snapshot [{file-uid :db/uid :as file}]
                (when-some [[raw-snapshot] (get snapshots-by-file-uid file-uid)]
                  (snapshot/->frontend-snapshot (uuid-fn) raw-snapshot file)))]
        (assoc vims ::vims/snapshots
               (into [] (keep file->frontend-snapshot) vims-master-branch-files))))))

(defmethod multi/context-spec ::queries/vims [_] (s/keys :req-un [::datomic/datomic]))
(defmethod multi/handle-event ::queries/vims
  [{:keys [datomic snapshot-store] :as context} [_ vims-uid]]
  (if-some [vims (datomic/pull datomic queries.vims/pull-query [:db/uid vims-uid])]
    ;; NOTE querying snapshots of the vims owner, this will need some
    ;; refinements once we have forks
    (let [owner-uid (-> vims ::vims/owner :db/uid)]
      (multi/async
       context
       (multi/set-response context (<? (vims+snapshots-chan snapshot-store uuid owner-uid vims)))))
    (multi/set-response context 404 nil)))

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
