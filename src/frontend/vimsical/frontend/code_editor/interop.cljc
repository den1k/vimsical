(ns vimsical.frontend.code-editor.interop
  (:require
   [vimsical.vcs.edit-event :as edit-event]
   [vimsical.vcs.file :as file]))

(defn file-lang [{::file/keys [sub-type]}]
  (name sub-type))

(defn set-model-language [file editor-instance]
  #?(:cljs
     (let [model (.-model editor-instance)]
       (.. js/monaco -editor (setModelLanguage model (file-lang file))))))

;;
;; * Position <> Index
;;

#?(:cljs
   (defn reveal-range [^js/monaco.editor editor js-pos]
     (.revealRange editor js-pos)))

#?(:cljs
   (defn set-selection [^js/monaco.editor editor js-pos]
     (.setSelection editor js-pos)))

#?(:cljs
   (defn focus [^js/monaco.editor editor]
     (.focus editor)))

(defn pos->str-idx
  "Takes a line, col, and lines, array of strings. Counts the characters of
  each line, including \newline, returning an absolute positional index from
  the beginning."
  [line col lines]
  (let [xf (comp (take (dec line))
                 (map #(inc (.-length %))))]
    ;; dec the col because monaco counts from 1
    (transduce xf + (dec col) lines)))

(defn pos->js-pos
  "Creates a monaco range object given either one position for a cursor or two
  for a selection."
  ([pos] (pos->js-pos pos pos))
  ([start-pos end-pos]
   {:pre [start-pos end-pos]}
   #?(:cljs #js {:startColumn     (:col start-pos)
                 :startLineNumber (:line start-pos)
                 :endColumn       (:col end-pos)
                 :endLineNumber   (:line end-pos)})))

(defn idx->pos
  "Takes an idx and a string and computes a :line and :col hashmap."
  [idx string]
  (let [str-len (count string)]
    (cond
      (zero? idx) {:line 1 :col 1}
      (> idx str-len) nil               ; out of bounds
      :else
      (reduce
       (fn [[cur-idx line col :as step] char]
         (let [next-idx (inc cur-idx)]
           (cond
             ;; lookahead in case we're at the last char
             (or (= cur-idx idx) (= next-idx str-len))
             (reduced {:line line :col (inc col)})

             (identical? \newline char)
             [next-idx (inc line) 1]

             :else
             [next-idx line (inc col)])))
       [0 1 1] string))))

;;
;; * Change Parsing
;;

#?(:cljs
   (defn- content-event-state [^js/Object model ^js/Object e]
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
        :deleted (when-not (zero? deleted-count) deleted-count)})))

(defn content-event-type [{:keys [added deleted]}]
  (or
   (and deleted (if added :str/rplc :str/rem))
   (and added :str/ins)))

#?(:cljs
   (defn parse-content-event [model e]
     (let [{:keys [diff idx added deleted]
            :as   e-state} (content-event-state model e)
           event-type (content-event-type e-state)]
       (case event-type
         :str/ins {::edit-event/op event-type ::edit-event/diff diff ::edit-event/idx idx}
         :str/rem {::edit-event/op event-type ::edit-event/idx idx ::edit-event/amt deleted}
         :str/rplc {::edit-event/op event-type ::edit-event/diff diff ::edit-event/idx idx ::edit-event/amt deleted}))))

;;
;; * Cursor & Selection change
;;

#?(:cljs
   (defn- selection-event-state [^js/Object model ^js/Object e]
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
        :lines      lines})))

#?(:cljs
   (defn parse-selection-event [model e]
     (let [{:keys [selection? start-idx end-idx]} (selection-event-state model e)]
       (if-not selection?
         {::edit-event/op :crsr/mv ::edit-event/idx start-idx}
         {::edit-event/op :crsr/sel ::edit-event/range [start-idx end-idx]}))))

#?(:cljs
   (defn dispose-editor
     "Dispose Monaco editor"
     [^js/monaco.editor editor]
     (.dispose editor)))

#?(:cljs
   (defn clear-disposables [disposables]
     (reduce-kv
      (fn [_ k ^js/Object disposable]
        (.dispose disposable))
      nil disposables)))

#?(:cljs
   (defn set-value [^js/monaco.editor editor string]
     (.setValue editor string)))

#?(:cljs
   (defn bind-listeners
     "Register `listeners` with their respective handlers on `editor` and return a
     map with the same keys as `listeners` but where the values are the disposables
     returned from binding the listeners."
     [^js/monaco.editor editor
      {:as listeners
       :keys
           [model->content-change-handler
            model->cursor-change-handler
            editor->focus-handler
            editor->blur-handler
            editor->mouse-down-handler]}]
     (when (and editor listeners)
       (let [model (.-model editor)]
         {:model->content-change-handler (.onDidChangeModelContent editor (model->content-change-handler model))
          :model->cursor-change-handler  (.onDidChangeCursorSelection editor (model->cursor-change-handler model))
          :editor->focus-handler         (.onDidFocusEditor editor (editor->focus-handler editor))
          :editor->blur-handler          (.onDidBlurEditor editor (editor->blur-handler editor))
          :editor->mouse-down-handler    (.onMouseDown editor editor->mouse-down-handler)}))))
