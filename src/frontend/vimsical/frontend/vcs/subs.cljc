(ns vimsical.frontend.vcs.subs
  (:require
   [com.stuartsierra.mapgraph :as mg]
   [re-frame.core :as re-frame]
   [vimsical.frontend.vcs.db :as db]
   [vimsical.frontend.vcs.queries :as queries]
   [vimsical.vcs.core :as vcs]
   [vimsical.vcs.file :as file]
   [vimsical.frontend.util.preprocess.core :as preprocess]
   [vimsical.frontend.util.lint.core :as lint]
   [vimsical.vcs.state.timeline :as timeline]))

;;
;; * VCS
;;

(re-frame/reg-sub
 ::vcs
 (fn [db [vims]]
   (if-some [lookup-ref (mg/ref-to db vims)]
     (-> db
         (mg/pull queries/vims-vcs lookup-ref)
         (get-in [:vims/vcs]))
     (-> db
         (mg/pull [{[:app/vims '_] queries/vims-vcs}])
         (get-in [:app/vims :vims/vcs])))))

;;
;; * Heads (timeline entries)
;;

(re-frame/reg-sub ::playhead-entry :<- [::vcs] (fn [vcs _] (::db/playhead-entry vcs)))
(re-frame/reg-sub ::skimhead-entry :<- [::vcs] (fn [vcs _] (::db/skimhead-entry vcs)))

(re-frame/reg-sub
 ::timeline-entry
 :<- [::playhead-entry]
 :<- [::skimhead-entry]
 (fn [[playhead-entry skimhead-entry] _]
   (or skimhead-entry playhead-entry)))

;;
;; * Files
;;

(re-frame/reg-sub
 ::file
 (fn [db [_ file-id]]
   (mg/pull db queries/file [:db/id file-id])))

(re-frame/reg-sub
 ::file-string
 :<- [::vcs]
 :<- [::timeline-entry]
 (fn [[vcs [_ {delta-id id}]] [_ {file-id :id}]]
   (vcs/file-string vcs file-id delta-id)))

(re-frame/reg-sub
 ::preprocessed-file-data
 (fn [[_ file]]
   (re-frame/subscribe [::file-string file]))
 (fn [string [_ file]]
   ;; TODO handle errors
   (preprocess/preprocess file string)))

(re-frame/reg-sub
 ::file-cursor
 :<- [::vcs]
 (fn [vcs [_ {:keys [db/id] :as file}]]
   (vcs/file-cursor vcs id)))

(re-frame/reg-sub
 ::preprocessed-file-string
 (fn [[_ file]]
   (re-frame/subscribe [::preprocessed-file-data file]))
 (fn [{::preprocess/keys [string error]} [_ file]]
   string))

(re-frame/reg-sub
 ::timeline-chunks-by-absolute-start-time
 :<- [::vcs]
 (fn [vcs _]
   (vcs/timeline-chunks-by-absolute-start-time vcs)))


(re-frame/reg-sub
 ::file-lint-data
 (fn [[_ file]]
   (re-frame/subscribe [::file-string file]))
 (fn [string [_ file]]
   (lint/lint file string)))

(re-frame/reg-sub
 ::file-lint-errors
 (fn [[_ file]]
   (re-frame/subscribe [::file-lint-data file]))
 (fn [{::lint/keys [errors]} [_ file]]
   errors))

(re-frame/reg-sub
 ::file-lint-or-preprocessing-errors
 (fn [[_ file]]
   [(re-frame/subscribe [::preprocessed-file-data file])
    (re-frame/subscribe [::file-lint-data file])])
 (fn [[preprocessed linted] [_ file]]
   (or (some-> preprocessed ::preprocess/error vector)
       (::lint/errors linted))))

(re-frame/reg-sub
 ::timeline-duration
 :<- [::vcs]
 (fn [vcs _]
   (vcs/timeline-duration vcs)))

(re-frame/reg-sub
 ::timeline-entry
 :<- [::vcs]
 (fn [vcs _]
   (get vcs ::timeline/current-entry)))
