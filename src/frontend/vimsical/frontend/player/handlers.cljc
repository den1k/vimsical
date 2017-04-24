(ns vimsical.frontend.player.handlers
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-event-db
 ::register-editor
 (fn [db [_ id editor-instance]]
   (assoc-in db [:app/player :player/editors id] editor-instance)))