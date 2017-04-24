(ns vimsical.frontend.player.subs
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
  ::editor
  (fn [db [_ id]]
    (-> db :app/player :player/editors (get id))))