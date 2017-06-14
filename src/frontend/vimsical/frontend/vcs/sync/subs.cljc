(ns vimsical.frontend.vcs.sync.subs
  (:require [re-frame.core :as re-frame]
            [vimsical.frontend.vcs.sync.handlers :as handlers]
            [re-frame.interop :as interop]
            [vimsical.frontend.util.re-frame :refer [<sub]]
            [vimsical.frontend.remotes.fx :as remotes.fx]))

(re-frame/reg-sub-raw
 ::vims-saved?
 (fn [db [_ vims-uid]]
   (interop/make-reaction
    #(and (not (::handlers/add-deltas-debouncing? @db))
          (= ::remotes.fx/success
             (<sub [::remotes.fx/status :backend [::handlers/sync vims-uid]]))))))
