(ns vimsical.frontend.code-editor.handlers
  #?@(:clj
      [(:require
        [re-frame.core :as re-frame]
        [vimsical.frontend.util.re-frame :refer [<sub]]
        [vimsical.frontend.code-editor.ui-db :as ui-db]
        [vimsical.common.util.core :as util]
        [vimsical.frontend.code-editor.subs :as subs]
        [vimsical.frontend.util.re-frame :as util.re-frame]
        [vimsical.frontend.timeline.subs :as timeline.subs]
        [vimsical.frontend.vcr.subs :as vcr.subs]
        [vimsical.frontend.vcs.subs :as vcs.subs]
        [vimsical.vcs.core :as vcs])]
      :cljs
      [(:require
        [re-frame.core :as re-frame]
        [vimsical.frontend.util.re-frame :refer [<sub]]
        [re-frame.loggers :refer [console]]
        [vimsical.frontend.code-editor.ui-db :as ui-db]
        [vimsical.frontend.code-editor.subs :as subs]
        [vimsical.frontend.util.re-frame :as util.re-frame]
        [vimsical.frontend.timeline.subs :as timeline.subs]
        [vimsical.frontend.vcr.subs :as vcr.subs]
        [vimsical.frontend.vcs.subs :as vcs.subs]
        [vimsical.vcs.core :as vcs])]))

;;
;; * Monaco helpers
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
   #?(:cljs
      #js {:startColumn     (:col start-pos)
           :startLineNumber (:line start-pos)
           :endColumn       (:col end-pos)
           :endLineNumber   (:line end-pos)})))

(defn idx->pos
  "Takes an idx and a string and computes a :line and :col hashmap."
  [idx string]
  (let [str-len (count string)]
    (cond
      (zero? idx)     {:line 1 :col 1}
      (> idx str-len) nil ; out of bounds
      :else           (reduce
                       (fn [[cur-idx l c :as step] char]
                         (let [next-idx (inc cur-idx)]
                           (cond
                             (= cur-idx idx)      (reduced {:line l :col c})
                             ;; lookahead at last step of reduction
                             ;; this becomes true only when idx is at last char
                             (= next-idx str-len) (reduced {:line l :col (inc c)})
                             :else                (if (identical? \newline char)
                                                    [next-idx (inc l) 1]
                                                    [next-idx l (inc c)]))))
                       [0 1 1]
                       string))))

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
        event-type      (content-event-type e-state)]
    (case event-type
      :str/ins  {::edit-event/op event-type ::edit-event/diff diff ::edit-event/idx idx}
      :str/rem  {::edit-event/op event-type ::edit-event/idx idx ::edit-event/amt deleted}
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

;; todo, move into monaco URLs?
(defn dispose-editor
  "Dispose Monaco editor"
  [editor]
  (.dispose editor))

(defn bind-listeners
  "Register `listeners` with their respective handlers on `editor` and return a
  map with the same keys as `listeners` but where the values are the disposables
  returned from binding the listeners."
  [editor
   {:as listeners
    :keys
    [model->content-change-handler
     model->cursor-change-handler
     editor->focus-handler
     editor->blur-handler]}]
  (when (and editor listeners)
    (let [model (.-model editor)]
      {:model->content-change-handler (.onDidChangeModelContent editor (model->content-change-handler model))
       :model->cursor-change-handler  (.onDidChangeCursorSelection editor (model->cursor-change-handler model))
       :editor->focus-handler         (.onDidFocusEditor editor (editor->focus-handler editor))
       :editor->blur-handler          (.onDidBlurEditor editor (editor->blur-handler editor))})))

;;
;; * Events
;;

;;
;; ** Instance lifecycle
;;

(re-frame/reg-event-fx
 ::register
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ file editor-instance listeners]]
   {:ui-db (-> ui-db
               (ui-db/set-editor file editor-instance)
               (ui-db/set-listeners file listeners))
    :dispatch-n
    [[::set-string nil file ""]
     [::bind-listeners file]
     [::track-start file]]}))

(re-frame/reg-event-fx
 ::dispose
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db] :as cofx} [_ file]]
   (do
     ;; XXX fx?
     (dispose-editor (ui-db/get-editor ui-db file))
     {:ui-db    (ui-db/set-editor ui-db file nil)
      :dispatch [::track-stop file]})))

;;
;; ** Listeners lifecycle
;;

(re-frame/reg-event-fx
 ::clear-disposables
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ file]]
   #?(:cljs
      (if-some [disposables (ui-db/get-disposables ui-db file)]
        (do
          ;; Clear disposables
          (reduce-kv
           (fn [_ k disposable]
             (.dispose disposable))
           nil disposables)
          {:ui-db (ui-db/set-disposables ui-db file nil)})
        (console :error "disposables not found")))))

