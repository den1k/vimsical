(ns vimsical.frontend.code-editor.handlers
  (:require [re-frame.core :as re-frame]
            [vimsical.vcs.core :as vcs]
            [vimsical.common.util.core :as util]
            [vimsical.frontend.util.dom :as util.dom]))

;; todo, move into monaco utils?
(defn dispose-editor
  "Dispose Monaco editor"
  [editor]
  (.dispose editor))

(re-frame/reg-event-fx
 ::register
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [db ui-db]} [_ reg-key id editor-instance]]
   {:ui-db (assoc-in ui-db [reg-key id] editor-instance)}))

(re-frame/reg-event-fx
 ::dispose
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [db ui-db] :as cofx} [_ reg-key id]]
   (-> (get-in ui-db [reg-key id])
       (dispose-editor))
   {:ui-db (util/dissoc-in ui-db [reg-key id])}))

(re-frame/reg-event-fx
 ::focus
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [db ui-db] :as cofx} [_ focus-key editor]]
   {:ui-db (assoc ui-db focus-key editor)}))

(re-frame/reg-event-fx
 ::paste
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ string]]
   #?(:cljs
      (let [editor (:app/active-editor ui-db)
            model  (.-model editor)
            sels   (.. editor -cursor getSelections)]
        (.pushEditOperations
         model
         sels
         (clj->js
          [{; if true moves cursor, else makes selection around added text
            :forceMoveMarkers true
            :text             string
            :range            (first sels)}]))))))
