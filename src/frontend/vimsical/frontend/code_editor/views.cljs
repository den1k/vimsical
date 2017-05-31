(ns vimsical.frontend.code-editor.views
  (:require
   [re-frame.core :as re-frame]
   [reagent.core :as reagent]
   [vimsical.common.util.core :as util :include-macros true]
   [vimsical.frontend.code-editor.handlers :as handlers]
   [vimsical.frontend.util.re-frame :refer [<sub]]
   [vimsical.frontend.vcs.handlers :as vcs.handlers]
   [vimsical.frontend.vcs.subs :as vcs.subs]
   [vimsical.vcs.file :as file]
   [vimsical.frontend.config :as config]
   [vimsical.frontend.code-editor.interop :as interop]))

(defn editor-opts
  "https://microsoft.github.io/monaco-editor/api/interfaces/monaco.editor.ieditorconstructionoptions.html"
  [{:keys [file compact? read-only? custom-opts]
    :or   {read-only? false}}]
  (let [defaults
        {:value                     ""
         :language                  (interop/file-lang file)
         :readOnly                  read-only?
         :theme                     "vs" ; default
         :quickSuggestions          false
         ;; FIXME! Odd things happen to monaco's line-width calculations
         ;; when using FiraCode regardless of ligatures
         ;; :fontFamily           "FiraCode-Retina"
         ;; :fontLigatures        true
         ;; :fontSize             13
         ;; columns within the .decorationsOverviewRuler (under scrollbar) in
         ;; which decorations like errors are rendered.
         :overviewRulerLanes        1
         ;; removes the ugly black bar signifying the position of the cursor
         :hideCursorInOverviewRuler true
         ;; two chars gap between line nums and code
         :lineDecorationsWidth      "2ch"
         :lineNumbersMinChars       1
         ;; "none" | "gutter" | "line" | "all"
         :renderLineHighlight       "line"
         :renderIndentGuides        false ; default
         ;; Enable that the editor will install an interval to check if its container
         ;; dom node size has changed. Enabling this might have a severe performance
         ;; impact. Defaults to false.
         :automaticLayout           true
         :contextmenu               false
         ;; cmd + scroll changes font-size
         :mouseWheelZoom            true
         ;; Control the wrapping strategy of the editor. Using -1 means no
         ;; wrapping whatsoever. Using 0 means viewport width wrapping
         ;; (ajusts with the resizing of the editor). Using a positive number
         ;; means wrapping after a fixed number of characters. Defaults to 300.
         ;; https://microsoft.github.io/monaco-editor/api/modules/monaco.editor.html#definetheme
         :wrappingColumn            0
         ;; "none" | "same" | "indent"
         :wrappingIndent            "indent"
         :useTabStops               false}

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

;;
;; * Component
;;

(defn code-editor
  [{:keys [file] :as opts}]
  {:pre [file]}
  (reagent/create-class
   {:component-did-mount
    (fn [c]
      (let [node   (reagent/dom-node c)
            editor (new-editor node (editor-opts opts))]
        (re-frame/dispatch [::handlers/register opts editor])
        (re-frame/dispatch [::handlers/init opts])))
    :component-will-unmount
    (fn [c] (re-frame/dispatch [::handlers/dispose (reagent/props c)]))
    :component-will-receive-props
    (fn [c [_ new-opts]]
      (let [old-opts (reagent/props c)]
        (re-frame/dispatch [::handlers/recycle old-opts new-opts])))
    :render
    (fn [_] [:div.code-editor])}))
