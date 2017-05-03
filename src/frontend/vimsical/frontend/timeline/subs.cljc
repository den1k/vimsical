(ns vimsical.frontend.timeline.subs
  (:require
   [clojure.spec :as s]
   [re-frame.core :as re-frame]
   [vimsical.frontend.vcs.subs :as vcs.subs]
   [vimsical.vcs.core :as vcs]
   [vimsical.vcs.state.timeline :as timeline]))

(s/def ::timeline/playhead nat-int?)
(s/def ::timeline/skimhead nat-int?)

(re-frame/reg-sub
 ::timeline
 :<- [::vcs.subs/vcs]
 (fn [{::vcs/keys [timeline] :as vcs} _] timeline))

(re-frame/reg-sub
 ::playhead
 :<- [::timeline]
 (fn [{::timeline/keys [playhead]} _] (or playhead 0)))

(re-frame/reg-sub
 ::skimhead
 :<- [::timeline]
 (fn [{::timeline/keys [skimhead]} _] (or skimhead 0)))
