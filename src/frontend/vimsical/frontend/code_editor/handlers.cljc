(ns vimsical.frontend.code-editor.handlers
  (:require [re-frame.core :as re-frame]
            [vimsical.frontend.util.dom :as util.dom]))

(re-frame/reg-event-db ::new-edit-event
  (fn [db [_ edit-event]]
    #?(:cljs (js/console.debug edit-event))
    db))

(re-frame/reg-event-db ::paste
  (fn [db [_ editor-instance string]]
    #?(:cljs
       (let [model (.-model editor-instance)
             sels  (.. editor-instance -cursor getSelections)]
         (.pushEditOperations
          model
          sels
          (clj->js
           [{; if true moves cursor, else makes selection around added text
             :forceMoveMarkers true
             :text             string
             :range            (first sels)}]))))
    db))