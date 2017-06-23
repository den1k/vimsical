(ns vimsical.frontend.landing.handlers
  (:require [re-frame.core :as re-frame]
            [vimsical.frontend.util.re-frame :as util.re-frame]
            [vimsical.vcs.core :as vcs]
            [vimsical.frontend.vcs.subs :as vcs.subs]
            [vimsical.frontend.timeline.subs :as timeline.subs]
            [vimsical.frontend.timeline.handlers :as timeline.handlers]
            [vimsical.frontend.timeline.ui-db :as timeline.ui-db]
            [vimsical.frontend.vcs.db :as vcs.db]
            [com.stuartsierra.mapgraph :as mg]))

(re-frame/reg-event-fx
 ::set-vims-preview-throttle
 (fn [_ [_ vims ratio]]
   {:throttle {:ms       100
               :dispatch [::set-vims-preview vims ratio]}}))

(re-frame/reg-event-fx
 ::set-vims-preview
 [(util.re-frame/inject-sub (fn [[_ vims _]] [::vcs.subs/vcs vims]))
  (util.re-frame/inject-sub (fn [[_ vims _]] [::timeline.subs/duration vims]))]
 (fn [{:as             cofx
       :keys           [db]
       ::vcs.subs/keys [vcs]
       dur             ::timeline.subs/duration}
      [_ vims ratio]]
   (let [ui-time (* ratio dur)
         [entry-time :as entry] (vcs/timeline-entry-at-time vcs ui-time)
         vcs'    (assoc vcs ::vcs.db/playhead-entry entry)]
     {:db (mg/add db vcs')})))

(re-frame/reg-event-fx
 ::set-player-preview-throttle
 (fn [_ [_ vims ratio]]
   {:throttle {:ms       100
               :dispatch [::set-player-preview vims ratio]}}))

(re-frame/reg-event-fx
 ::set-player-preview
 [(re-frame/inject-cofx :ui-db)
  (util.re-frame/inject-sub (fn [[_ vims _]] [::vcs.subs/vcs vims]))
  (util.re-frame/inject-sub (fn [[_ vims _]] [::timeline.subs/duration vims]))]
 (fn [{:as             cofx
       :keys           [db ui-db]
       ::vcs.subs/keys [vcs]
       dur             ::timeline.subs/duration}
      [_ vims ratio]]
   (let [ui-time (* ratio dur)
         [entry-time :as entry] (vcs/timeline-entry-at-time vcs ui-time)
         vcs'    (assoc vcs ::vcs.db/skimhead-entry entry)
         db'     (mg/add db vcs')
         ui-db'  (-> ui-db
                     (timeline.ui-db/set-skimming vims true)
                     (timeline.ui-db/set-skimhead vims ui-time))]
     {:db       db'
      :ui-db    ui-db'
      :debounce {:ms       500
                 :dispatch [::reset-skimming vims]}})))

(re-frame/reg-event-fx
 ::reset-skimming
 [(re-frame/inject-cofx :ui-db)
  (util.re-frame/inject-sub (fn [[_ vims]] [::vcs.subs/vcs vims]))]
 (fn [{:keys [db ui-db]
       vcs   ::vcs.subs/vcs} [_ vims]]
   {:db    (mg/add db (vcs.db/set-skimhead-entry vcs nil))
    :ui-db (-> ui-db
               (timeline.ui-db/set-skimming vims false)
               (timeline.ui-db/set-skimhead vims nil))}))