(ns vimsical.frontend.code-editor.handlers
  (:require
   [re-frame.core :as re-frame]
   [vimsical.common.util.core :as util]
   [vimsical.frontend.vcs.subs :as vcs.subs]
   [vimsical.vcs.edit-event :as edit-event]
   [vimsical.frontend.vcs.handlers :as vcs.handlers]))

;;
;; * Monaco Utils
;;

(defn dispose-editor
  "Dispose Monaco editor"
  [editor]
  (.dispose editor))

(defn update-editor-text [{:keys [db ui-db]} [_ reg-key {:keys [db/id] :as file}]]
  (let [editor (get-in ui-db [reg-key id])
        string (-> db
                   (vcs.subs/vims-vcs)
                   (vcs.subs/file-string file))]
    (.setValue editor string)
    nil))

(defn pos->str-idx
  ; Counts only until where we are
  ; inc's at each line to account for \n
  ; dec column because monaco returns col after event
  [line column lines]
  (transduce (comp (take (dec line))
                   (map #(inc (.-length %)))) + (dec column) lines))

;;
;; * Change Parsing
;;

(defn- content-event-state [model e]
  (let [range         (.-range e)
        diff          (.-text e)
        added-count   (.-length diff)
        deleted-count (.-rangeLength e)
        start-line    (.-startLineNumber range)
        start-column  (.-startColumn range)
        lines         (.getLinesContent model)]
    {:idx     (pos->str-idx start-line start-column lines)
     :diff    diff
     :added   (when-not (zero? added-count) added-count)
     :deleted (when-not (zero? deleted-count) deleted-count)}))

(defn content-event-type [{:keys [added deleted]}]
  (or
   (and deleted (if added :str/rplc :str/rem))
   (and added :str/ins)))

(defn parse-content-event [model e]
  (let [{:keys [diff idx added deleted]
         :as   e-state} (content-event-state model e)
        event-type (content-event-type e-state)]
    (case event-type
      :str/ins {::edit-event/op event-type ::edit-event/diff diff ::edit-event/idx idx}
      :str/rem {::edit-event/op event-type ::edit-event/idx idx ::edit-event/amt deleted}
      :str/rplc {::edit-event/op event-type ::edit-event/diff diff ::edit-event/idx idx ::edit-event/amt deleted})))

;;
;; * Cursor & Selection change
;;

(defn- selection-event-state [model e]
  (let [sel          (.-selection e)
        start-line   (.-startLineNumber sel)
        start-column (.-startColumn sel)
        end-line     (.-endLineNumber sel)
        end-column   (.-endColumn sel)
        lines        (.getLinesContent model)
        selection?   (or (not (identical? start-column end-column))
                         (not (identical? start-line end-line)))]
    {:selection? selection?
     :start-idx  (pos->str-idx start-line start-column lines)
     :end-idx    (when selection? (pos->str-idx end-line end-column lines))
     :lines      lines}))

(defn parse-selection-event [model e]
  (let [{:keys [selection? start-idx end-idx]} (selection-event-state model e)]
    (if-not selection?
      {::edit-event/op :crsr/mv ::edit-event/idx start-idx}
      {::edit-event/op :crsr/sel ::edit-event/range [start-idx end-idx]})))

;;
;; * Data Utils
;;

(defn ui-db->editor-info [ui-db reg-key {:keys [db/id] :as file}]
  (let [editor (get-in ui-db [reg-key id])
        model  (.-model editor)]
    {::editor editor
     ::model  model}))

;;
;; * Handlers
;;

(re-frame/reg-event-fx
 ::register
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [db ui-db]} [_ reg-key {:keys [db/id] :as file} editor-instance]]
   {:ui-db    (assoc-in ui-db [reg-key id] editor-instance)
    :dispatch [::init reg-key file]}))

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
 update-editor-text)

(re-frame/reg-event-fx
 ::text-change
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ reg-key {:keys [db/id] :as file} e]]
   (let [model      (::model (ui-db->editor-info ui-db reg-key file))
         edit-event (parse-content-event model e)]
     {:dispatch [::vcs.handlers/add-edit-event id edit-event]})))

(re-frame/reg-event-fx
 ::selection-change
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ reg-key {:keys [db/id] :as file} e]]
   (let [model      (::model (ui-db->editor-info ui-db reg-key file))
         edit-event (parse-selection-event model e)]
     {:dispatch [::vcs.handlers/add-edit-event id edit-event]})))

(re-frame/reg-event-fx
 ::focus
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [db ui-db] :as cofx} [_ focus-key editor]]
   {:ui-db (assoc ui-db focus-key editor)}))

(re-frame/reg-event-fx
 ::paste
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ string]]
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
         :range            (first sels)}])))
   nil))