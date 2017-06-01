(ns vimsical.frontend.vcs.sync.handlers
  (:require
   [re-frame.core :as re-frame]
   [vimsical.frontend.util.re-frame :as util.re-frame]
   [vimsical.frontend.vcs.subs :as vcs.subs]
   [vimsical.frontend.vcs.sync.db :as db]
   [vimsical.remotes.backend.vcs.commands :as commands]
   [vimsical.remotes.backend.vcs.queries :as queries]
   [vimsical.vcs.sync :as vcs.sync]
   [vimsical.vcs.validation :as vcs.validation]))

;;
;; * Helpers
;;

(defn ->dispatch [event-id & args]
  (fn [response]
    (conj (into [event-id] args) response)))

;;
;; * Entry point: Wait -> Init or Ready (for new vims)
;;

(re-frame/reg-event-fx
 ::start
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
 (fn [{:keys [db]} [_ vims-uid]]
   {:dispatch [::sync vims-uid]}))

;;
;; * Init -> Ready or InitError
;;

(re-frame/reg-event-fx
 ::init-success
 [db/fsm-interceptor]
 (fn [{:keys [db]} [_ vims-uid delta-by-branch-uid]]
   {:db       (assoc-in db (db/path vims-uid ::db/delta-by-branch-uid) delta-by-branch-uid)
    :dispatch [::sync vims-uid]}))

(re-frame/reg-event-fx
 ::init-error
 [db/fsm-interceptor]
 (fn [{:keys [db]} [_ vims-uid error]]
   (assert false)))

;;
;; * Sync
;;

(re-frame/reg-event-fx
 ::sync
 [db/fsm-interceptor
  (util.re-frame/inject-sub (fn [[_ vims-uid]] [::vcs.subs/vcs {:db/uid vims-uid}]))]
 (fn [{:keys [db] ::vcs.subs/keys [vcs]} [_ vims-uid]]
   ;; Get the current sync state
   (let [delta-by-branch-uid (get-in db (db/path vims-uid ::db/delta-by-branch-uid))]
     ;; Diff it against the current state of the vcs
     (if-some [deltas (vcs.sync/diff-deltas vcs delta-by-branch-uid)]
       ;; If we have some diffed deltas, send them to the backend
       (do (println "Syncing " (count deltas) " deltas...")
           {:remote
            {:id               :backend
             :event            [::commands/add-deltas vims-uid deltas]
             :dispatch-success (->dispatch ::sync-success vims-uid deltas)
             :dispatch-error   (->dispatch ::sync-error vims-uid deltas)}})
       ;; If not we'll just signal a success with not deltas
       {:dispatch [::sync-success vims-uid]}))))

(re-frame/reg-event-fx
 ::sync-success
 [db/fsm-interceptor]
 (fn [{:keys [db]} [_ vims-uid ?deltas]]
   ;; If we successfully synced some deltas we need to update the sync state to
   ;; point to the deltas that we just synced
   (when (seq ?deltas)
     (println "Sync success")
     (let [path (db/path vims-uid ::db/delta-by-branch-uid)]
       {:db (update-in db path vcs.validation/update-delta-by-branch-uid ?deltas)}))))

(re-frame/reg-event-fx
 ::sync-error
 [db/fsm-interceptor]
 (fn [{:keys [db]} [_ e]]
   (throw (ex-info "Sync error" {:remote-error e}))))

;;
;; * Ready
;;

(re-frame/reg-event-fx
 ::add-deltas
 [db/fsm-interceptor]
 (fn [{:keys [db]} [_ vims-uid deltas]]
   {:debounce
    {:id       ::add-deltas
     :ms       2e3
     :dispatch [::sync vims-uid]}}))

(re-frame/reg-event-fx
 ::add-branch
 [db/fsm-interceptor]
 (fn [{:keys [db]} [_ vims-uid branch]]
   {:remote
    {:id               :backend
     :event            [::commands/add-branch branch]
     :dispatch-success (->dispatch ::add-branch-success vims-uid)
     :dispatch-error   (->dispatch ::add-branch-error vims-uid)}}))

(re-frame/reg-event-fx
 ::add-branch-success
 [db/fsm-interceptor]
 (fn [{:keys [db]} [_]]))

(re-frame/reg-event-fx
 ::add-branch-error
 [db/fsm-interceptor]
 (fn [{:keys [db]} [_]]))
