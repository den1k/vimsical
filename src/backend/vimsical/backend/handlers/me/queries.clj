(ns vimsical.backend.handlers.me.queries
  (:require
   [clojure.core.async :as a]
   [clojure.spec :as s]
   [vimsical.backend.components.datomic :as datomic]
   [vimsical.backend.components.snapshot-store :as snapshot-store]
   [vimsical.backend.components.snapshot-store.protocol :as snapshot-store.protocol]
   [vimsical.backend.handlers.multi :as multi]
   [vimsical.queries.user :as queries.user]
   [vimsical.user :as user]
   [vimsical.vcs.branch :as vcs.branch]
   [vimsical.vcs.snapshot :as snapshot]
   [vimsical.vims :as vims]))

;;
;; * Session helpers
;;

(defn- context->user-uid
  [context] (some-> context :request :session ::user/uid))

;;
;; * Cross-stores join helpers
;;

(s/fdef user-join-snapshots
        :args (s/cat :user ::user/user :snapshots (s/every ::snapshot/snaphot))
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

(defmethod multi/context-spec ::me [_]
  (s/keys :req-un [::datomic/datomic ::snapshot-store/snapshot-store]))

(defmethod multi/handle-event ::me
  [{:keys [datomic snapshot-store] :as context} _]
  (let [out-chan (a/chan)]
    (when-some [user-uid (context->user-uid context)]
      (let [{user-uid :db/uid :as user} (datomic/pull datomic queries.user/pull-query [:db/uid user-uid])]
        (snapshot-store.protocol/select-snapshots-async
         snapshot-store user-uid nil
         (fn [snapshots]
           (doto out-chan
             (a/put! (user-join-snapshots user snapshots))
             (a/close!)))
         (fn [error]
           (a/close! out-chan)))))
    out-chan))
