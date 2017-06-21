(ns vimsical.frontend.app.vims.subs
  (:require [re-frame.core :as re-frame]
            [vimsical.frontend.vcs.subs :as vcs.subs]
            [vimsical.frontend.app.subs :as app.subs]
            [re-frame.interop :as interop]
            [vimsical.frontend.util.re-frame :refer [<sub]]))

(re-frame/reg-sub-raw
 ::added-libs
 (fn [_ _]
   (interop/make-reaction
    #(let [vims (<sub [::app.subs/vims [:db/uid]])]
       (<sub [::vcs.subs/libs vims])))))