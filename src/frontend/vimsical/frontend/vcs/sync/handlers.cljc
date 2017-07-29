(ns vimsical.frontend.vcs.sync.handlers
  (:require
   [re-frame.core :as re-frame]
   [re-frame.interceptor :as interceptor]
   [vimsical.frontend.util.re-frame :as util.re-frame]
   [vimsical.frontend.vcs.subs :as vcs.subs]
   [vimsical.frontend.vcs.sync.db :as db]
   [vimsical.remotes.backend.vcs.commands :as commands]
   [vimsical.common.util.core :as util]
   [vimsical.remotes.backend.vcs.queries :as queries]
   [vimsical.vims :as vims]
   [vimsical.frontend.util.subgraph :as util.sg]
   [vimsical.vcs.sync :as vcs.sync]
   [vimsical.vcs.validation :as vcs.validation]))

;;
;; * Helpers
;;

(defn ->dispatch [event-id & args]
  (fn [response]
    (conj (into [event-id] args) response)))

(def prevent-remote-interceptor
  "Dissoc the remote effect if the current `:app/user` isn't the same as the
  `::vims/owner` on `::app/vims`."
  (interceptor/->interceptor
   :id ::prevent-remote-interceptor
   :after
   (fn prevent-remote-interceptor-after
     [context]
     (letfn [(current-vims-owner? [db]
               (let [user                  (util.sg/pull* db [:app/user [:db/uid]])
                     {::vims/keys [owner]} (util.sg/pull* db [:app/vims [{::vims/owner [:db/uid]}]])]
                 (util/=by :db/uid user owner)))]
       (let [db              (or (interceptor/get-effect context :db)
                                 (interceptor/get-coeffect context :db))
             prevent-remote? (not (current-vims-owner? db))]
         (cond-> context
           prevent-remote? (util/dissoc-in [:effects :remote])))))))

;;
;; * Entry point: Wait -> Init or Ready (for new vims)
;;

(re-frame/reg-event-fx
 ::start
 [prevent-remote-interceptor]
 (fn [{:keys [db]} [_ vims-uid status-key]]
   {:db (assoc-in db (db/path vims-uid) (db/new-sync vims-uid))
    :remote
    {:id               :backend
     :event            [::queries/delta-by-branch-uid vims-uid]
     :dispatch-success (->dispatch ::init-success vims-uid)
     :dispatch-error   (->dispatch ::init-error vims-uid)
     :status-key       status-key}}))

(re-frame/reg-event-fx
 ::stop
 [prevent-remote-interceptor]
 (fn [{:keys [db]} [_ vims-uid]]
   {:dispatch [::sync vims-uid]}))

;;
;; * Init -> Ready or InitError
;;

(re-frame/reg-event-fx
 ::init-success
 (fn [{:keys [db]} [_ vims-uid delta-by-branch-uid]]
   {:db       (assoc-in db (db/path vims-uid ::db/delta-by-branch-uid) delta-by-branch-uid)
    :dispatch [::sync vims-uid]}))

(re-frame/reg-event-fx
 ::init-error
 (fn [{:keys [db]} [_ vims-uid error]]))

;;
;; * Sync
;;

(re-frame/reg-event-fx
 ::sync
 [prevent-remote-interceptor
  (util.re-frame/inject-sub (fn [[_ vims-uid]] [::vcs.subs/vcs {:db/uid vims-uid}]))]
 (fn [{:keys [db] ::vcs.subs/keys [vcs]} [d-key vims-uid]]
   ;; Get the current sync state
   (let [delta-by-branch-uid (get-in db (db/path vims-uid ::db/delta-by-branch-uid))
         status-key          [d-key vims-uid]]
     ;; Diff it against the current state of the vcs
     (if-some [deltas (vcs.sync/diff-deltas vcs delta-by-branch-uid)]
       ;; If we have some diffed deltas, send them to the backend
       {:remote
        {:id               :backend
         :status-key       status-key
         :event            [::commands/add-deltas vims-uid deltas]
         :dispatch-success (->dispatch ::sync-success vims-uid deltas)
         :dispatch-error   (->dispatch ::sync-error vims-uid deltas)}}
       ;; If not we'll just signal a success with not deltas
       {:dispatch [::sync-success vims-uid]}))))

(re-frame/reg-event-fx
 ::sync-success
 (fn [{:keys [db]} [_ vims-uid ?deltas]]
   ;; If we successfully synced some deltas we need to update the sync state to
   ;; point to the deltas that we just synced
   (when (seq ?deltas)
     (re-frame.loggers/console :log "Synced " (count ?deltas) "deltas")
     (let [path (db/path vims-uid ::db/delta-by-branch-uid)]
       {:db (-> db
                (assoc ::add-deltas-debouncing? false)
                (update-in path vcs.validation/update-delta-by-branch-uid ?deltas))}))))

(re-frame/reg-event-fx
 ::sync-error
 (fn [{:keys [db]} [_ e]]
   (throw (ex-info "Sync error" {:remote-error e}))))

;;
;; * Ready
;;

(re-frame/reg-event-fx
 ::add-deltas
 (fn [{:keys [db]} [_ vims-uid deltas]]
   {:db (assoc db ::add-deltas-debouncing? true)
    :debounce
    {:id       ::add-deltas
     :ms       2e3
     :dispatch [::sync vims-uid]}}))

(re-frame/reg-event-fx
 ::add-branch
 [prevent-remote-interceptor]
 (fn [{:keys [db]} [_ vims-uid branch]]
   {:remote
    {:id               :backend
     :event            [::commands/add-branch vims-uid branch]
     :dispatch-success (->dispatch ::add-branch-success vims-uid)
     :dispatch-error   (->dispatch ::add-branch-error vims-uid)}}))

(re-frame/reg-event-fx
 ::add-branch-success
 (fn [{:keys [db]} [_]]))

(re-frame/reg-event-fx
 ::add-branch-error
 (fn [{:keys [db]} [_]]))
