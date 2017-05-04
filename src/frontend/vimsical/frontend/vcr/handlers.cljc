(ns vimsical.frontend.vcr.handlers
  #?@(:clj
      [(:require
        [re-frame.core :as re-frame]
        [re-frame.loggers :refer [console]]
        [vimsical.frontend.timeline.subs :as timeline.subs]
        [vimsical.frontend.util.re-frame :as util.re-frame]
        [vimsical.frontend.vcs.handlers :as vcs.handlers]
        [vimsical.frontend.vcs.subs :as vcs.subs])]
      :cljs
      [(:require
        [re-frame.core :as re-frame]
        [re-frame.loggers :refer [console]]
        [vimsical.frontend.timeline.subs :as timeline.subs]
        [vimsical.frontend.util.re-frame :as util.re-frame]
        [vimsical.frontend.vcs.handlers :as vcs.handlers]
        [vimsical.frontend.vcs.subs :as vcs.subs])
       (:import [goog.async Delay nextTick])]))

(defonce scheduler
  (atom {::play? false}))

(defn schedule!
  [ms event]
  #?(:cljs
     (doto (goog.async.Delay. #(re-frame/dispatch event))
       (.start ms))))

(re-frame/reg-cofx
 :scheduler
 (fn [context]
   (assoc context :scheduler @scheduler)))

(re-frame/reg-fx
 :schedule
 (fn [value]
   (doseq [{:keys [ms event] :as effect} value]
     (if (or (empty? event) (not (number? ms)))
       (console :error "re-frame: ignoring bad :schedule value:" effect)
       (schedule! ms event)))))

;;
;; * Events
;;


(re-frame/reg-event-fx
 ::play
 [(re-frame/inject-cofx :scheduler)]
 (fn [{:keys [scheduler]} _]
   {:scheduler (assoc scheduler ::play? true)
    :dispatch  [::tick]}))

(re-frame/reg-event-fx
 ::pause
 [(re-frame/inject-cofx :scheduler)]
 (fn [{:keys [scheduler]} _]
   {:scheduler (assoc scheduler ::play? false)}))

(re-frame/reg-event-fx
 ::tick
 [(re-frame/inject-cofx :scheduler)
  ;; (util.re-frame/inject-sub [::timeline.subs/playhead])
  (util.re-frame/inject-sub [::vcs.subs/delta])]
 (fn [{:as             cofx
       ::vcs.subs/keys [vcs]}
      {:keys [pad] :as delta}]
   {:dispatch [::vcs.handlers/set-delta delta]
    :schedule [{:ms pad :dispatch [::tick delta]}]}))
