(ns vimsical.frontend.vcs.subs
  (:require
   [com.stuartsierra.mapgraph :as mg]
   [re-frame.core :as re-frame]
   [vimsical.frontend.vcs.queries :as queries]
   [vimsical.vcs.core :as vcs]
   [vimsical.vcs.file :as file]
   [vimsical.frontend.util.preprocess.core :as preprocess]
   [vimsical.frontend.util.lint.core :as lint]))

(defn vims-vcs [db]
  (:vims/vcs
   (mg/pull-link db queries/vims-vcs :app/vims)))

(defn file-string [vcs {:keys [db/id] :as file}]
  (vcs/file-string vcs id))

(re-frame/reg-sub
 ::vims-vcs
 (fn [db [_]]
   (vims-vcs db)))

(re-frame/reg-sub
 ::file-string
 :<- [::vims-vcs]
 (fn [vcs [_ file]]
   (file-string vcs file)))

(re-frame/reg-sub
 ::preprocessed-file-data
 (fn [[_ file]]
   (re-frame/subscribe [::file-string file]))
 (fn [string [_ file]]
   ;; TODO handle errors
   (preprocess/preprocess file string)))

(re-frame/reg-sub
 ::preprocessed-file-string
 (fn [[_ file]]
   (re-frame/subscribe [::preprocessed-file-data file]))
 (fn [{::preprocess/keys [string error]} [_ file]]
   string))

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