(ns vimsical.frontend.code-editor.views
  (:require [reagent.core :as r]
            [vimsical.common.util.core :as util]
            [re-frame.core :as re-frame]
            [vimsical.frontend.code-editor.handlers :as handlers]
            [vimsical.frontend.vcr.handlers :as vcr]
            [vimsical.frontend.vcr.subs :as vcr.subs]
            [vimsical.frontend.util.re-frame :refer-macros [with-subs]]))

(defn editor-opts
  "https://microsoft.github.io/monaco-editor/api/interfaces/monaco.editor.ieditorconstructionoptions.html"
  [{:keys [file-type compact? read-only? custom-opts]
    :or   {read-only? false}}]
  (let [defaults
        {:value                (name file-type)
         :language             (name file-type)
         :readOnly             read-only?

         :theme                "vs"     ; default
         ;; FIXME! Odd things happen to monaco's line-width calculations
         ;; when using FiraCode regardless of ligatures
         ;:fontFamily           "FiraCode-Retina"
         ;:fontLigatures        true
         ;:fontSize             13

         ;; two chars gap between line nums and code
         :lineDecorationsWidth "2ch"
         :lineNumbersMinChars  1
         ;; "none" | "gutter" | "line" | "all"
         :renderLineHighlight  "line"
         :renderIndentGuides   false    ; default

         ;; Enable that the editor will install an interval to check if its container
         ;; dom node size has changed. Enabling this might have a severe performance
         ;; impact. Defaults to false.
         :automaticLayout      true
         :contextmenu          false
         ;; cmd + scroll changes font-size
         :mouseWheelZoom       true

         ;; Control the wrapping strategy of the editor. Using -1 means no
         ;; wrapping whatsoever. Using 0 means viewport width wrapping
         ;; (ajusts with the resizing of the editor). Using a positive number
         ;; means wrapping after a fixed number of characters. Defaults to 300.
         ;; https://microsoft.github.io/monaco-editor/api/modules/monaco.editor.html#definetheme
         :wrappingColumn       0
         ;; "none" | "same" | "indent"
         :wrappingIndent       "indent"
         :useTabStops          false    ; use spaces
         }

        scrollbar-defaults
        {:scrollbar
         {; setting to be explicit, bar won't
          ; show when wrappingColumn is 0
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

    {:editor (util/deep-merge defaults
                              scrollbar-defaults
                              (when compact? compact-defaults)
                              custom-opts)
     ;; https://microsoft.github.io/monaco-editor/api/interfaces/monaco.editor.itextmodelupdateoptions.html
     :model  {:tabSize 2}}))

(defn new-editor [el {:keys [editor model] :as opts}]
  (doto (js/monaco.editor.create el (clj->js editor))
    (.. getModel (updateOptions (clj->js model)))))

(defn dispose-editor [editor]
  (.dispose editor))

(defn pos->str-idx
  ; Counts only until where we are
  ; inc's at each line to account for \n
  ; dec column because monaco returns col after event
  [line column lines]
  (transduce (comp (take (dec line))
                   (map #(inc (.-length %)))) + (dec column) lines))

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
  (or
   (and deleted (if added :str/rplc :str/rem))
   (and added :str/ins)))

(defn parse-content-event [model e]
  (let [{:keys [diff idx added deleted]
         :as   e-state} (content-event-state model e)
        event-type (content-event-type e-state)]
    (case event-type
      :str/ins {:op event-type :diff diff :idx idx}
      :str/rem {:op event-type :idx idx :amt deleted}
      :str/rplc {:op event-type :diff diff :idx idx :amt deleted})))

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
      {:op :crsr/mv :idx start-idx}
      {:op :crsr/sel :range [start-idx end-idx]})))

(defn handle-content-change [model e]
  (re-frame/dispatch [::handlers/new-edit-event (parse-content-event model e)]))

(defn handle-cursor-change [model e]
  (re-frame/dispatch [::handlers/new-edit-event (parse-selection-event model e)]))

(defn handle-focus [file-type]
  (re-frame/dispatch [::vcr/active-editor file-type]))

;; wip todo remove
(comment
 (sut/diffs->edit-events "" "foor" "four")
 [#:vimsical.vcs.edit-event{:op :str/ins, :idx 0, :diff "foor"}
  #:vimsical.vcs.edit-event{:op :crsr/mv, :idx 4}
  #:vimsical.vcs.edit-event{:op :crsr/mv, :idx 3}
  #:vimsical.vcs.edit-event{:op :crsr/mv, :idx 2}
  #:vimsical.vcs.edit-event{:op :str/rem, :idx 2, :amt 1}
  #:vimsical.vcs.edit-event{:op :crsr/mv, :idx 1}
  #:vimsical.vcs.edit-event{:op :crsr/mv, :idx 2}
  #:vimsical.vcs.edit-event{:op :str/ins, :idx 2, :diff "u"}
  #:vimsical.vcs.edit-event{:op :crsr/mv, :idx 3}])

(defn code-editor
  [{:keys [id file-type read-only? editor-sub-key editor-reg-key]
    :as   opts}]
  {:pre [id file-type]}
  (let [editor (re-frame/subscribe [editor-sub-key id])]
    (r/create-class
     {:component-did-mount
      (fn [c]
        (let [editor (new-editor (r/dom-node c) (editor-opts opts))
              model  (.-model editor)]
          (when-not read-only?
            (doto editor
              (.onDidChangeModelContent #(handle-content-change model %))
              (.onDidChangeCursorSelection #(handle-cursor-change model %))
              (.onDidFocusEditor #(handle-focus file-type))))
          (re-frame/dispatch [editor-reg-key id editor])))
      :component-will-unmount
      (fn [_]
        (dispose-editor @editor))
      :render
      (fn [_]
        [:div.code-editor])})))


