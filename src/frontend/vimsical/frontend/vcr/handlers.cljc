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

;;
;; * Fx
;;

(defn schedule!
  [ms f]
  #?(:cljs
     (if-not (some-> ms int pos-int?)
       (goog.async.nextTick f)
       (doto (goog.async.Delay. f) (.start ms)))
     :clj
     (.start (Thread. (fn [] (when (pos? ms) (Thread/sleep ms)) (f))))))

;;
;; * Events
;;

(re-frame/reg-fx
 :schedule
 (fn [value]
   (doseq [{:keys [ms dispatch] :as effect} value]
     (if (or (empty? dispatch) (not (number? ms)))
       (console :error "re-frame: ignoring bad :schedule value:" effect)
       (schedule! ms #(re-frame/dispatch dispatch))))))

(re-frame/reg-event-fx
 ::play
 (fn [_ _]
   {:dispatch [::next-delta nil]}))

(re-frame/reg-event-fx
 ::pause
 (fn [_ _]))

(re-frame/reg-event-fx
 ::next-delta
 [(util.re-frame/inject-sub [::timeline.subs/playhead])
  (util.re-frame/inject-sub [::vcs.subs/vims-vcs])]
 (fn [{:as                  cofx
       ::vcs.subs/keys      [vims-vcs]
       ::timeline.subs/keys [playhead]}
      {:keys [pad] :as delta}]
   {:dispatch [::vcs.handlers/set-delta delta]
    ;; :schedule [{:scheduler :ms pad :dispatch [::next-delta delta]}]
    }))
