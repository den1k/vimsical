(ns vimsical.frontend.code-editor.handlers
  (:require
   [re-frame.core :as re-frame]
   [vimsical.common.util.core :as util]
   [vimsical.frontend.vcs.subs :as vcs.subs]
   [vimsical.vcs.edit-event :as edit-event]
   [vimsical.frontend.vcs.handlers :as vcs.handlers]
   [vimsical.vcs.file :as file]
   [clojure.string :as str]))

;;
;; * Data Utils
;;

(defn ui-db->editor
  ([ui-db reg-key file]
   (ui-db->editor ui-db reg-key file :editor))
  ([ui-db reg-key {:keys [db/id] :as file} k]
   (let [editor (get-in ui-db [reg-key id])]
     (case k
       :editor editor
       :model (.-model editor)))))

;;
;; * Monaco Utils
;;

(defn pos->str-idx
  "Takes a line, column, and lines, array of strings. Counts the characters of
  each line, including \newline, returning an absolute positional index from
  the beginning."
  [line column lines]
  (transduce (comp (take (dec line))
                   (map #(inc (.-length %))))
             +
             (dec column)               ; dec bc we count from zero, monaco from one
             lines))

(defn idx->monaco-range
  "Creates a monaco range object given either one position for a cursor or two
  for a selection."
  ([pos]
   (idx->monaco-range pos pos))
  ([start-pos end-pos]
   {:pre [start-pos end-pos]}
   #js {:startColumn     (:col start-pos)
        :startLineNumber (:line start-pos)
        :endColumn       (:col end-pos)
        :endLineNumber   (:line end-pos)}))

(defn idx->pos
  "Takes an idx and a string and computes a :line and :col hashmap."
  [idx string]
  (let [str-len (count string)]
    (cond
      (zero? idx) {:line 1 :col 1}
      (> idx str-len) nil               ; out of bounds
      :else (reduce
             (fn [[cur-idx l c :as step] char]
               (let [next-idx (inc cur-idx)]
                 (js/console.debug next-idx str-len)
                 (cond
                   (= cur-idx idx) (reduced {:line l :col c})
                   ;; lookahead at last step of reduction
                   ;; this becomes true only when idx is at last char
                   (= next-idx str-len) (reduced {:line l :col (inc c)})
                   :else (if (identical? \newline char)
                           [next-idx (inc l) 1]
                           [next-idx l (inc c)]))))
             [0 1 1]
             string))))

(defn dispose-editor
  "Dispose Monaco editor"
  [editor]
  (.dispose editor))

(defn update-editor-text
  [{:keys [db ui-db]} [_ reg-key {:keys [db/id] :as file}]]
  (let [editor (ui-db->editor ui-db reg-key file)
        string (-> db
                   (vcs.subs/vims-vcs)
                   (vcs.subs/file-string file))]
    (.setValue editor string))
  nil)

(defn update-editor-cursor
  [{:keys [db ui-db]} [_ reg-key {:keys [db/id] :as file}]]
  (let [editor (ui-db->editor ui-db reg-key file)
        ;; FIXME get cursor / sel idx from vcs
        idx    2
        string "12\n456\n89"
        range  (idx->pos idx string)
        rr     (idx->monaco-range range)]
    (.revealRange editor rr)
    (.setSelection editor rr)
    (.focus editor))
  nil)



;;
;; * Change Parsing
;;

(defn- content-event-state [model e]
  (let [range         (.-range e)
        diff          (.-text e)
        added-count   (.-length diff)
        start-line    (.-startLineNumber range)
        deleted-count (.-rangeLength e)
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
;; * Handlers
;;

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
 ::update-editor-text
 [(re-frame/inject-cofx :ui-db)]
 update-editor-text)

(re-frame/reg-event-fx
 ::update-editor-cursor
 [(re-frame/inject-cofx :ui-db)]
 update-editor-cursor)

(re-frame/reg-event-fx
 ::text-change
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ reg-key {:keys [db/id] :as file} e]]
   (let [model      (ui-db->editor ui-db reg-key file :model)
         edit-event (parse-content-event model e)]
     {:dispatch [::vcs.handlers/add-edit-event id edit-event]})))

(re-frame/reg-event-fx
 ::selection-change
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ reg-key {:keys [db/id] :as file} e]]
   (let [model      (ui-db->editor ui-db reg-key file :model)
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