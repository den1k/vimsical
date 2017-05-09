(ns vimsical.frontend.code-editor.views
  (:require
   [re-frame.core :as re-frame]
   [vimsical.frontend.util.re-frame :refer [<sub]]
   [reagent.core :as r]
   [vimsical.vcs.file :as file]
   [vimsical.common.util.core :as util]
   [vimsical.frontend.code-editor.handlers :as handlers]
   [vimsical.frontend.vcs.handlers :as vcs.handlers]
   [vimsical.frontend.vcs.subs :as vcs.subs]
   [vimsical.vcs.edit-event :as edit-event]))

(defn editor-opts
  "https://microsoft.github.io/monaco-editor/api/interfaces/monaco.editor.ieditorconstructionoptions.html"
  [{:keys [{::file/keys [sub-type]} file compact? read-only? custom-opts]
    :or   {read-only? false}}]
  {:pre [file-type]}
  (let [defaults
        {:value    ""
         :language (name sub-type)
         :readOnly read-only?

         :theme "vs" ; default
         ;; FIXME! Odd things happen to monaco's line-width calculations
         ;; when using FiraCode regardless of ligatures
         ;; :fontFamily           "FiraCode-Retina"
         ;; :fontLigatures        true
         ;; :fontSize             13

         ;; two chars gap between line nums and code
         :lineDecorationsWidth "2ch"
         :lineNumbersMinChars  1
         ;; "none" | "gutter" | "line" | "all"
         :renderLineHighlight  "line"
         :renderIndentGuides   false ; default

         ;; Enable that the editor will install an interval to check if its container
         ;; dom node size has changed. Enabling this might have a severe performance
         ;; impact. Defaults to false.
         :automaticLayout true
         :contextmenu     false
         ;; cmd + scroll changes font-size
         :mouseWheelZoom  true

         ;; Control the wrapping strategy of the editor. Using -1 means no
         ;; wrapping whatsoever. Using 0 means viewport width wrapping
         ;; (ajusts with the resizing of the editor). Using a positive number
         ;; means wrapping after a fixed number of characters. Defaults to 300.
         ;; https://microsoft.github.io/monaco-editor/api/modules/monaco.editor.html#definetheme
         :wrappingColumn 0
         ;; "none" | "same" | "indent"
         :wrappingIndent "indent"
         :useTabStops    false}

        scrollbar-defaults
        {:scrollbar
         {;; setting to be explicit, bar won't
          ;; show when wrappingColumn is 0
          :useShadows            false
          ;; horizontal should never be needed
          ;; because editors are rendered in
          ;; width adjustable splitters
          :horizontal            :hidden
          :verticalScrollbarSize 9}}

        compact-defaults
        {:suggestOnTriggerCharacters false
         :quickSuggestions           false
         ;; not tested what these do
         :codeLens                   false
         :wordBasedSuggestions       false
         :snippetSuggestions         :none
         :scrollbar                  {:verticalScrollbarSize 7}}]

    {:editor
     (util/deep-merge
      defaults
      scrollbar-defaults
      (when compact? compact-defaults)
      custom-opts)
     ;; https://microsoft.github.io/monaco-editor/api/interfaces/monaco.editor.itextmodelupdateoptions.html
     :model {:tabSize 2}}))

(defn new-editor [el {:keys [editor model]}]
  {:pre [editor model]}
  (doto (js/monaco.editor.create el (clj->js editor))
    (.. getModel (updateOptions (clj->js model)))))

(defn pos->str-idx
  ;; Counts only until where we are
  ;; inc's at each line to account for \n
  ;; dec column because monaco returns col after event
  [line column lines]
  (let [xf (comp
            (take (dec line))
            (map #(inc (.-length %))))]
    (transduce xf + (dec column) lines)))

;;
;; * Content change
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
  (or (and deleted (if added :str/rplc :str/rem))
      (and added :str/ins)))

(defn parse-content-event [model e]
  (let [{:keys [diff idx _ deleted]
         :as   e-state} (content-event-state model e)
        event-type      (content-event-type e-state)]
    (case event-type
      :str/ins  {::edit-event/op event-type ::edit-event/diff diff ::edit-event/idx idx}
      :str/rem  {::edit-event/op event-type ::edit-event/idx idx ::edit-event/amt deleted}
      :str/rplc {::edit-event/op event-type ::edit-event/diff diff ::edit-event/idx idx ::edit-event/amt deleted})))

;;
;; * Cursor & selection change
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
;; * Model listeners
;;

;; XXX use interceptors to parse events?
(defn handle-content-change [model file e]
  (re-frame/dispatch
   [::vcs.handlers/add-edit-event file  (parse-content-event model e)]))

(defn handle-cursor-change [model file e]
  (re-frame/dispatch
   [::vcs.handlers/add-edit-event file (parse-selection-event model e)]))

(defn editor-focus-handler [file editor]
  (fn [_]
    (re-frame/dispatch [::handlers/focus file editor])))

(defn editor-blur-handler [file editor]
  (fn [_]
    (re-frame/dispatch [::handlers/blur file editor])))

(defn new-listeners
  [file editor]
  {:model->content-change-handler
   (fn model->content-change-handler [model]
     (fn [e]
       (handle-content-change model file e)))
   :model->cursor-change-handler
   (fn model->cursor-change-handler [model]
     (fn [e]
       (handle-cursor-change model file e)))
   :editor->focus-handler (partial editor-focus-handler file)
   :editor->blur-handler  (partial editor-blur-handler file)})

;;
;; * Component
;;

(defn code-editor
  [{:keys [file] :as opts}]
  {:pre [file]}
  (r/create-class
   {:component-did-mount
    (fn [c]
      (let [editor    (new-editor (r/dom-node c) (editor-opts opts))
            listeners (new-listeners file editor)]
        (re-frame/dispatch [::handlers/register file editor listeners])))
    :component-will-unmount
    (fn [_]
      (re-frame/dispatch [::handlers/dispose file]))
    :render
    (fn [_]
      (when-let [errors
                 (<sub [::vcs.subs/file-lint-or-preprocessing-errors file])]
        (js/console.warn ::LINT_OR_PREPROCESSING_ERRORS errors))
      [:div.code-editor])}))
