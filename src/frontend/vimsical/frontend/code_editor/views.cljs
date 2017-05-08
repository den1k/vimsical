(ns vimsical.frontend.code-editor.views
  (:require
   [reagent.core :as r]
   [re-frame.core :as re-frame]
   [vimsical.frontend.util.re-frame :refer [<sub]]
   [vimsical.common.util.core :as util]
   [vimsical.frontend.code-editor.handlers :as handlers]
   [vimsical.frontend.vcs.subs :as vcs.subs]
   [vimsical.vcs.file :as file]))

(defn editor-opts
  "https://microsoft.github.io/monaco-editor/api/interfaces/monaco.editor.ieditorconstructionoptions.html"
  [{:keys [file compact? read-only? custom-opts]
    :or   {read-only? false}}]
  (let [sub-type
        (::file/sub-type file)
        defaults
        {:value                (<sub [::vcs.subs/file-string file])
         :language             (name sub-type)
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

(defn setup-event-handlers [editor reg-key file]
  (doto editor
    (.onDidChangeModelContent
     #(re-frame/dispatch [::handlers/text-change reg-key file %]))
    (.onDidChangeCursorSelection
     #(re-frame/dispatch [::handlers/selection-change reg-key file %]))
    (.onDidFocusEditor
     #(re-frame/dispatch [::handlers/focus :app/active-editor editor]))))

(defn code-editor
  [{:keys [file read-only? editor-reg-key]
    :as   opts}]
  {:pre [editor-reg-key]}
  (r/create-class
   {:component-did-mount
    (fn [c]
      (let [editor (new-editor (r/dom-node c) (editor-opts opts))
            model  (.-model editor)]
        (when-not read-only?
          (setup-event-handlers editor editor-reg-key file))
        (re-frame/dispatch [::handlers/register editor-reg-key file editor])))
    :component-will-unmount
    (fn [_]
      (re-frame/dispatch [::handlers/dispose editor-reg-key file]))
    :render
    (fn [_]
      (when-let [lint-errors (<sub [::vcs.subs/file-lint-errors file])]
        (js/console.warn ::LINT_ERRORS lint-errors))
      [:div.code-editor])}))
