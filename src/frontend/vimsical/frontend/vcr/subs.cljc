(ns vimsical.frontend.vcr.subs
  (:require
   [re-frame.core :as re-frame]
   [vimsical.vcs.branch :as branch]))

(re-frame/reg-sub
 ::branch
 (fn [[_ pattern]]
   (re-frame/subscribe
    [:q
     [:app/vims
      [{:vims/branches (or pattern '[*])}]]]))
 (fn [{:vims/keys [branches]}]
   (->> branches first)))

(re-frame/reg-sub
 ::playhead
 (fn [db [_]] 2000))

(re-frame/reg-sub
 ::skimhead
 (fn [db [_]] 1000))
