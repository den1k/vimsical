(ns vimsical.frontend.vcs.subs
  (:require
   [vimsical.vims :as vims]
   [com.stuartsierra.mapgraph :as mg]
   [re-frame.core :as re-frame]
   [vimsical.common.util.core :as util]
   [vimsical.frontend.util.lint.core :as lint]
   [vimsical.frontend.util.preprocess.core :as preprocess]
   [vimsical.frontend.vcs.db :as db]
   [vimsical.frontend.vcs.queries :as queries]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.file :as file]
   [vimsical.vcs.core :as vcs]))

;;
;; * VCS
;;

(re-frame/reg-sub
 ::vcs
 (fn [db [_ vims]]
   (if-some [lookup-ref (mg/ref-to db vims)]
     (-> db
         (mg/pull queries/vims-vcs lookup-ref)
         (get-in [::vims/vcs]))
     (-> db
         (mg/pull [{[:app/vims '_] queries/vims-vcs}])
         (get-in [:app/vims ::vims/vcs])))))

;;
;; * Branch
;;

(re-frame/reg-sub ::branches  :<- [::vcs] (fn [{::vcs/keys [branches]}] branches))
(re-frame/reg-sub ::branch-uid :<- [::vcs] (fn [{::db/keys [branch-uid]}] branch-uid))

(re-frame/reg-sub
 ::branch
 :<- [::branch-uid]
 :<- [::branches]
 (fn [[branch-uid branches] _]
   (util/ffilter
    (partial util/=by identity :db/uid branch-uid)
    branches)))

;;
;; * Heads (timeline entries)
;;

(re-frame/reg-sub ::skimhead-entry :<- [::vcs] (fn [vcs _] (some-> vcs db/get-skimhead-entry)))
(re-frame/reg-sub ::playhead-entry :<- [::vcs] (fn [vcs _] (some-> vcs db/get-playhead-entry)))
(re-frame/reg-sub ::timeline-first-entry :<- [::vcs] (fn [vcs _] (some-> vcs vcs/timeline-first-entry)))

(re-frame/reg-sub
 ::timeline-entry
 :<- [::skimhead-entry]
 :<- [::playhead-entry]
 (fn [[skimhead-entry playhead-entry] _]
   (or skimhead-entry playhead-entry)))

;;
;; * Files
;;

(re-frame/reg-sub ::files :<- [::branch] (fn [{::branch/keys [files]}] files))

(re-frame/reg-sub
 ::file-for-subtype
 :<- [::files]
 (fn [files [_ sub-type]]
   {:pre [sub-type]}
   (util/ffilter (fn [file] (= (::file/sub-type file) sub-type)) files)))

(re-frame/reg-sub
 ::file
 (fn [db [_ file-uid]]
   {:pre [file-uid]}
   (mg/pull db queries/file [:db/uid file-uid])))

(re-frame/reg-sub
 ::file-string
 :<- [::vcs]
 :<- [::timeline-entry]
 (fn [[vcs [_ {delta-uid :uid}]] [_ {file-uid :db/uid}]]
   (when (and file-uid delta-uid)
     (vcs/file-string vcs file-uid delta-uid))))

(re-frame/reg-sub
 ::file-cursor
 :<- [::vcs]
 :<- [::timeline-entry]
 (fn [[vcs [_ {delta-uid :uid}]] [_ {file-uid :db/uid}]]
   (when (and file-uid delta-uid)
     (vcs/file-cursor vcs file-uid delta-uid))))

;;
;; * Pre-processors
;;

;; TODO handle errors
(re-frame/reg-sub
 ::preprocessed-file-data
 (fn [[_ file]]
   (re-frame/subscribe [::file-string file]))
 (fn [string [_ file]]
   (when string
     (preprocess/preprocess file string))))

(re-frame/reg-sub
 ::preprocessed-file-string
 (fn [[_ file]]
   (re-frame/subscribe [::preprocessed-file-data file]))
 (fn [{::preprocess/keys [string error]} _]
   string))

;;
;; * Linters
;;

(re-frame/reg-sub
 ::file-lint-data
 (fn [[_ file]]
   (re-frame/subscribe [::file-string file]))
 (fn [string [_ file]]
   (when string
     (lint/lint file string))))

(re-frame/reg-sub
 ::file-lint-errors
 (fn [[_ file]]
   (re-frame/subscribe [::file-lint-data file]))
 (fn [{::lint/keys [errors]} _]
   errors))

(re-frame/reg-sub
 ::file-lint-or-preprocessing-errors
 (fn [[_ file]]
   [(re-frame/subscribe [::preprocessed-file-data file])
    #_(re-frame/subscribe [::file-lint-data file])])
 (fn [[preprocessed {::lint/keys [errors]}] _]
   (or (some-> preprocessed ::preprocess/error vector)
       errors)))
