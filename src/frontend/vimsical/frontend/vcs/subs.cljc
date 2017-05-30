(ns vimsical.frontend.vcs.subs
  (:require
   [vimsical.vims :as vims]
   [com.stuartsierra.mapgraph :as mg]
   [re-frame.core :as re-frame]
   [vimsical.frontend.util.re-frame :refer [<sub]]
   [vimsical.common.util.core :as util :include-macros true]
   [vimsical.frontend.util.lint.core :as lint]
   [vimsical.frontend.util.preprocess.core :as preprocess]
   [vimsical.frontend.vcs.db :as db]
   [vimsical.frontend.vcs.queries :as queries]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.file :as file]
   [vimsical.vcs.core :as vcs]
   [re-frame.interop :as interop]
   [vimsical.frontend.util.re-frame :as util.re-frame]
   [clojure.spec :as s]))

;;
;; * VCS
;;

(re-frame/reg-sub
 ::vcs
 (fn [db [_ vims :as event]]
   (if-some [lookup-ref (mg/ref-to db vims)]
     (-> db
         (mg/pull queries/vims-vcs lookup-ref)
         (get-in [::vims/vcs]))
     (do
       (re-frame.loggers/console :error (ex-info "No Vims" {:event event}))
       (-> db
           (mg/pull [{[:app/vims '_] queries/vims-vcs}])
           (get-in [:app/vims ::vims/vcs]))))))

;;
;; * Branch
;;

(defn- vims-vcs-sub [[_ vims]]
  (re-frame/subscribe [::vcs vims]))

(re-frame/reg-sub
 ::branches
 vims-vcs-sub
 (fn [{::vcs/keys [branches]}] branches))

(re-frame/reg-sub
 ::branch-uid
 vims-vcs-sub
 (fn [{::db/keys [branch-uid]}] branch-uid))

(re-frame/reg-sub
 ::branch
 (fn [[_ vims]]
   [(re-frame/subscribe [::branch-uid vims])
    (re-frame/subscribe [::branches vims])])
 (fn [[branch-uid branches] _]
   (util/ffilter (partial util/=by identity :db/uid branch-uid) branches)))

;;
;; * Heads (timeline entries)
;;

(re-frame/reg-sub
 ::skimhead-entry
 vims-vcs-sub
 (fn [vcs _] (some-> vcs db/get-skimhead-entry)))

(re-frame/reg-sub
 ::playhead-entry
 vims-vcs-sub
 (fn [vcs _] (some-> vcs db/get-playhead-entry)))

(re-frame/reg-sub
 ::timeline-first-entry
 vims-vcs-sub
 (fn [vcs _] (some-> vcs vcs/timeline-first-entry)))

(re-frame/reg-sub
 ::timeline-entry
 (fn [[_ vims]]
   [(re-frame/subscribe [::skimhead-entry vims])
    (re-frame/subscribe [::playhead-entry vims])])
 (fn [[skimhead-entry playhead-entry] _]
   (or skimhead-entry playhead-entry)))

;;
;; * Files
;;

(re-frame/reg-sub
 ::files
 (fn [[_ vims]] (re-frame/subscribe [::branch vims]))
 (fn [{::branch/keys [files]}] files))

(re-frame/reg-sub
 ::file-for-subtype
 (fn [[_ vims]] (re-frame/subscribe [::files vims]))
 (fn [files [_ _ sub-type]]
   {:pre [sub-type]}
   (util/ffilter (fn [file] (= (::file/sub-type file) sub-type)) files)))

(re-frame/reg-sub
 ::file
 (fn [db [_ file-uid]]
   {:pre [file-uid]}
   (mg/pull db queries/file [:db/uid file-uid])))

(defn- vcs-and-timeline-entry-subs [[_ vims]]
  [(re-frame/subscribe [::vcs vims])
   (re-frame/subscribe [::timeline-entry vims])])

(re-frame/reg-sub
 ::file-string
 vcs-and-timeline-entry-subs
 (fn [[vcs [_ {delta-uid :uid}]] [_ _ {file-uid :db/uid}]]
   (when (and file-uid delta-uid)
     (vcs/file-string vcs file-uid delta-uid))))

(re-frame/reg-sub
 ::file-cursor
 vcs-and-timeline-entry-subs
 (fn [[vcs [_ {delta-uid :uid}]] [_ _ {file-uid :db/uid}]]
   (when (and file-uid delta-uid)
     (vcs/file-cursor vcs file-uid delta-uid))))

;;
;; * Pre-processors
;;

;; TODO handle errors
(re-frame/reg-sub
 ::preprocessed-file-data
 (fn [[_ vims file]]
   (re-frame/subscribe [::file-string vims file]))
 (fn [string [_ file]]
   (when string
     (preprocess/preprocess file string))))

(re-frame/reg-sub
 ::preprocessed-file-string
 (fn [[_ vims file]]
   (re-frame/subscribe [::preprocessed-file-data vims file]))
 (fn [{::preprocess/keys [string error]} _]
   string))

;;
;; * Linters
;;

(re-frame/reg-sub
 ::file-lint-data
 (fn [[_ vims file]]
   (re-frame/subscribe [::file-string vims file]))
 (fn [string [_ file]]
   (when string
     (lint/lint file string))))

(re-frame/reg-sub
 ::file-lint-errors
 (fn [[_ vims file]]
   (re-frame/subscribe [::file-lint-data vims file]))
 (fn [{::lint/keys [errors]} _]
   errors))

(re-frame/reg-sub
 ::file-lint-or-preprocessing-errors
 (fn [[_ vims file]]
   [(re-frame/subscribe [::preprocessed-file-data vims file])
    #_(re-frame/subscribe [::file-lint-data file])])
 (fn [[preprocessed {::lint/keys [errors]}] _]
   (or (some-> preprocessed ::preprocess/error vector)
       errors)))
