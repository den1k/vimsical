(ns vimsical.frontend.player.handlers
  (:require [re-frame.core :as re-frame]
            [vimsical.frontend.util.re-frame :as util.re-frame]
            [vimsical.vcs.core :as vcs]
            [vimsical.frontend.vcs.subs :as vcs.subs]
            [vimsical.frontend.timeline.ui-db :as timeline.ui-db]
            [vimsical.frontend.vcs.db :as vcs.db]
            [vimsical.frontend.vcr.handlers :as vcr.handlers]
            [com.stuartsierra.mapgraph :as mg]
            [vimsical.frontend.timeline.handlers :as timeline.handlers]
            [vimsical.frontend.timeline.subs :as timeline.subs]
            [vimsical.frontend.code-editor.handlers :as code-editor.handlers]))

(re-frame/reg-event-fx
 ::on-click
 [(re-frame/inject-cofx :ui-db)
  (util.re-frame/inject-sub (fn [[_ vims]] [::vcs.subs/vcs vims]))]
 (fn [{:as             cofx
       :keys           [db ui-db]
       ::vcs.subs/keys [vcs]}
      [_ vims ui-time]]
   (let [[entry-time :as entry] (vcs/timeline-entry-at-time vcs ui-time)
         vcs'   (assoc vcs ::vcs.db/playhead-entry entry)
         db'    (mg/add db vcs')
         ui-db' (timeline.ui-db/set-playhead ui-db vims ui-time)]
     {:db        db'
      :ui-db     ui-db'
      :scheduler [{:action :set-time :t ui-time}
                  {:override? true :t entry-time :event [::vcr.handlers/step vims]}]})))

;;
;; ** Skimhead
;;

(re-frame/reg-event-fx
 ::on-mouse-enter
 [(re-frame/inject-cofx :ui-db)
  (util.re-frame/inject-sub (fn [[_ vims]] [::vcs.subs/vcs vims]))]
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
  (util.re-frame/inject-sub (fn [[_ vims]] [::vcs.subs/vcs vims]))]
 (fn [{:keys           [db ui-db]
       ::vcs.subs/keys [vcs]} [_ vims ui-time]]
   (let [entry  (vcs/timeline-entry-at-time vcs ui-time)
         vcs'   (vcs.db/set-skimhead-entry vcs entry)
         db'    (mg/add db vcs')
         ui-db' (timeline.ui-db/set-skimhead ui-db vims ui-time)]
     {:db db' :ui-db ui-db'})))

(re-frame/reg-event-fx
 ::on-mouse-leave
 [(re-frame/inject-cofx :ui-db)
  (util.re-frame/inject-sub (fn [[_ vims]] [::vcs.subs/vcs vims]))]
 (fn [{:keys [ui-db db] ::vcs.subs/keys [vcs]} [_ vims]]
   (let [vcs'   (vcs.db/set-skimhead-entry vcs nil)
         db'    (mg/add db vcs')
         ui-db' (-> ui-db
                    (timeline.ui-db/set-skimhead vims nil)
                    (timeline.ui-db/set-skimming vims false))]
     {:db       db'
      :ui-db    ui-db'
      :dispatch [::code-editor.handlers/reset-all-editors-to-playhead vims]})))
