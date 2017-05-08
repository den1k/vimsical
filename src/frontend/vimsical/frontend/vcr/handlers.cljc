(ns vimsical.frontend.vcr.handlers
  (:require
   [com.stuartsierra.mapgraph :as mg]
   [re-frame.core :as re-frame]
   [vimsical.frontend.timeline.handlers :as timeline.handlers]
   [vimsical.frontend.util.re-frame :as util.re-frame]
   [vimsical.frontend.vcs.db :as vcs.db]
   [vimsical.frontend.vcs.subs :as vcs.subs]
   [vimsical.vcs.core :as vcs]))

(defn tick-fn
  [t]
  (re-frame/dispatch [::timeline.handlers/set-playhead t]))

(re-frame/reg-event-fx
 ::play
 [(util.re-frame/inject-sub [::vcs.subs/vcs])]
 (fn [{:keys           [db scheduler]
       ::vcs.subs/keys [vcs timeline-entry]}
      _]
   (if (some? timeline-entry)
     (let [[t] timeline-entry ]
       {:dispatch [::timeline.handlers/set-playing true]
        :scheduler
        [{:action :start
          :t      t
          :event  [::step]
          :tick   tick-fn}]})
     (let [[t {:keys [pad]} :as entry] (vcs/timeline-first-entry vcs)
           vcs'                        (assoc vcs ::vcs.db/playhead-entry entry)
           db'                         (mg/add db vcs')]
       {:db       db'
        :dispatch [::timeline.handlers/set-playing true]
        :scheduler
        [{:action :start
          :t      (- t pad)
          :event  [::step]
          :tick   tick-fn}]}))))

(re-frame/reg-event-fx
 ::step
 [(util.re-frame/inject-sub [::vcs.subs/vcs])
  (util.re-frame/inject-sub [::vcs.subs/playhead-entry])]
 (fn [{:keys           [db]
       ::vcs.subs/keys [vcs playhead-entry]} _]
   (if-some [[t :as entry] (some->> playhead-entry (vcs/timeline-next-entry vcs))]
     (let [vcs' (assoc vcs ::vcs.db/playhead-entry entry)
           db'  (mg/add db vcs')]
       {:db db' :scheduler {:t t :event [::step]}})
     {:dispatch [::stop]})))

(re-frame/reg-event-fx
 ::pause
 (fn [_ _]
   {:dispatch  [::timeline.handlers/set-playing false]
    :scheduler {:action :pause}}))

(re-frame/reg-event-fx
 ::stop
 (fn [_ _]
   {:dispatch  [::timeline.handlers/set-playing false]
    :scheduler {:action :stop}}))

