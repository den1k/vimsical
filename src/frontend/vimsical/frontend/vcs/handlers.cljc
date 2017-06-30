(ns vimsical.frontend.vcs.handlers
  (:require
   [com.stuartsierra.mapgraph :as mg]
   [re-frame.core :as re-frame]
   [vimsical.common.uuid :refer [uuid]]
   [vimsical.frontend.app.subs :as app.subs]
   [vimsical.frontend.timeline.ui-db :as timeline.ui-db]
   [vimsical.frontend.util.mapgraph :as util.mg]
   [vimsical.frontend.util.re-frame :as util.re-frame]
   [vimsical.frontend.vcs.db :as vcs.db]
   [vimsical.remotes.backend.vcs.commands :as vcs.commands]
   [vimsical.frontend.vcs.queries :as queries]
   [vimsical.frontend.vcs.subs :as subs]
   [vimsical.frontend.vcs.sync.handlers :as sync.handlers]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.core :as vcs]
   [vimsical.vcs.editor :as editor]
   [vimsical.vcs.edit-event :as edit-event]
   [vimsical.vims :as vims]
   [vimsical.vcs.data.splittable :as splittable]
   [vimsical.common.util.core :as util]
   [vimsical.vcs.lib :as vcs.lib]))

;;
;; * VCS Vims init-event-fx
;;

(defn init-event-fx
  [{:keys [db]} [_ vims deltas {:keys [uuid-fn] :or {uuid-fn uuid} :as options}]]
  (let [vims-ref   (util.mg/->ref db vims)
        {:as         vims
         ::vims/keys [branches]} (mg/pull db queries/vims vims-ref)

        vcs        (vcs/add-deltas (vcs/empty-vcs branches) uuid-fn deltas)

        {branch-uid :db/uid} (branch/master branches)

        [_ {delta-uid :uid}
         :as playhead-entry] (vcs/timeline-last-entry vcs)
        vcs-db     {::vcs.db/branch-uid     branch-uid
                    ::vcs.db/delta-uid      delta-uid
                    ::vcs.db/playhead-entry playhead-entry}
        vcs-entity (merge {:db/uid (uuid-fn)} vcs vcs-db)
        vcs-ref    (util.mg/->ref db vcs-entity)
        vims-vcs   (assoc vims ::vims/vcs vcs-ref)]
    {:db (-> db
             (mg/add vims-vcs)
             (vcs.db/add vcs-entity))
     #?@(:cljs
         [:dispatch
          ;; Cyclic deps
          [:vimsical.frontend.timeline.handlers/set-playhead-with-timeline-entry vims playhead-entry]])}))

(re-frame/reg-event-fx ::init init-event-fx)

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
        :else (min (if (zero? elapsed) 1 elapsed) event-max-pad)))))

;;
;; ** Editor
;;

