(ns vimsical.frontend.code-editor.handlers
  (:require [re-frame.core :as re-frame]
            [vimsical.frontend.util.dom :as util.dom]))

(re-frame/reg-event-db ::new-edit-event
  (fn [db [_ edit-event]]
    #?(:cljs
       (util.dom/open-ext-popup "https://youtu.be/zvO2IvL0sBM?t=44s"))))


