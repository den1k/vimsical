(ns vimsical.frontend.code-editor.handlers
  (:require [re-frame.core :as re-frame]
            [vimsical.common.util.core :as util]
            [vimsical.frontend.util.dom :as util.dom]
            [vimsical.frontend.app.ui :as app.ui]))

;; todo, move into monaco utils?
(defn dispose-editor
  "Dispose Monaco editor"
  [editor]
  (.dispose editor))

(re-frame/reg-event-fx
 ::register
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [db ui-db]} [_ reg-key file-type editor-instance]]
   {:db    db
    :ui-db (assoc-in ui-db [reg-key file-type] editor-instance)}))

(re-frame/reg-event-fx
 ::dispose
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [db ui-db] :as cofx} [_ reg-key file-type]]
   (-> (get-in ui-db [reg-key file-type])
       (dispose-editor))
   {:db    db
    :ui-db (util/dissoc-in ui-db [reg-key file-type])}))

(re-frame/reg-event-fx
 ::focus
 (fn [{:keys [db ui-db]} [_ focus-key editor]]
   {:db    db
    :ui-db (assoc ui-db focus-key editor)}))

(re-frame/reg-event-db
 ::new-edit-event
 (fn [db [_ edit-event]]
   #?(:cljs (js/console.debug edit-event))
   db))

(re-frame/reg-event-fx
 ::paste
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db] :as cofx} [_ string]]
   #?(:cljs
      (let [editor (::app.ui/active-editor ui-db)
            model  (.-model editor)
            sels   (.. editor -cursor getSelections)]
        (.pushEditOperations
         model
         sels
         (clj->js
          [{; if true moves cursor, else makes selection around added text
            :forceMoveMarkers true
            :text             string
            :range            (first sels)}]))))
   (dissoc cofx :event)))