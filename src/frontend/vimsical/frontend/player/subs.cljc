(ns vimsical.frontend.player.subs
  (:require [re-frame.core :as re-frame]
            [vimsical.frontend.ui-db :as ui-db]
            [vimsical.frontend.vcs.subs :as vcs.subs]
            [vimsical.frontend.timeline.subs :as timeline.subs]
            [vimsical.frontend.code-editor.subs :as code-editor.subs]))

(re-frame/reg-sub
 ::active-file-id
 :<- [::vcs.subs/skimhead-entry]
 :<- [::vcs.subs/playhead-entry]
 (fn [[skimhead-entry playhead-entry] _]
   (some-> (or skimhead-entry playhead-entry) second :file-id)))

(re-frame/reg-sub
 ::playback-unset?
 :<- [::vcs.subs/playhead-entry]
 :<- [::timeline.subs/playing?]
 :<- [::timeline.subs/skimming?]
 (fn [[entry playing? skimming?] _]
   (and (nil? entry) (not (or playing? skimming?)))))