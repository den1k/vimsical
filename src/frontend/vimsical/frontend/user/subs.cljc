(ns vimsical.frontend.user.subs
  (:require
   [re-frame.core :as re-frame]
   [vimsical.frontend.app.subs :as app.subs]
   [vimsical.user :as user]))

(re-frame/reg-sub
 ::vimsae
 (fn [[_ ?pattern]]
   (re-frame/subscribe
    [::app.subs/user [{::user/vimsae (or ?pattern '[*])}]]))
 ::user/vimsae)
