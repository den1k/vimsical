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
  [[vcs timeline-entry] [_ _ {file-uid :db/uid}]]
  {:pre [file-uid]}
  (when-not (= ::sentinel timeline-entry)
    (let [[_ {delta-uid :uid}] timeline-entry]
      (or (vcs/file-string vcs file-uid delta-uid) ""))))

(defn cursor-handler
  [[vcs timeline-entry] [_ _ {file-uid :db/uid}]]
  {:pre [file-uid]}
  (when-not (= ::sentinel timeline-entry)
    (let [[_ {delta-uid :uid}] timeline-entry]
      (or (vcs/file-cursor vcs file-uid delta-uid) 0))))

(defn position-handler
  [[cursor string] [_ _ {file-uid :db/uid}]]
  (when (and cursor string)
    (interop/idx->pos cursor string)))

;;
;; * Subscriptions
;;

(re-frame/reg-sub
 ::editor-instance-for-subtype
 (fn [[_ vims sub-type]]
   [(re-frame/subscribe [::ui-db/ui-db])
    (re-frame/subscribe [::vcs.subs/file-for-subtype vims sub-type])])
 (fn [[ui-db file] [_ vims]]
   (code-editor.ui-db/get-editor ui-db {:vims vims :file file})))

(re-frame/reg-sub
 ::timeline-entry
 (fn [[_ vims]]
   [(re-frame/subscribe [::vcs.subs/skimhead-entry vims])
    (re-frame/subscribe [::vcs.subs/playhead-entry vims])
    (re-frame/subscribe [::timeline.subs/skimming? vims])
    (re-frame/subscribe [::timeline.subs/playing? vims])])
 (fn [[skimhead-entry playhead-entry skimming? playing?] _]
   (cond
     skimming? skimhead-entry
     playing? playhead-entry
     :else ::sentinel)))

(defn- timeline-entry-subs [[_ vims _]]
  [(re-frame/subscribe [::vcs.subs/vcs vims])
   (re-frame/subscribe [::timeline-entry vims])])

(re-frame/reg-sub ::string timeline-entry-subs string-handler)
(re-frame/reg-sub ::cursor timeline-entry-subs cursor-handler)

(defn- playhead-entry-subs [[_ vims _]]
  [(re-frame/subscribe [::vcs.subs/vcs vims])
   (re-frame/subscribe [::vcs.subs/playhead-entry vims])])

(re-frame/reg-sub ::playhead-string playhead-entry-subs string-handler)
(re-frame/reg-sub ::playhead-cursor playhead-entry-subs cursor-handler)

(re-frame/reg-sub
 ::position
 (fn [[_ vims file]]
   [(re-frame/subscribe [::cursor vims file])
    (re-frame/subscribe [::string vims file])])
 position-handler)

(re-frame/reg-sub
 ::playhead-position
 (fn [[_ vims file]]
   [(re-frame/subscribe [::playhead-cursor vims file])
    (re-frame/subscribe [::playhead-string vims file])])
 position-handler)