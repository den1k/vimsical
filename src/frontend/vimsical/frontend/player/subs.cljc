(ns vimsical.frontend.player.subs
  (:require [re-frame.core :as re-frame]
            [vimsical.frontend.ui-db :as ui-db]
            [vimsical.frontend.vcs.subs :as vcs.subs]
            [vimsical.frontend.timeline.subs :as timeline.subs]
            [vimsical.frontend.code-editor.subs :as code-editor.subs]))

(re-frame/reg-sub
 ::active-file-uid
 :<- [::vcs.subs/skimhead-entry]
 :<- [::vcs.subs/playhead-entry]
 :<- [::vcs.subs/timeline-first-entry]
 (fn [[skimhead-entry playhead-entry timeline-first-entry] _]
   (some-> (or skimhead-entry
               playhead-entry
               timeline-first-entry)
           second
           :file-uid)))

(re-frame/reg-sub
 ::playback-unset?
 :<- [::vcs.subs/playhead-entry]
 :<- [::timeline.subs/playing?]
 :<- [::timeline.subs/skimming?]
 (fn [[entry playing? skimming?] _]
   (and (nil? entry) (not (or playing? skimming?)))))
