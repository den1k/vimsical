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

(re-frame/reg-sub ::playhead  :<- [::ui-db/ui-db] (fn [{::timeline.ui-db/keys [playhead]} _] (or playhead 0)))
(re-frame/reg-sub ::skimhead  :<- [::ui-db/ui-db] (fn [{::timeline.ui-db/keys [skimhead]} _] skimhead))
(re-frame/reg-sub ::playing?  :<- [::ui-db/ui-db] (fn [{::timeline.ui-db/keys [playing?]} _] (boolean playing?)))
(re-frame/reg-sub ::skimming? :<- [::ui-db/ui-db] (fn [{::timeline.ui-db/keys [skimming?]} _] (boolean skimming?)))

;;
;; * Db
;;

(re-frame/reg-sub
 ::chunks-by-absolute-start-time
 :<- [::vcs.subs/vcs]
 (fn [vcs _]
   (vcs/timeline-chunks-by-absolute-start-time vcs)))

(re-frame/reg-sub
 ::duration
 :<- [::vcs.subs/vcs]
 (fn [vcs _]
   (or (vcs/timeline-duration vcs) 0)))