(re-frame/reg-event-fx
 ::bind-listeners
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ file]]
   #?(:cljs
      (when-some [editor (ui-db/get-editor ui-db file)]
        (when-some [listeners (ui-db/get-listeners ui-db file)]
          ;; Create new disposables and update ui-db
          (let [listeners' (bind-listeners editor listeners)]
            {:ui-db (ui-db/set-disposables ui-db file listeners')}))))))

;;
;; ** User actions
;;

(re-frame/reg-event-fx
 ::update-editor-text
 [(re-frame/inject-cofx :ui-db)]
 update-editor-text)

(re-frame/reg-event-fx
 ::update-editor-cursor
 [(re-frame/inject-cofx :ui-db)]
 update-editor-cursor)

(defn set-model-markers [model sub-type markers]
  #?(:cljs
     (.. js/monaco -editor (setModelMarkers model (name sub-type) markers))))

(defn severity-code [flag]
  (case flag
    :error   3
    :warning 2
    :info    1
    :ignore  0))

(defn model-marker
  [{:keys [pos msg severity]
    :or   {severity :error}}]
  #?(:cljs
     (let [{:keys [line col]} pos]
       #js {:message         msg
            :severity        (severity-code severity)
            :startColumn     col
            :startLineNumber line
            :endColumn       col
            :endLineNumber   line})))

(re-frame/reg-event-fx
 ::text-change
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ reg-key {:keys [db/id] :as file} e]]
   (let [model      (ui-db->editor ui-db reg-key file :model)
         edit-event (parse-content-event model e)]
     {:dispatch-n [[::vcs.handlers/add-edit-event id edit-event]
                   [::check-code-errors reg-key file]]})))

(re-frame/reg-event-fx
 ::check-code-errors
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ reg-key {:keys [db/id] :as file}]]
   (when-let [errors (<sub [::vcs.subs/file-lint-or-preprocessing-errors file])]
     (let [model   (ui-db->editor ui-db reg-key file :model)
           markers (into-array (map model-marker errors))]
       ;; TODO fixme
       #?(:cljs
          (.setTimeout js/window
                       #(set-model-markers model
                                           :javascript
                                           markers)
                       1000))
       nil))))

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
 (fn [{:keys [ui-db] :as cofx} [_ file]]
   {:ui-db (ui-db/set-active-file ui-db file)}))

(re-frame/reg-event-fx
 ::blur
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db] :as cofx} [_ file]]
   {:ui-db (ui-db/set-active-file ui-db nil)}))

(re-frame/reg-event-fx
 ::paste
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ string]]
   #?(:cljs
      (let [editor (ui-db/get-active-editor ui-db)
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

;;
;; ** Updates
;;

;; NOTE this will cause the .onContentDidChange callback to fire unless we
;; dispatch ::clear-disposables before
(re-frame/reg-event-fx
 ::set-string
 [(re-frame/inject-cofx :ui-db)]
 (fn set-string
   [{:keys [ui-db]} [_ read-only? file string]]
   #?(:cljs
      (if-some [editor (ui-db/get-editor ui-db file)]
        (do (.setValue editor string) nil)
        (console :error "editor not found")))))

(re-frame/reg-event-fx
 ::set-cursor
 [(re-frame/inject-cofx :ui-db)]
 (fn set-cursor
   [{:keys [ui-db]} [_ file cursor string]]
   #?(:cljs
      (if-some [editor (ui-db/get-editor ui-db file)]
        ;; TODO convert index to line/col pos
        ;; (.setCursor editor cursor)
        nil
        (console :error "editor not found")))))

(re-frame/reg-event-fx
 ::update-editor
 (fn update-editor
   [_ [_ {file-id :db/id :as file} string cursor]]
   (when (and (some? string) (some? cursor))
     {:dispatch-n
      [[::clear-disposables file]
       [::set-string false file string]
       [::set-cursor file cursor string]
       [::bind-listeners file]]})))

(re-frame/reg-event-fx
 ::track-start
 (fn [_ [_ file]]
   {:track
    [{:id           [::editor file]
      :action       :register
      :subscription [::subs/string-and-cursor file]
      :val->event   (fn [{:keys [string cursor] :as val}]
                      [::update-editor file string cursor])}]}))

(re-frame/reg-event-fx
 ::track-stop
 (fn [_ [_ file]]
   {:track
    [{:id     [::editor file]
      :action :dispose}]}))
