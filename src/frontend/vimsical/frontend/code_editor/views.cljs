(ns vimsical.frontend.code-editor.views
  (:require [reagent.core :as r]
            [vimsical.common.util.core :as util]))

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

(defn new-editor [c {:keys [editor model] :as opts}]
  (doto (js/monaco.editor.create (r/dom-node c) (clj->js editor))
    (.. getModel (updateOptions (clj->js model)))))

(defn dispose-editor [editor]
  (.dispose editor))

(defn code-editor [opts]
  {:pre [(:file-type opts)]}
  (let [editor-instance (r/atom nil)]
    (r/create-class
     {:component-did-mount
      (fn [c]
        (reset! editor-instance (new-editor c (editor-opts opts))))
      :component-will-unmount
      (fn [_]
        (dispose-editor @editor-instance))
      :render
      (fn [_]
        [:div.code-editor])})))


