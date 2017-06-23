(ns vimsical.frontend.vcr.handlers
  (:require
   [com.stuartsierra.mapgraph :as mg]
   [re-frame.core :as re-frame]
   [vimsical.frontend.timeline.handlers :as timeline.handlers]
   [vimsical.frontend.util.re-frame :as util.re-frame]
   [vimsical.frontend.vcs.db :as vcs.db]
   [vimsical.frontend.vcs.subs :as vcs.subs]
   [vimsical.frontend.timeline.subs :as timeline.subs]
   [vimsical.vcs.core :as vcs]
   [vimsical.frontend.vcr.ui-db :as ui-db]))

(defn tick-fn [vims]
  (fn [t]
    (re-frame/dispatch [::timeline.handlers/set-playhead vims t])))

(defn at-end?
  [vcs playhead-entry]
  (nil? (vcs/timeline-next-entry vcs playhead-entry)))

(re-frame/reg-event-fx
 ::play
 [(util.re-frame/inject-sub (fn [[_ vims]] [::vcs.subs/vcs vims]))
  (util.re-frame/inject-sub (fn [[_ vims]] [::vcs.subs/playhead-entry vims]))
  (util.re-frame/inject-sub (fn [[_ vims]] [::timeline.subs/playhead vims]))
  (util.re-frame/inject-sub (fn [[_ vims]] [::timeline.subs/duration vims]))]
 (fn [{:keys                [db scheduler]
       ::vcs.subs/keys      [vcs playhead-entry]
       ::timeline.subs/keys [playhead duration]} [_ vims]]
   (cond
     ;; Start from beginning:
     ;; - set the scheduler at 0
     ;; - schedule ::step after the duration of the first pad
     ;; - let ::step figure out the vcs update
     (nil? playhead-entry)
     (let [[t] (vcs/timeline-first-entry vcs)
           start-time (if (>= playhead duration) 0 playhead)]
       {:dispatch  [::timeline.handlers/set-playing vims true]
        :scheduler [{:action :start :t start-time :tick (tick-fn vims)}
                    {:action :schedule :t t :event [::step vims]}]})

     ;; Reset and "recurse" so we'll start from beginning next time
     (at-end? vcs playhead-entry)
     (let [vcs' (assoc vcs ::vcs.db/playhead-entry nil)
           db'  (vcs.db/add db vcs')]
       {:db       db'
        :dispatch [::play vims]})

     ;; Set time and start
     :else
     (let [[t] playhead-entry]
       {:dispatch  [::timeline.handlers/set-playing vims true]
        :scheduler [{:action :start :t playhead :tick (tick-fn vims)}
                    {:action :schedule :t t :event [::step vims]}]}))))

(re-frame/reg-event-fx
 ::step
 [(util.re-frame/inject-sub (fn [[_ vims]] [::vcs.subs/vcs vims]))
  (util.re-frame/inject-sub (fn [[_ vims]] [::vcs.subs/playhead-entry vims]))]
 (fn [{:keys           [db]
       ::vcs.subs/keys [vcs playhead-entry]} [_ vims]]
   (letfn [(next-entry [vcs playhead-entry]
             (if (nil? playhead-entry)
               (vcs/timeline-first-entry vcs)
               (vcs/timeline-next-entry vcs playhead-entry)))]
     (if-some [[t :as entry] (next-entry vcs playhead-entry)]
       (let [vcs' (assoc vcs ::vcs.db/playhead-entry entry)
             db'  (vcs.db/add db vcs')]
         {:db db' :scheduler {:action :schedule :t t :event [::step vims]}})
       {:dispatch [::pause vims]}))))

(re-frame/reg-event-fx
 ::pause
 (fn [_ [_ vims]]
   {:dispatch  [::timeline.handlers/set-playing vims false]
    :scheduler {:action :pause}}))

(re-frame/reg-event-fx
 ::stop
 [(util.re-frame/inject-sub (fn [[_ vims]] [::vcs.subs/vcs vims]))]
 (fn [{:keys           [db]
       ::vcs.subs/keys [vcs]} [_ vims]]
   (let [vcs' (assoc vcs ::vcs.db/playhead-entry nil)
         db'  (vcs.db/add db vcs')]
     {:dispatch  [::timeline.handlers/set-playing vims false]
      :db        db'
      :scheduler {:action :stop}})))

(re-frame/reg-event-fx
 ::toggle-file-visibility
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ file]]
   {:ui-db (ui-db/toggle-file-visibility ui-db file)}))
