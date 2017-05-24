(ns vimsical.frontend.user.subs
  (:require [re-frame.core :as re-frame]
            [vimsical.user :as user]
            [vimsical.frontend.app.subs :as app.subs]
            [re-frame.interop :as interop]
            [vimsical.frontend.util.re-frame :refer [<sub]]))

(re-frame/reg-sub-raw
 ::vimsae
 (fn [db [_ ?pattern]]
   (interop/make-reaction
    #(-> (<sub [::app.subs/user [{::user/vimsae (or ?pattern '[*])}]])
         ::user/vimsae))))
