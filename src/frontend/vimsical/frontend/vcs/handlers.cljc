(ns vimsical.frontend.vcs.handlers
  (:require
   [vimsical.vims :as vims]
   [vimsical.user :as user]
   [com.stuartsierra.mapgraph :as mg]
   [re-frame.core :as re-frame]
   [vimsical.frontend.timeline.ui-db :as timeline.ui-db]
   [vimsical.frontend.util.re-frame :as util.re-frame]
   [vimsical.frontend.vcs.db :as vcs.db]
   [vimsical.frontend.vcs.queries :as queries]
   [vimsical.frontend.vcs.subs :as subs]
   [vimsical.remotes.backend.vcs.queries :as backend.vcs.queries]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.core :as vcs]
   [vimsical.vcs.editor :as editor]
   [vimsical.frontend.util.mapgraph :as util.mg]))

;;
;; * VCS Vims init
;;

(defn init-vims-vcs
  [uuid-fn {:as vims :keys [db/uid] ::vims/keys [branches]} deltas]
  {:pre [uid (seq branches)]}
  (let [master             (branch/master branches)
        empty-vcs          (vcs/empty-vcs branches)
        vcs-state          (reduce
                            (fn [vcs delta]
                              (vcs/add-delta vcs uuid-fn delta))
                            empty-vcs deltas)
        vcs-frontend-state {:db/uid             (uuid-fn)
                            ::vcs.db/branch-uid (:db/uid master)
                            ::vcs.db/delta-uid  nil}
        vcs-entity         (merge vcs-state vcs-frontend-state)]
    (assoc vims ::vims/vcs vcs-entity)))

(defmulti init
  (fn [db [_ _ vims _]]
    (cond
      (map? vims)       :vims
      (mg/ref? db vims) :ref
      (uuid? vims)      :uid)))

(defmethod init :uid
  [db [event-id uuid-fn uid deltas :as event]]
  (let [ref [:db/uid uid]]
    (init db [event-id uuid-fn ref deltas])))

(defmethod init :ref
  [db [event-id uuid-fn ref deltas :as event]]
  (let [vims (mg/pull db queries/vims ref)]
    (init db [event-id uuid-fn vims deltas])))

