(ns vimsical.frontend.vcs.handlers
  (:require
   [com.stuartsierra.mapgraph :as mg]
   ;; [vimsical.frontend.db :as db]
   [re-frame.core :as re-frame]
   [vimsical.common.uuid :refer [uuid]]
   [vimsical.frontend.util.re-frame :as util.re-frame]
   [vimsical.frontend.util.mapgraph :as util.mg]
   [vimsical.frontend.vcs.queries :as queries]
   [vimsical.frontend.vcs.subs :as subs]
   [vimsical.frontend.vcs.db :as vcs.db]
   [vimsical.frontend.timeline.ui-db :as timeline.ui-db]
   [vimsical.vcs.core :as vcs]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.editor :as editor]))

;;
;; * VCS Vims init
;;

(defn init-vims
  [db [_]]
  (let [{:as        vims
         :keys      [db/uid]
         :vims/keys [branches]} (util.mg/pull* db [:app/vims queries/vims])
        master                  (branch/master branches)
        vcs-state               (vcs/empty-vcs branches)
        vcs-frontend-state      {:db/uid             (uuid)
                                 ::vcs.db/branch-uid (:db/uid master)
                                 ::vcs.db/delta-uid  nil}
        vcs-entity              (merge vcs-state vcs-frontend-state)
        vims'                   (assoc vims :vims/vcs vcs-entity)]
    (mg/add db vims')))

(re-frame/reg-event-db ::init-vims init-vims)

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

(defn add-edit-event
  "Update the vcs with the edit event and move the playhead-entry to the newly created timeline entry"
  [{:as           vcs
    ::vcs.db/keys [branch-uid delta-uid playhead-entry]}
   effects
   file-uid
   edit-event]
  (let [[vcs' delta-uid'] (vcs/add-edit-event vcs effects file-uid branch-uid delta-uid edit-event)
        playhead-entry'   (if playhead-entry (vcs/timeline-next-entry vcs' playhead-entry) (vcs/timeline-first-entry vcs'))
        state'            {::vcs.db/delta-uid delta-uid' ::vcs.db/playhead-entry playhead-entry'}]
    (merge vcs' state')))

(re-frame/reg-event-fx
 ::add-edit-event
 [editor-cofxs
  (re-frame/inject-cofx :ui-db)
  (util.re-frame/inject-sub [::subs/vcs])
  (util.re-frame/inject-sub [::subs/playhead-entry])]
 (fn [{:keys         [db ui-db]
       ::subs/keys   [vcs]
       ::editor/keys [effects]}
      [_ {file-uid :db/uid} edit-event]]
   ;; Get the time from the update timeline entry and update the timeline ui.
   (let [{[t] ::vcs.db/playhead-entry :as vcs'} (add-edit-event vcs effects file-uid edit-event)]
     {:ui-db (timeline.ui-db/set-playhead ui-db t)
      :db    (mg/add db vcs')})))
