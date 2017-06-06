(ns vimsical.backend.handlers.user.queries
  (:require
   [clojure.core.async :as a]
   [clojure.spec.alpha :as s]
   [vimsical.backend.components.datomic :as datomic]
   [vimsical.backend.components.server.interceptors.event-auth :as event-auth]
   [vimsical.backend.components.snapshot-store :as snapshot-store]
   [vimsical.common.uuid :refer [uuid]]
   [vimsical.backend.components.snapshot-store.protocol :as snapshot-store.protocol]
   [vimsical.backend.handlers.multi :as multi]
   [vimsical.backend.util.async :as async :refer [<?]]
   [vimsical.queries.user :as queries.user]
   [vimsical.remotes.backend.user.queries :as user.queries]
   [vimsical.user :as user]
   [vimsical.vcs.branch :as vcs.branch]
   [vimsical.vcs.snapshot :as snapshot]
   [vimsical.vims :as vims]
   [vimsical.vcs.file :as file]))

;;
;; * Cross-stores join helpers
;;

(s/fdef user-join-snapshots
        :args (s/cat :user ::user/user :snapshots (s/every ::snapshot/snapshot) :uuid-fn (s/? ifn?))
        :ret  ::user/user)

(defn- user-join-snapshots
  ([user snapshots] (user-join-snapshots user snapshots uuid))
  ([{::user/keys [vimsae] :as user} snapshots uuid-fn]
   (let [snapshots-by-vims-uid (group-by ::snapshot/vims-uid snapshots)]
     (letfn [(get-vims-snapshots [{vims-uid :db/uid :as vims}]
               (when-some [snapshots (get snapshots-by-vims-uid vims-uid)]
                 (let [snapshots-by-file-uid    (group-by ::snapshot/file-uid snapshots)
                       vims-master-branch-files (-> vims vims/master-branch ::vcs.branch/files)]
                   (letfn [(file->frontend-snapshot [{file-uid :db/uid :as file}]
                             (when-some [[raw-snapshot] (get snapshots-by-file-uid file-uid)]
                               (snapshot/->frontend-snapshot (uuid-fn) raw-snapshot file)))]
                     (into [] (keep file->frontend-snapshot) vims-master-branch-files)))))
             (vims-join-snapshots [vims]
               (if-some [snapshots (get-vims-snapshots vims)]
                 (assoc vims ::vims/snapshots snapshots)
                 vims))
             (vimsae-join-snapshots [vimsae]
               (mapv vims-join-snapshots vimsae))]
       (cond-> user
         (seq vimsae) (update ::user/vimsae vimsae-join-snapshots))))))

;;
;; * Query helpers
;;

(defn- user-chan
  [datomic uid]
  (datomic/pull-chan datomic queries.user/datomic-pull-query [:db/uid uid]))

(defn- snapshots-chan
  [snapshot-store uid]
  (a/into [] (snapshot-store.protocol/select-user-snapshots-chan snapshot-store uid)))

(defn user+snapshots-chan
  [{:keys [datomic snapshot-store]} uid]
  (a/go
    (apply user-join-snapshots
           (<? (async/parallel-promises?
                [(user-chan datomic uid)
                 (snapshots-chan snapshot-store uid)])))))

;;
;; * Handler
;;

(defn context-handler
  [{::user/keys [uid] :as context} _]
  (multi/async context (multi/set-response context (<? (user+snapshots-chan context uid)))))

;;
;; * Event handler
;;

(s/def ::context-spec
  (s/keys :req-un [::datomic/datomic ::snapshot-store/snapshot-store]))
(defmethod event-auth/require-auth? ::user.queries/me [_] true)
(defmethod multi/context-spec ::user.queries/me [_] ::context-spec)
(defmethod multi/handle-event ::user.queries/me [context event] (context-handler context event))
