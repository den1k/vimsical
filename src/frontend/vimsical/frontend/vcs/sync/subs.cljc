(ns vimsical.frontend.vcs.sync.subs
  (:require [re-frame.core :as re-frame]
            [vimsical.frontend.vcs.sync.handlers :as handlers]
            [re-frame.interop :as interop]
            [vimsical.frontend.util.re-frame :refer [<sub]]
            [vimsical.frontend.remotes.fx :as remotes.fx]))

(re-frame/reg-sub
 ::add-deltas-debouncing?
 (fn [db _] (::handlers/add-deltas-debouncing? db)))

(re-frame/reg-sub
 ::vims-sync-status
 (fn [[_ vims-uid]]
   [(re-frame/subscribe [::add-deltas-debouncing?])
    (re-frame/subscribe [::remotes.fx/status :backend [::handlers/sync vims-uid]])])
 (fn [[debouncing? status]]
   (cond
     (nil? status) :init
     debouncing? :waiting
     (and (not debouncing?)
          (= ::remotes.fx/success status)) :success
     (map? status) :error)))
