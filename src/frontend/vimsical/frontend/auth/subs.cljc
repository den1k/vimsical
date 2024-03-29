(ns vimsical.frontend.auth.subs
  (:require
   [vimsical.subgraph :as sg]
   [re-frame.core :as re-frame]
   [vimsical.user :as user]
   [vimsical.vims :as vims]))

(re-frame/reg-sub-raw
 ::user
 (fn [db [_ ?pattern]]
   (re-frame/subscribe [:q [:app/user (or ?pattern '[*])]])))
