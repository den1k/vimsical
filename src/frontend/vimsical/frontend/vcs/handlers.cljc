(ns vimsical.frontend.vcs.handlers
  "TODO
  - advance vcs playhead-entry to newly inserted entry
  - change `init-vims` to init from an exitsing vims
  - add cofxs for add-edit-event (timestamp, uuid and padding)
  "
  (:require
   [com.stuartsierra.mapgraph :as mg]
   ;; [vimsical.frontend.db :as db]
   [re-frame.core :as re-frame]
   [vimsical.common.util.core :as util]
   [vimsical.common.uuid :refer [uuid]]
   [vimsical.frontend.util.re-frame :as util.re-frame]
   [vimsical.frontend.util.mapgraph :as util.mg]
   [vimsical.frontend.vcs.queries :as queries]
   [vimsical.frontend.vcs.subs :as subs]
   [vimsical.frontend.vcs.db :as vcs.db]
   [vimsical.frontend.timeline.ui-db :as timeline.ui-db]
   [vimsical.vcs.core :as vcs]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.editor :as editor]
   [vimsical.vcs.edit-event :as edit-event]))

;;
;; * VCS Vims init
;;

(defn init-vims
  [db [_]]
  (let [{:as        vims
         :keys      [db/id]
         :vims/keys [branches]} (util.mg/pull* db [:app/vims queries/vims])
        master                  (branch/master branches)
        vcs-state               (vcs/empty-vcs branches)
        vcs-frontend-state      {:db/id             (uuid)
                                 ::vcs.db/branch-id (:db/id master)
                                 ::vcs.db/delta-id  nil}
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

;; NOTE
;; - the first padding will always be `event-max-pad`
;; - any subsequent padding will be capped at `event-max-pad`
;; - when pad-fn is invoked more than once it will always return 1
;; - we currently don't handle zero paddings well due to how deltas are
;;   denormalized in the timeline and the chunks, they cause the latest delta
;;   with a zero padding value to replace the previous one

(def event-max-pad 500)

(defn pad [elapsed]
  ;; The first time `::add-edit-event` is dispatched `elapsed` will be -1 in
  ;; which case we want to return `event-max-pad` so the delta doesn't end up
  ;; "stuck" to the left of the timeline.
  ;;
  ;; In all other cases we want to cap the `elapsed` time to `event-max-pad`
  ;; which will most definitely yield the max value for the first delta in a new
  ;; vims when switching between vims.
  ;;
  ;; NOTE this will need to change with audio since we'll need to stop capping
  ;; the elapsed time when an audio clip is recording.
  (if (== -1 elapsed)
    event-max-pad
    (min elapsed event-max-pad)))

(defn new-pad-fn
  [elapsed]
  (let [pad-counter (atom -1)]
    (fn [edit-event]
      (pad (if (zero? (swap! pad-counter inc)) elapsed 1)))))

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
    ::vcs.db/keys [branch-id delta-id playhead-entry]}
   effects
   file-id
   edit-event]
  (let [[vcs' delta-id'] (vcs/add-edit-event vcs effects file-id branch-id delta-id edit-event)
        playhead-entry'  (if playhead-entry (vcs/timeline-next-entry vcs' playhead-entry) (vcs/timeline-first-entry vcs'))
        state'           {::vcs.db/delta-id delta-id' ::vcs.db/playhead-entry playhead-entry'}]
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
      [_ {file-id :db/id} edit-event]]
   ;; Get the time from the update timeline entry and update the timeline ui.
   (let [{[t] ::vcs.db/playhead-entry :as vcs'} (add-edit-event vcs effects file-id edit-event)]
     {:ui-db (timeline.ui-db/set-playhead ui-db t)
      :db    (mg/add db vcs')})))
