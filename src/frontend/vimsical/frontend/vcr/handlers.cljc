(ns vimsical.frontend.vcr.handlers
  #?@(:clj
      [(:require
        [com.stuartsierra.mapgraph :as mg]
        [re-frame.core :as re-frame]
        [re-frame.loggers :refer [console]]
        [vimsical.frontend.timeline.subs :as timeline.subs]
        [vimsical.frontend.util.re-frame :as util.re-frame]
        [vimsical.vcs.core :as vcs]
        [vimsical.vcs.state.timeline :as timeline]
        [vimsical.frontend.vcs.handlers :as vcs.handlers]
        [vimsical.frontend.vcs.subs :as vcs.subs])]
      :cljs
      [(:require
        [com.stuartsierra.mapgraph :as mg]
        [re-frame.core :as re-frame]
        [re-frame.loggers :refer [console]]
        [vimsical.frontend.timeline.subs :as timeline.subs]
        [vimsical.vcs.core :as vcs]
        [vimsical.frontend.util.re-frame :as util.re-frame]
        [vimsical.frontend.util.scheduler :as util.scheduler]
        [vimsical.vcs.state.timeline :as timeline]
        [vimsical.frontend.vcs.handlers :as vcs.handlers]
        [vimsical.frontend.vcs.subs :as vcs.subs])
       (:import [goog.async Delay nextTick])]))


;;
;; * Events
;;

(defn timeline-entry->ms [[_ {:keys [pad]}]] pad)

(re-frame/reg-event-fx
 ::play
 [(re-frame/inject-cofx :scheduler [::next-timeline-entry])
  (util.re-frame/inject-sub [::vcs.subs/vcs])
  (util.re-frame/inject-sub [::vcs.subs/timeline-entry])]
 (fn [{:keys           [db scheduler]
       ::vcs.subs/keys [vcs timeline-entry]}
      _]
   ;; TODO check if we're at the end in which case set to beginning
   (if (some? timeline-entry)
     {:scheduler [{:scheduler scheduler :ms (timeline-entry->ms timeline-entry)}]}
     (let [first-timeline-entry (vcs/timeline-first-entry vcs)
           vcs'                 (assoc vcs ::timeline/current-entry first-timeline-entry)
           ms                   (timeline-entry->ms first-timeline-entry)]
       {:db       (mg/add db vcs')
        :schedule [{:scheduler scheduler :ms ms}]}))))

(re-frame/reg-event-fx
 ::pause
 [(re-frame/inject-cofx :scheduler [::next-timeline-entry])]
 (fn [{:keys [scheduler]} _]
   #?(:cljs {:scheduler (doto scheduler util.scheduler/cancel!)})))

(re-frame/reg-event-fx
 ::next-timeline-entry
 [(re-frame/inject-cofx :scheduler [::next-timeline-entry])
  (util.re-frame/inject-sub [::vcs.subs/vcs])
  (util.re-frame/inject-sub [::vcs.subs/timeline-entry])]
 (fn [{:keys           [db scheduler]
       ::vcs.subs/keys [vcs timeline-entry]}
      _ ]
   (let [[t :as next-timeline-entry] (vcs/timeline-next-entry vcs timeline-entry)
         vcs'                        (assoc vcs ::timeline/current-entry next-timeline-entry)]
     #?(:cljs (println {:dt (- t (util.scheduler/elapsed scheduler))
                        :timeline t :scheduler (util.scheduler/elapsed scheduler)}))
     {:db       (mg/add db vcs')
      :schedule [{:scheduler scheduler
                  :ms        (timeline-entry->ms next-timeline-entry)}]})))
