(ns vimsical.backend.handlers.me.queries
  (:require
   [clojure.spec :as s]
   [vimsical.backend.components.datomic :as datomic]
   [vimsical.backend.components.server.interceptors.event-auth :as event-auth]
   [vimsical.backend.components.snapshot-store :as snapshot-store]
   [vimsical.backend.components.snapshot-store.protocol :as snapshot-store.protocol]
   [vimsical.backend.handlers.multi :as multi]
   [vimsical.backend.util.async :refer [<?]]
   [vimsical.queries.user :as queries.user]
   [vimsical.remotes.backend.user.queries :as user.queries]
   [vimsical.user :as user]
   [vimsical.vcs.branch :as vcs.branch]
   [vimsical.vcs.snapshot :as snapshot]
   [vimsical.vims :as vims]))

;;
;; * Cross-stores join helpers
;;

(s/fdef user-join-snapshots
        :args (s/cat :user ::user/user :snapshots (s/every ::snapshot/snapshot))
        :ret  ::user/user)

(defn- user-join-snapshots
  [user snapshots]
  (let [snapshots-by-vims-uid (group-by ::snapshot/vims-uid snapshots)]
    (letfn [(get-vims-snapshots [{:keys [db/uid] :as vims}]
              (when-some [snapshots (get snapshots-by-vims-uid uid)]
                (let [snapshots-by-file-uid    (group-by ::snapshot/file-uid snapshots)
                      vims-master-branch-files (-> vims vims/master-branch ::vcs.branch/files)]
                  (letfn [(file->snapshot [{:keys [db/uid] :as file}]
                            (when-some [[{::snapshot/keys [text]}] (get snapshots-by-file-uid uid)]
                              {::snapshot/uid  uid
                               ::snapshot/text text
                               ::snapshot/file file}))]
                    (into [] (keep file->snapshot) vims-master-branch-files)))))
            (vims-join-snapshots [vims]
              (if-some [snapshots (get-vims-snapshots snapshots-by-vims-uid vims)]
                (assoc vims ::vims/snapshots snapshots)
                vims))
            (vimsae-join-snapshots [vimsae]
              (mapv #(vims-join-snapshots snapshots-by-vims-uid %) vimsae))]
      (update user :user/vimsae vimsae-join-snapshots))))

;;
;; * Event handler
;;

(s/def ::context-spec
  (s/keys :req-un [::datomic/datomic ::snapshot-store/snapshot-store]))
(defmethod event-auth/require-auth? ::user.queries/me [_] true)
(defmethod multi/context-spec ::user.queries/me [_] ::context-spec)
(defmethod multi/handle-event ::user.queries/me
  [{:keys [datomic snapshot-store user/uid] :as context} _]
  (letfn [(user-chan []
            (datomic/pull-chan
             datomic queries.user/pull-query [:db/uid uid]))
          (snapshots-chan []
            (snapshot-store.protocol/select-snapshots-chan
             snapshot-store uid nil))]
    (multi/async
     context
     ;; XXX Would be better if we could run the ops in parallel
     (let [user           (<? (user-chan))
           snapshots      (<? (snapshots-chan))
           user+snapshots (user-join-snapshots user snapshots)]
       (multi/set-response context user+snapshots)))))
