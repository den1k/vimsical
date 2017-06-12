(ns vimsical.frontend.timeline.handlers
  (:require
   [com.stuartsierra.mapgraph :as mg]
   [re-frame.core :as re-frame]
   [vimsical.frontend.code-editor.handlers :as code-editor.handlers]
   [vimsical.frontend.timeline.subs :as subs]
   [vimsical.frontend.timeline.ui-db :as timeline.ui-db]
   [vimsical.frontend.util.re-frame :as util.re-frame]
   [vimsical.frontend.vcs.db :as vcs.db]
   [vimsical.frontend.app.subs :as app.subs]
   [vimsical.frontend.vcs.subs :as vcs.subs]
   [vimsical.vcs.core :as vcs]))

;;
;; * UI Db
;;

;;
;; ** SVG
;;

(re-frame/reg-event-fx
 ::register-svg
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ node]]
   {:ui-db (assoc ui-db ::svg node)}))

(re-frame/reg-event-fx
 ::dispose-svg
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ node]]
   {:ui-db (dissoc ui-db ::svg node)}))

(defn ui-time
  [{:keys [ui-db]} coords coords-and-svg-node->timeline-position-fn]
  (let [svg-node (get ui-db ::svg)]
    (coords-and-svg-node->timeline-position-fn coords svg-node)))

(defn ui-timeline-entry
  [{::vcs.subs/keys [vcs] :as cofx} coords coords-and-svg-node->timeline-position-fn]
  (let [ui-time        (ui-time cofx coords coords-and-svg-node->timeline-position-fn)
        timeline-entry (vcs/timeline-entry-at-time vcs ui-time)]
    [ui-time timeline-entry]))

;;
;; ** User activity
;;

(re-frame/reg-event-fx
 ::set-playing
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ vims playing?]]
   {:ui-db (timeline.ui-db/set-playing ui-db vims playing?)}))

;;
;; ** Skimhead
;;

;; The following handlers are responsible for keeping the skimhead in the ui-db,
;; and the skimhead-entry in the db in sync

(re-frame/reg-event-fx
 ::on-mouse-enter
 [(re-frame/inject-cofx :ui-db)
  (util.re-frame/inject-sub (fn [[_ vims]] [::vcs.subs/vcs vims]))]
 (fn [{:as             cofx
       :keys           [db ui-db]
       ::vcs.subs/keys [vcs]}
      [_ vims coords coords-and-svg-node->timeline-position-fn]]
   (let [[t entry] (ui-timeline-entry cofx coords coords-and-svg-node->timeline-position-fn)
         vcs'   (vcs.db/set-skimhead-entry vcs entry)
         db'    (mg/add db vcs')
         ui-db' (-> ui-db
                    (timeline.ui-db/set-skimhead vims t)
                    (timeline.ui-db/set-skimming vims true))]
     {:db db' :ui-db ui-db'})))

(re-frame/reg-event-fx
 ::on-mouse-wheel
 [(re-frame/inject-cofx :ui-db)
  (util.re-frame/inject-sub (fn [[_ vims]] [::vcs.subs/vcs vims]))
  (util.re-frame/inject-sub (fn [[_ vims]] [::subs/duration vims]))
  (util.re-frame/inject-sub (fn [[_ vims]] [::subs/skimhead vims]))]
 (fn [{:keys           [db ui-db]
       ::vcs.subs/keys [vcs]
       ::subs/keys     [skimhead duration]}
      [_ vims dx]]
   (let [skimhead' (max 0 (min duration (- skimhead dx)))
         entry     (vcs/timeline-entry-at-time vcs skimhead')
         vcs'      (vcs.db/set-skimhead-entry vcs entry)
         db'       (mg/add db vcs')
         ui-db'    (timeline.ui-db/set-skimhead ui-db vims skimhead')]
     {:db db' :ui-db ui-db'})))

(re-frame/reg-event-fx
 ::on-mouse-move
 [(re-frame/inject-cofx :ui-db)
  (util.re-frame/inject-sub (fn [[_ vims]] [::vcs.subs/vcs vims]))]
 (fn [{:as             cofx
       :keys           [db ui-db]
       ::vcs.subs/keys [vcs]}
      [_ vims coords coords-and-svg-node->timeline-position-fn]]
   (let [[t entry] (ui-timeline-entry cofx coords coords-and-svg-node->timeline-position-fn)
         vcs'   (vcs.db/set-skimhead-entry vcs entry)
         db'    (mg/add db vcs')
         ui-db' (timeline.ui-db/set-skimhead ui-db vims t)]
     {:db db' :ui-db ui-db'})))

(re-frame/reg-event-fx
 ::on-mouse-leave
 [(re-frame/inject-cofx :ui-db)
  (util.re-frame/inject-sub (fn [[_ vims]] [::vcs.subs/vcs vims]))]
 (fn [{:keys [ui-db db] ::vcs.subs/keys [vcs] vims ::app.subs/vims} [_ vims]]
   (let [vcs'   (vcs.db/set-skimhead-entry vcs nil)
         db'    (mg/add db vcs')
         ui-db' (-> ui-db
                    (timeline.ui-db/set-skimhead vims nil)
                    (timeline.ui-db/set-skimming vims false))]
     {:db       db'
      :ui-db    ui-db'
      :dispatch [::code-editor.handlers/reset-all-editors-to-playhead
                 {:ui-key :vcr
                  :vims   vims}]})))

;;
;; ** Playhead
;;

;; Used to sync the value of the scheduler's time with the playhead in the ui,
;; the scheduler takes care of setting the playhead-entry at the appropriate
;; time so we only update the ui-db

(re-frame/reg-event-fx
 ::set-playhead
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ vims t]]
   {:ui-db (timeline.ui-db/set-playhead ui-db vims t)}))

(re-frame/reg-event-fx
 ::set-playhead-with-timeline-entry
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ vims [t]]]
   {:ui-db (timeline.ui-db/set-playhead ui-db vims t)}))

;; Used when clicking the timeline, we need to set:
;; - the playhead in the ui-db
;; - the playhead-entry in the db
;; - reset the time in the scheduler
;; NOTE: the step event is used to break a circular dependency between this file
;; and vcr.handlers

(re-frame/reg-event-fx
 ::on-click
 [(re-frame/inject-cofx :ui-db)
  (util.re-frame/inject-sub (fn [[_ vims]] [::vcs.subs/vcs vims]))]
 (fn [{:as             cofx
       :keys           [db ui-db]
       ::vcs.subs/keys [vcs]}
      [_ vims coords coords-and-svg-node->timeline-position-fn step-event]]
   (let [[ui-time [entry-time :as entry]] (ui-timeline-entry cofx coords coords-and-svg-node->timeline-position-fn)
         vcs'   (assoc vcs ::vcs.db/playhead-entry entry)
         db'    (mg/add db vcs')
         ui-db' (timeline.ui-db/set-playhead ui-db vims ui-time)]
     {:db        db'
      :ui-db     ui-db'
      :scheduler [{:action :set-time :t ui-time}
                  {:override? true :t entry-time :event step-event}]})))