(defn editor-cofx
  [{:keys [uuid-fn timestamp elapsed] :as context}]
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

(defn print-vcs-state
  [{:as vcs ::vcs.db/keys [playhead-entry]} file-uid]
  (let [delta-uid (:uid (second playhead-entry))
        string    (vcs/file-string vcs file-uid delta-uid)
        cursor    (vcs/file-cursor vcs file-uid delta-uid)]
    (println string)
    (if (number? cursor)
      (println (str (apply str (repeat cursor " ")) "_"))
      (let [left  (apply min cursor)
            right (apply max cursor)
            span  (- right left 2)]
        (println (str (apply str (repeat left " ")) "[" (apply str (repeat span " ")) "]"))))))

(defn- update-pointers
  [[{:as vcs ::vcs.db/keys [playhead-entry branch-uid]} _ delta-uid {new-branch-uid :db/uid :as branch}]]
  {:post [::vcs.db/playhead-entry]}
  (let [next-entry     (vcs/timeline-next-entry vcs playhead-entry)
        playhead-entry (or next-entry (vcs/timeline-first-entry vcs))
        pointers       (cond-> {::vcs.db/playhead-entry playhead-entry
                                ::vcs.db/delta-uid      delta-uid}
                         (some? branch) (assoc ::vcs.db/branch-uid new-branch-uid))]
    (merge vcs pointers)))

(defmulti add-edit-event*
  (fn [{:as vcs ::vcs.db/keys [branch-uid playhead-entry]} effects file-uid edit-event]
    (let [branching?    (vcs/branching? vcs branch-uid playhead-entry)
          cursor-event? (edit-event/cursor-event? edit-event)]
      (cond
        (and branching? cursor-event?) :no-op
        branching? :branching
        :else :default))))

(defmethod add-edit-event* :default
  [{:as vcs ::vcs.db/keys [branch-uid playhead-entry]} effects file-uid edit-event]
  (let [[_ {current-delta-uid :uid}] playhead-entry]
    (vcs/add-edit-event vcs effects file-uid branch-uid current-delta-uid edit-event)))

(defmethod add-edit-event* :no-op
  [{:as vcs ::vcs.db/keys [branch-uid playhead-entry]} effects file-uid edit-event]
  false)

(defmethod add-edit-event* :branching
  [{:as vcs ::vcs.db/keys [branch-uid playhead-entry]} effects file-uid edit-event]
  (let [[_ {current-delta-uid :uid}] playhead-entry]
    (vcs/add-edit-event-branching vcs effects file-uid branch-uid current-delta-uid edit-event)))

(defn add-edit-event
  "Update the vcs with the edit event and move the playhead-entry to the newly created timeline playhead-entry"
  [{:as vcs ::vcs.db/keys [branch-uid playhead-entry]} effects file-uid edit-event]
  ;; Use editor effects to create a branch id that we can reference in the deltas
  (when-let [after-add-event (add-edit-event* vcs effects file-uid edit-event)]
    (let [[_ deltas _ ?branch :as result] after-add-event]
      [(update-pointers result) deltas ?branch])))

(re-frame/reg-event-fx
 ::add-edit-event
 [editor-cofxs
  (re-frame/inject-cofx :ui-db)
  (util.re-frame/inject-sub (fn [[_ vims]] [::subs/vcs vims]))]
 (fn [{:keys           [db ui-db]
       ::app.subs/keys [user]
       ::subs/keys     [vcs]
       ::editor/keys   [effects]}
      [_ {vims-uid :db/uid :as vims} {file-uid :db/uid} edit-event]]
   {:pre [vims-uid effects file-uid]}
   (when-let [[vcs' deltas ?branch] (add-edit-event vcs effects file-uid edit-event)]
     (let [[playhead' _] (::vcs.db/playhead-entry vcs')
           db'    (vcs.db/add db vcs')
           ui-db' (timeline.ui-db/set-playhead ui-db vims playhead')]
       (cond-> {:db         db'
                :ui-db      ui-db'
                :dispatch-n [[::sync.handlers/add-deltas vims-uid deltas]]}
         ;; NOTE branch is already in ::vcs/branches, don't need to mg/add it
         (some? ?branch)
         (-> (update :db util.mg/add-join :app/vims ::vims/branches ?branch)
             ;; NOTE We don't remote branches for now
             #_(update :dispatch-n conj [::sync.handlers/add-branch vims-uid ?branch])))))))

;;
;; * Libs
;;

(re-frame/reg-event-fx
 ::add-lib
 [(util.re-frame/inject-sub (fn [[_ vims _]] [::subs/branch vims]))]
 (fn [{:keys [db] branch ::subs/branch} [_ _ lib]]
   (let [[_ branch-uid :as branch-ref] (util.mg/->ref db branch)]
     {:db (util.mg/add-join db branch-ref ::branch/libs lib)
      :remote
          {:id               :backend
           :event            [::vcs.commands/add-lib branch-uid lib]
           :dispatch-success ::add-lib-success
           :dispatch-error   ::add-lib-error}})))

;; temp
(re-frame/reg-event-fx ::add-lib-success (fn [{:keys [db]} _] (println "Lib add sucess")))
(re-frame/reg-event-fx ::add-lib-error (fn [{:keys [db]} e] (println "Lib add error" e)))

(re-frame/reg-event-fx
 ::remove-lib
 [(util.re-frame/inject-sub (fn [[_ vims _]] [::subs/branch vims]))]
 (fn [{:keys [db] branch ::subs/branch} [_ _ lib]]
   (let [[_ branch-uid :as branch-ref] (util.mg/->ref db branch)]
     (println lib)
     {:db (util.mg/remove-join db branch-ref ::branch/libs lib)
      :remote
          {:id               :backend
           :event            [::vcs.commands/remove-lib branch-uid lib]
           :dispatch-success ::remove-lib-success
           :dispatch-error   ::remove-lib-error}})))

;; temp
(re-frame/reg-event-fx ::remove-lib-success (fn [{:keys [db]} _] (println "Lib remove sucess")))
(re-frame/reg-event-fx ::remove-lib-error (fn [{:keys [db]} e] (println "Lib remove error" e)))
