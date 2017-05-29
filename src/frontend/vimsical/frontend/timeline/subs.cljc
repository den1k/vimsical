(ns vimsical.frontend.timeline.subs
  (:require
   [re-frame.core :as re-frame]
   [vimsical.frontend.timeline.ui-db :as timeline.ui-db]
   [vimsical.frontend.ui-db :as ui-db]
   [vimsical.frontend.vcs.subs :as vcs.subs]
   [vimsical.vcs.core :as vcs]))

;;
;; * UI Db
;;

(re-frame/reg-sub
 ::playhead
 (fn [[_ {vims-uid :db/uid}]] (re-frame/subscribe [::ui-db/ui-db vims-uid]))
 (fn [{::timeline.ui-db/keys [playhead]} _] (or playhead 0)))

(re-frame/reg-sub
 ::skimhead
 (fn [[_ {vims-uid :db/uid}]] (re-frame/subscribe [::ui-db/ui-db vims-uid]))
 (fn [{::timeline.ui-db/keys [skimhead]} _]
   skimhead))

(re-frame/reg-sub
 ::playing?
 (fn [[_ {vims-uid :db/uid}]] (re-frame/subscribe [::ui-db/ui-db vims-uid]))
 (fn [{::timeline.ui-db/keys [playing?]} _] (boolean playing?)))

(re-frame/reg-sub
 ::skimming?
 (fn [[_ {vims-uid :db/uid}]] (re-frame/subscribe [::ui-db/ui-db vims-uid]))
 (fn [{::timeline.ui-db/keys [skimming?]} _] (boolean skimming?)))

(re-frame/reg-sub
 ::time
 [(fn [[_ vims]]
    [(re-frame/subscribe [::playhead vims])
     (re-frame/subscribe [::skimhead vims])])]
 (fn [[skimhead playhead] _]
   (or skimhead playhead)))

;;
;; * Db
;;

(re-frame/reg-sub
 ::chunks-by-absolute-start-time
 (fn [[_ vims]] (re-frame/subscribe [::vcs.subs/vcs vims]))
 (fn [vcs _] (vcs/timeline-chunks-by-absolute-start-time vcs)))

(re-frame/reg-sub
 ::duration
 (fn [[_ vims]] (re-frame/subscribe [::vcs.subs/vcs vims]))
 (fn [vcs _] (or (vcs/timeline-duration vcs) 0)))
