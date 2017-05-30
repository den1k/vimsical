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

(defn ->dispatch [event-id & [vims-uid :as args]]
  {:pre [(uuid? vims-uid)]}
  (fn [response]
    (conj (into [event-id] args) response)))

;;
;; * Entry point
;;

(re-frame/reg-event-fx
 ::start
 (fn [{:keys [db]} [_ vims-uid status-key]]
   {:db (assoc-in db (db/path vims-uid) (db/new-sync vims-uid))
    :remote
    {:id               :backend
     :event            [::queries/delta-by-branch-uid vims-uid]
     :dispatch-success (->dispatch ::delta-by-branch-uid-success vims-uid)
     :dispatch-error   (->dispatch ::delta-by-branch-uid-error vims-uid)
     :status-key       status-key}}))

;;
;; * Init
;;

(re-frame/reg-event-fx
 ::delta-by-branch-uid-success
 [db/fsm-interceptor]
 (fn [{:keys [db]} [_ vims-uid delta-by-branch-uid]]
   {:db       (cond-> db
                (seq delta-by-branch-uid)
                (assoc-in (db/path vims-uid ::db/delta-by-branch-uid) delta-by-branch-uid))
    :dispatch [::sync vims-uid delta-by-branch-uid]}))

(re-frame/reg-event-fx
 ::delta-by-branch-uid-error
 [db/fsm-interceptor]
 (fn [{:keys [db]} [_ vims-uid error]]
   (assert false)))

;;
;; * Sync
;;

(re-frame/reg-event-fx
 ::sync
 [db/fsm-interceptor
  (util.re-frame/inject-sub (fn [[_ vims-uid]] (re-frame/subscribe [::vcs.subs/vcs [:db/uid vims-uid]])))]
 (fn [{:keys [db] ::vcs.subs/keys [vcs]} [_ vims-uid delta-by-branch-uid]]
   (if-not (seq delta-by-branch-uid)
     {:dispatch [::sync-success vims-uid []]}
     (let [deltas-by-branch-uid (vcs.sync/diff vcs delta-by-branch-uid)
           deltas               (apply concat (vals deltas-by-branch-uid))]
       {:remote
        {:id               :backend
         :event            [::commands/add-deltas deltas]
         :dispatch-success (->dispatch ::sync-success vims-uid deltas)
         :dispatch-error   (->dispatch ::sync-error vims-uid deltas)}}))))

(re-frame/reg-event-fx
 ::sync-success
 [db/fsm-interceptor]
 (fn [{:keys [db]} [_ vims-uid deltas]]
   (let [path [:app/sync vims-uid ::db/delta-by-branch-uid]
         f    (fn [old] (vcs.validation/update-delta-by-branch-uid old deltas))]
     {:db (update-in db path f)})))

(re-frame/reg-event-fx
 ::sync-error
 [db/fsm-interceptor]
 (fn [{:keys [db]} [_]]
   (assert false)))

;;
;; * Ready
;;

(re-frame/reg-event-fx
 ::add-deltas
 [db/fsm-interceptor]
 (fn [{:keys [db]} [_ vims-uid deltas]]
   {:remote
    {:id               :backend
     :event            [::commands/add-deltas deltas]
     :dispatch-success (->dispatch ::add-deltas-success vims-uid deltas)
     :dispatch-error   (->dispatch ::add-deltas-error vims-uid deltas)}}))

(re-frame/reg-event-fx
 ::add-deltas-success
 [db/fsm-interceptor]
 (fn [{:keys [db]} [_ vims-uid deltas]]
   (let [path [:app/sync vims-uid ::db/delta-by-branch-uid]
         f    (fn [old] (vcs.validation/update-delta-by-branch-uid old deltas))]
     {:db (update-in db path f)})))

(re-frame/reg-event-fx
 ::add-deltas-error
 [db/fsm-interceptor]
 (fn [{:keys [db]} [_]]))

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
