(ns vimsical.frontend.player.subs
  (:require [re-frame.core :as re-frame]
            [vimsical.frontend.ui-db :as ui-db]
            [vimsical.frontend.vcs.subs :as vcs.subs]
            [vimsical.frontend.timeline.subs :as timeline.subs]
            [vimsical.frontend.code-editor.subs :as code-editor.subs]))

(re-frame/reg-sub
 ::active-file-id
 :<- [::code-editor.subs/timeline-entry]
 :<- [::vcs.subs/playhead-entry]
 (fn [[timeline-entry playhead-entry] _]
   ;; todo this should be timeline entry instead
   (some-> playhead-entry second :file-id)))

(re-frame/reg-sub
 ::playback-unset?
 :<- [::vcs.subs/playhead-entry]
 :<- [::timeline.subs/playing?]
 (fn [[entry playing?] _]
   (and (nil? entry) (not playing?))))