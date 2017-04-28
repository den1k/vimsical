(ns vimsical.frontend.code-editor.handlers
  (:require
   [re-frame.core :as re-frame]
   [vimsical.common.util.core :as util]
   [vimsical.frontend.vcs.subs :as vcs.subs]))

;; todo, move into monaco utils?
(defn dispose-editor
  "Dispose Monaco editor"
  [editor]
  (.dispose editor))

(defn update-editor [{:keys [db ui-db]} [_ reg-key {:keys [db/id] :as file}]]
  (let [editor (get-in ui-db [reg-key id])
        string (-> db
                   (vcs.subs/vims-vcs)
                   (vcs.subs/file-string file))]
    (.setValue editor string)
    nil))

(re-frame/reg-event-fx
 ::register
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [db ui-db]} [_ reg-key {:keys [db/id] :as file} editor-instance]]
   {:ui-db (assoc-in ui-db [reg-key id] editor-instance)}))

(re-frame/reg-event-fx
 ::dispose
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [db ui-db] :as cofx} [_ reg-key {:keys [db/id] :as file}]]
   (-> (get-in ui-db [reg-key id])
       (dispose-editor))
   {:ui-db (util/dissoc-in ui-db [reg-key id])}))

(re-frame/reg-event-fx
 ::init
 [(re-frame/inject-cofx :ui-db)]
 update-editor)

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
            :range            (first sels)}]))))
   nil))
