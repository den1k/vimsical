(ns vimsical.frontend.code-editor.subs
  "Editor subscriptions

  The editor tracks the ::string and ::cursor subs to update its internal
  state. However we do not want that to happen unless we're interacting with the
  timeline. We have a contract with downstream handlers that nil values for
  these subs should be interpreted as a signal NOT to update.

  These subscriptions do the right thing by distinguishing between a nil value
  for ::timeline-entry, meaning the current head is at the beginning of the
  timeline, and a ::sentinel value meaning the ::string and ::cursor should
  return nil so that the editors don't update."
  (:require
   [re-frame.core :as re-frame]
   [vimsical.frontend.code-editor.interop :as interop]
   [vimsical.frontend.code-editor.ui-db :as code-editor.ui-db]
   [vimsical.frontend.timeline.subs :as timeline.subs]
   [vimsical.frontend.ui-db :as ui-db]
   [vimsical.frontend.vcs.subs :as vcs.subs]
   [vimsical.vcs.core :as vcs]))

;;
;; * Handlers
;;

(defn string-handler
  [[vcs timeline-entry] [_ {file-uid :db/uid}]]
  {:pre [file-uid]}
  (when-not (= ::sentinel timeline-entry)
    (let [[_ {delta-uid :uid}] timeline-entry]
      (or (vcs/file-string vcs file-uid delta-uid) ""))))

(defn cursor-handler
  [[vcs timeline-entry] [_ {file-uid :db/uid}]]
  {:pre [file-uid]}
  (when-not (= ::sentinel timeline-entry)
    (let [[_ {delta-uid :uid}] timeline-entry]
      (or (vcs/file-cursor vcs file-uid delta-uid) 0))))

(defn position-handler
  [[cursor string] [_ {file-uid :db/uid}]]
  (when (and cursor string)
    (interop/idx->pos cursor string)))

;;
;; * Subscriptions
;;

(re-frame/reg-sub
 ::active-file
 :<- [::ui-db/ui-db]
 (fn [ui-db _]
   (code-editor.ui-db/get-active-file ui-db)))

(re-frame/reg-sub
 ::editor-instance-for-subtype
 (fn [[_ sub-type]]
   [(re-frame/subscribe [::ui-db/ui-db])
    (re-frame/subscribe [::vcs.subs/file-for-subtype sub-type])])
 (fn [[ui-db file] _]
   (code-editor.ui-db/get-editor ui-db file)))

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
     :else     ::sentinel)))

(re-frame/reg-sub ::string          :<- [::vcs.subs/vcs] :<- [::timeline-entry] string-handler)
(re-frame/reg-sub ::cursor          :<- [::vcs.subs/vcs] :<- [::timeline-entry] cursor-handler)
(re-frame/reg-sub ::playhead-string :<- [::vcs.subs/vcs] :<- [::vcs.subs/playhead-entry] string-handler)
(re-frame/reg-sub ::playhead-cursor :<- [::vcs.subs/vcs] :<- [::vcs.subs/playhead-entry] cursor-handler)

(re-frame/reg-sub
 ::position
 (fn [[_ file]]
   [(re-frame/subscribe [::cursor file])
    (re-frame/subscribe [::string file])])
 position-handler)

(re-frame/reg-sub
 ::playhead-position
 (fn [[_ file]]
   [(re-frame/subscribe [::playhead-cursor file])
    (re-frame/subscribe [::playhead-string file])])
 position-handler)