(defmethod init :vims
  [db [_ uuid-fn vims deltas]]
  (let [vims' (init-vims-vcs uuid-fn vims deltas)]
    (mg/add db vims')))

(re-frame/reg-event-db ::init init)

;;
;; * Cofxs
;;

;;
;; ** Padding
;;

(def event-max-pad 500)

(defn new-pad-fn
  [elapsed]
  (let [pad-counter (atom -1)]
    (fn [edit-event]
      (cond
        ;; Always return 0 after the first invocation, ensures that spliced
        ;; deltas pad at 0 after the first one. Due to an implementation detail
        ;; in the vcs -- the AVL maps assoc a relative time to a single delta
        ;; inside chunks -- the timeline will end up with only the last spliced
        ;; delta, at a time equal to that of the first one, which is in fact the
        ;; behavior we want.
        (pos? (swap! pad-counter inc)) 0

        ;; If it's the very first time the event handler is called, we want to
        ;; return `event-max-pad` so the first delta doesn't end up "stuck" to
        ;; the left of the timeline
        (== -1 elapsed) event-max-pad

        ;; In all other cases we want to cap the elapsed time to
        ;; `event-max-pad`.
        ;;
        ;; NOTE that this will need to change with audio since we'll want the
        ;; actual elapsed time when an audio clip is recording.
        :else (min elapsed event-max-pad)))))

;;
;; ** Editor
;;

(defn editor-cofx
  [{:keys [uuid-fn timestamp elapsed] :as context} _]
  {:pre [uuid-fn timestamp elapsed]}
  (assoc context ::editor/effects
         ;; NOTE all these fns take the edit-event
         {::editor/uuid-fn      (fn [& _] (uuid-fn))
          ::editor/timestamp-fn (fn [& _] timestamp)
          ::editor/pad-fn       (new-pad-fn elapsed)}))

(re-frame/reg-cofx :editor editor-cofx)

;; The :editor cofx depends on the 3 previous cofxs, and should be injected
;; _after_ them. re-frame flattens the handler's cofxs so we can nest them in a
;; vector it has no special meaning, just a way to refer to a stack of cofxs

(def editor-cofxs
  [(re-frame/inject-cofx :uuid-fn)
   (re-frame/inject-cofx :timestamp)
   (re-frame/inject-cofx :elapsed)
   (re-frame/inject-cofx :editor)])

;;
;; * Edit events
;;

(defn- update-pointers
  [[{:as vcs ::vcs.db/keys [playhead-entry]} _ delta-uid {branch-uid :db/uid :as branch}]]
  (let [playhead-entry' (vcs/timeline-next-entry vcs playhead-entry)
        pointers        (cond-> {::vcs.db/delta-uid      delta-uid
                                 ::vcs.db/playhead-entry playhead-entry'}
                          (some? branch) (assoc ::vcs.db/branch-uid branch-uid))]
    (merge vcs pointers)))

(defmulti add-edit-event*
  (fn [{:as vcs ::vcs.db/keys [branch-uid playhead-entry]} effects file-uid edit-event]
    (when (vcs/branching? vcs branch-uid playhead-entry) :branching)))

(defmethod add-edit-event* :default
  [{:as vcs ::vcs.db/keys [branch-uid playhead-entry]} effects file-uid edit-event]
  (let [[_ {current-delta-uid :uid}] playhead-entry]
    (vcs/add-edit-event vcs effects file-uid branch-uid current-delta-uid edit-event)))

(defmethod add-edit-event* :branching
  [{:as vcs ::vcs.db/keys [branch-uid playhead-entry]} effects file-uid edit-event]
  (let [[_ {current-delta-uid :uid}] playhead-entry]
    (vcs/add-edit-event-branching vcs effects file-uid branch-uid current-delta-uid edit-event)))

(defn add-edit-event
  "Update the vcs with the edit event and move the playhead-entry to the newly created timeline entry"
  [{:as vcs ::vcs.db/keys [branch-uid playhead-entry]} effects file-uid edit-event]
  ;; Use editor effects to create a branch id that we can reference in the deltas
  (let [[_ _ _ branch-maybe :as result] (add-edit-event* vcs effects file-uid edit-event)]
    [(update-pointers result) branch-maybe]))

(re-frame/reg-event-fx
 ::add-edit-event
 [editor-cofxs
  (re-frame/inject-cofx :ui-db)
  (util.re-frame/inject-sub (fn [[_ vims]] [::subs/vcs vims]))
  (util.re-frame/inject-sub (fn [[_ vims]] [::subs/playhead-entry vims]))]
 (fn [{:keys         [db ui-db]
       ::subs/keys   [vcs]
       ::editor/keys [effects]}
      [_ vims {file-uid :db/uid} edit-event]]
   ;; Get the time from the update timeline entry and update the timeline ui.
   (let [[{[t] ::vcs.db/playhead-entry :as vcs'}
          branch-maybe] (add-edit-event vcs effects file-uid edit-event)]
     ;; TODO Add new branch to vims
     {:ui-db (timeline.ui-db/set-playhead ui-db vims t)
      :db    (mg/add db vcs')})))

;;
;; * Remote
;;

;;
;; ** Deltas by branch uid
;;

(re-frame/reg-event-fx
 ::sync-init
 (fn [{:keys [db]} [_ vims-uid status-key]]
   {:remote
    {:id               :backend
     :event            [::backend.vcs.queries/deltas-by-branch-uid vims-uid]
     :status-key       status-key
     :dispatch-success (fn [deltas]
                         [::sync-init-success vims-uid deltas])}}))

;; {:app/sync
;;  {:<vims-uid> {:deltas-branch-branch-uid {:<branch-uid> :<delta>}
;;                :queue                    [:<delta1> :<delta2>]
;;                :in-flight                [:<delta-0>]}}}

(re-frame/reg-event-fx
 ::sync-init-success
 (fn [{:keys [db]} [_ vims-uid deltas-by-branch-uid]]
   {:db (assoc-in db [:app/sync vims-uid :deltas-by-branch-uid] deltas-by-branch-uid)}))
