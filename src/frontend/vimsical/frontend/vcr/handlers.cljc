(ns vimsical.frontend.vcr.handlers
  (:require
   [com.stuartsierra.mapgraph :as mg]
   [re-frame.core :as re-frame]
   [vimsical.frontend.timeline.handlers :as timeline.handlers]
   [vimsical.frontend.util.re-frame :as util.re-frame]
   [vimsical.frontend.vcs.db :as vcs.db]
   [vimsical.frontend.vcs.subs :as vcs.subs]
   [vimsical.frontend.timeline.subs :as timeline.subs]
   [vimsical.vcs.core :as vcs]))

(defn tick-fn
  [t]
  (re-frame/dispatch [::timeline.handlers/set-playhead (int t)]))

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
