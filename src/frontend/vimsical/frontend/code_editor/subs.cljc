(ns vimsical.frontend.code-editor.subs
  (:require
   [re-frame.core :as re-frame]
   [vimsical.vcs.core :as vcs]
   [vimsical.frontend.code-editor.ui-db :as code-editor.ui-db]
   [vimsical.frontend.vcs.subs :as vcs.subs]
   [vimsical.frontend.timeline.subs :as timeline.subs]
   [vimsical.frontend.ui-db :as ui-db]))

(re-frame/reg-sub
 ::active-file
 :<- [::ui-db/ui-db]
 (fn [ui-db _]
   (code-editor.ui-db/get-active-file ui-db)))

(re-frame/reg-sub
 ::timeline-entry
 :<- [::vcs.subs/skimhead-entry]
 :<- [::vcs.subs/playhead-entry]
 :<- [::timeline.subs/skimming?]
 :<- [::timeline.subs/playing?]
 (fn [[skimhead-entry playhead-entry skimming? playing?] _]
   (cond
     skimming? skimhead-entry
     playing?  playhead-entry
     :else     nil)))

(re-frame/reg-sub
 ::string-and-cursor
 :<- [::vcs.subs/vcs]
 :<- [::timeline-entry]
 (fn string-and-cursor-sub
   [[vcs [_ {delta-id :id} :as timeline-entry]] [_ {file-id :db/id}]]
   {:pre [file-id]}
   (when (some? timeline-entry)
     {:string (or (vcs/file-string vcs file-id delta-id) "")
      :cursor (or (vcs/file-cursor vcs file-id delta-id) 0)})))
