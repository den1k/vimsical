(ns vimsical.frontend.code-editor.subs
  (:require
   [re-frame.core :as re-frame]
   [vimsical.vcs.core :as vcs]
   [vimsical.frontend.code-editor.ui-db :as code-editor.ui-db]
   [vimsical.frontend.code-editor.interop :as interop]
   [vimsical.frontend.vcs.subs :as vcs.subs]
   [vimsical.frontend.timeline.subs :as timeline.subs]
   [vimsical.frontend.ui-db :as ui-db]))

(re-frame/reg-sub
 ::active-file
 :<- [::ui-db/ui-db]
 (fn [ui-db _]
   (code-editor.ui-db/get-active-file ui-db)))

;; NOTE this is different from vcs.subs/timeline-entry since the code-editors
;; are tracking this sub to set their state and we want to disable that behavior
;; while the editor is currently active

;; NOTE 2: the behavior is currently incorrect since skimming while not playing
;; will end up setting the state of the editor, we'll need something more
;; fine-grained that takes into account the interactions with the editor
;; elements

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
 ::string
 :<- [::vcs.subs/vcs]
 :<- [::timeline-entry]
 (fn string-sub
   [[vcs [_ {delta-id :id} :as timeline-entry]] [_ {file-id :db/id}]]
   {:pre [file-id]}
   (when (some? timeline-entry)
     (vcs/file-string vcs file-id delta-id))))

(re-frame/reg-sub
 ::cursor
 :<- [::vcs.subs/vcs]
 :<- [::timeline-entry]
 (fn cursor-sub
   [[vcs [_ {delta-id :id} :as timeline-entry]] [_ {file-id :db/id}]]
   {:pre [file-id]}
   (when (some? timeline-entry)
     (vcs/file-cursor vcs file-id delta-id))))

(re-frame/reg-sub
 ::position
 (fn [[_ file]]
   [(re-frame/subscribe [::cursor file])
    (re-frame/subscribe [::string file])])
 (fn position-sub
   [[cursor string] [_ {file-id :db/id}]]
   (when (and cursor string)
     (interop/idx->js-pos cursor string))))
