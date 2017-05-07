(ns vimsical.frontend.vcr.handlers
  (:require
   [com.stuartsierra.mapgraph :as mg]
   [re-frame.core :as re-frame]
   [vimsical.frontend.code-editor.handlers :as code-editor.handlers]
   [vimsical.frontend.timeline.handlers :as timeline.handlers]
   [vimsical.frontend.util.re-frame :as util.re-frame]
   [vimsical.frontend.util.scheduler :as scheduler]
   [vimsical.frontend.vcs.db :as vcs.db]
   [vimsical.frontend.vcs.subs :as vcs.subs]
   [vimsical.vcs.core :as vcs]))

(defn tick-fn
  [t]
  (re-frame/dispatch [::timeline.handlers/playhead-set t]))

(re-frame/reg-event-fx
 ::play
 [(util.re-frame/inject-sub [::vcs.subs/vcs])]
 (fn [{:keys           [db scheduler]
       ::vcs.subs/keys [vcs timeline-entry]}
      _]
   (let [[t :as entry] (or timeline-entry
                           (vcs/timeline-first-entry vcs))
         vcs'          (assoc vcs ::vcs.db/playhead-entry entry)
         db'           (mg/add db vcs')]
     {:db       db'
      :dispatch [::code-editor.handlers/update-editors]
      ::scheduler/scheduler
      {:action :start
       :t      t
       :event  [::step]
       :tick   tick-fn}})))

(re-frame/reg-event-fx
 ::step
 [(util.re-frame/inject-sub [::vcs.subs/vcs])
  (util.re-frame/inject-sub [::vcs.subs/timeline-entry])]
 (fn [{:keys           [db]
       ::vcs.subs/keys [vcs timeline-entry]} _]
   (if-some [[t :as entry] (vcs/timeline-next-entry vcs timeline-entry)]
     (let [vcs' (assoc vcs ::vcs.db/playhead-entry entry)
           db'  (mg/add db vcs')]
       {:db                   db'
        ::scheduler/scheduler {:t t :event [::step]}
        :dispatch             [::code-editor.handlers/update-editors]})
     {:scheduler {:action :stop}})))

(re-frame/reg-event-fx
 ::pause
 (fn [_ _]
   {::scheduler/scheduler {:action :pause}}))
