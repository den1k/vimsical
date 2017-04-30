(ns vimsical.frontend.vcr.subs
  (:require
   [re-frame.core :as re-frame]
   [vimsical.vcs.branch :as branch]))

(re-frame/reg-sub
 ::files
 (fn [[_ pattern]]
   (re-frame/subscribe
    [:q*
     [{:vims/branches ['* {:vimsical.vcs.branch/files (or pattern '[*])}]}]
     :app/vims]))
 (fn [{:vims/keys [branches]}]
   (->> branches first ::branch/files)))

(re-frame/reg-sub
 ::branch
 (fn [[_ pattern]]
   (re-frame/subscribe
    [:q*
     [{:vims/branches (or pattern '[*])}]
     :app/vims]))
 (fn [{:vims/keys [branches]}]
   (->> branches first)))