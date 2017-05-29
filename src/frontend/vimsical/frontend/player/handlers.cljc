(ns vimsical.frontend.player.handlers
  (:require [re-frame.core :as re-frame]
            [vimsical.frontend.util.re-frame :as util.re-frame]
            [vimsical.vcs.core :as vcs]
            [vimsical.frontend.vcs.subs :as vcs.subs]
            [vimsical.frontend.timeline.ui-db :as timeline.ui-db]
            [vimsical.frontend.vcs.db :as vcs.db]
            [com.stuartsierra.mapgraph :as mg]
            [vimsical.frontend.timeline.handlers :as timeline.handlers]
            [vimsical.frontend.timeline.subs :as timeline.subs]
            [vimsical.frontend.code-editor.handlers :as code-editor.handlers]))

(defn tick-fn
  [t]
  (re-frame/dispatch [::timeline.handlers/set-playhead t]))

(defn at-end?
  [vcs playhead-entry]
  (nil? (vcs/timeline-next-entry vcs playhead-entry)))

(re-frame/reg-event-fx
 ::play
 [(util.re-frame/inject-sub [::vcs.subs/vcs])
  (util.re-frame/inject-sub [::vcs.subs/playhead-entry])
  (util.re-frame/inject-sub [::timeline.subs/playhead])]
 (fn [{:keys                [db scheduler]
       ::vcs.subs/keys      [vcs playhead-entry]
       ::timeline.subs/keys [playhead]} _]
   (cond
     ;; Start from beginning:
     ;; - set the scheduler at 0
     ;; - schedule ::step after the duration of the first pad
     ;; - let ::step figure out the vcs update
     (nil? playhead-entry)
     (let [[t] (vcs/timeline-first-entry vcs)]
       {:dispatch [::timeline.handlers/set-playing true]
        :scheduler
                  [{:action :start :t 0 :tick tick-fn}
                   {:action :schedule :t t :event [::step]}]})

     ;; Reset and "recurse" so we'll start from beginning next time
     (at-end? vcs playhead-entry)
     (let [vcs' (assoc vcs ::vcs.db/playhead-entry nil)
           db'  (mg/add db vcs')]
       {:db       db'
        :dispatch [::play]})

     ;; Set time and start
     :else
     (let [[t] playhead-entry]
       {:dispatch [::timeline.handlers/set-playing true]
        :scheduler
                  [{:action :start :t playhead :tick tick-fn}
                   {:action :schedule :t t :event [::step]}]}))))

(re-frame/reg-event-fx
 ::pause
 (fn [_ _]
   {:dispatch  [::timeline.handlers/set-playing false]
    :scheduler {:action :pause}}))

(re-frame/reg-event-fx
 ::stop
 [(util.re-frame/inject-sub [::vcs.subs/vcs])]
 (fn [{:keys           [db]
       ::vcs.subs/keys [vcs]} _]
   (let [vcs' (assoc vcs ::vcs.db/playhead-entry nil)
         db'  (mg/add db vcs')]
     {:dispatch  [::timeline.handlers/set-playing false]
      :db        db'
      :scheduler {:action :stop}})))

(re-frame/reg-event-fx
 ::step
 [(util.re-frame/inject-sub [::vcs.subs/vcs])
  (util.re-frame/inject-sub [::vcs.subs/playhead-entry])]
 (fn [{:keys           [db]
       ::vcs.subs/keys [vcs playhead-entry]} _]
   (letfn [(next-entry [vcs playhead-entry]
             (if (nil? playhead-entry)
               (vcs/timeline-first-entry vcs)
               (vcs/timeline-next-entry vcs playhead-entry)))]
     (if-some [[t :as entry] (next-entry vcs playhead-entry)]
       (let [vcs' (assoc vcs ::vcs.db/playhead-entry entry)
             db'  (mg/add db vcs')]
         {:db db' :scheduler {:action :schedule :t t :event [::step]}})
       {:dispatch [::stop]}))))

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

;; Used when clicking the timeline, we need to set:
;; - the playhead in the ui-db
;; - the playhead-entry in the db
;; - reset the time in the scheduler
;; NOTE: the step event is used to break a circular dependency between this file
;; and vcr.handlers

(re-frame/reg-event-fx
 ::on-click
 [(re-frame/inject-cofx :ui-db)
  (util.re-frame/inject-sub [::vcs.subs/vcs])]
 (fn [{:as             cofx
       :keys           [db ui-db]
       ::vcs.subs/keys [vcs]}
      [_ ui-time]]
   (let [[entry-time :as entry] (vcs/timeline-entry-at-time vcs ui-time)
         vcs'   (assoc vcs ::vcs.db/playhead-entry entry)
         db'    (mg/add db vcs')
         ui-db' (assoc ui-db ::timeline.ui-db/playhead ui-time)]
     {:db        db'
      :ui-db     ui-db'
      :scheduler [{:action :set-time :t ui-time}
                  {:override? true :t entry-time :event [::step]}]})))

;;
;; ** Skimhead
;;

(re-frame/reg-event-fx
 ::on-mouse-enter
 [(re-frame/inject-cofx :ui-db)
  (util.re-frame/inject-sub [::vcs.subs/vcs])]
 (fn [{:keys           [db ui-db]
       ::vcs.subs/keys [vcs]} [_ vims ui-time]]
   (let [entry  (vcs/timeline-entry-at-time vcs ui-time)
         vcs'   (vcs.db/set-skimhead-entry vcs entry)
         db'    (mg/add db vcs')
         ui-db' (-> ui-db
                    (timeline.ui-db/set-skimhead vims ui-time)
                    (timeline.ui-db/set-skimming vims true))]
     {:db db' :ui-db ui-db'})))

(re-frame/reg-event-fx
 ::on-mouse-move
 [(re-frame/inject-cofx :ui-db)
  (util.re-frame/inject-sub [::vcs.subs/vcs])]
 (fn [{:keys           [db ui-db]
       ::vcs.subs/keys [vcs]} [_ vims ui-time]]
   (let [entry  (vcs/timeline-entry-at-time vcs ui-time)
         vcs'   (vcs.db/set-skimhead-entry vcs entry)
         db'    (mg/add db vcs')
         ui-db' (timeline.ui-db/set-skimhead vims ui-db ui-time)]
     {:db db' :ui-db ui-db'})))

(re-frame/reg-event-fx
 ::on-mouse-leave
 [(re-frame/inject-cofx :ui-db)
  (util.re-frame/inject-sub [::vcs.subs/vcs])]
 (fn [{:keys [ui-db db] ::vcs.subs/keys [vcs]} [_ vims]]
   (let [vcs'   (vcs.db/set-skimhead-entry vcs nil)
         db'    (mg/add db vcs')
         ui-db' (-> ui-db
                    (timeline.ui-db/set-skimhead vims nil)
                    (timeline.ui-db/set-skimming vims false))]
     {:db       db'
      :ui-db    ui-db'
      :dispatch [::code-editor.handlers/reset-all-editors-to-playhead]})))
