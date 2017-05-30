(ns vimsical.frontend.code-editor.views
  (:require
   [re-frame.core :as re-frame]
   [reagent.core :as reagent]
   [vimsical.common.util.core :as util :include-macros true]
   [vimsical.frontend.code-editor.handlers :as handlers]
   [vimsical.frontend.util.re-frame :refer [<sub]]
   [vimsical.frontend.vcs.handlers :as vcs.handlers]
   [vimsical.frontend.vcs.subs :as vcs.subs]
   [vimsical.vcs.file :as file]))

(defn editor-opts
  "https://microsoft.github.io/monaco-editor/api/interfaces/monaco.editor.ieditorconstructionoptions.html"
  [{:keys [file compact? read-only? custom-opts]
    :or   {read-only? false}}]
  (letfn [(file-lang [{::file/keys [sub-type]}] (name sub-type))]
    (let [defaults
          {:value                     ""
           :language                  (file-lang file)
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
       :model {:tabSize 2}})))

(defn new-editor [el {:keys [editor model]}]
  {:pre [editor model]}
  (doto (js/monaco.editor.create el (clj->js editor))
    (.. getModel (updateOptions (clj->js model)))))

;;
;; * Model listeners
;;

;; XXX use interceptors to parse events?
(defn handle-content-change [vims model file e]
  (re-frame/dispatch [::handlers/content-change vims file e]))

(defn handle-cursor-change [vims model file e]
  (re-frame/dispatch [::handlers/cursor-change vims file e]))

(defn editor-focus-handler [vims file editor]
  (fn [_]
    (re-frame/dispatch [::handlers/focus vims file editor])))

(defn editor-blur-handler [vims file editor]
  (fn [_]
    (re-frame/dispatch [::handlers/blur vims file editor])))

(defn new-listeners
  [vims file editor]
  {:pre [vims file editor]}
  {:model->content-change-handler
                          (fn model->content-change-handler [model]
                            (fn [e]
                              (handle-content-change vims model file e)))
   :model->cursor-change-handler
                          (fn model->cursor-change-handler [model]
                            (fn [e]
                              (handle-cursor-change vims model file e)))
   :editor->focus-handler (partial editor-focus-handler vims file)
   :editor->blur-handler  (partial editor-blur-handler vims file)})

(defn register [c {:keys [vims file] :as opts}]
  (let [editor    (new-editor (reagent/dom-node c) (editor-opts opts))
        listeners (new-listeners vims file editor)]
    (re-frame/dispatch [::handlers/register vims file editor listeners])))

(defn dispose [{:keys [file vims] :as opts}]
  (re-frame/dispatch [::handlers/dispose vims file]))

(defn recycle [c old-opts new-opts]
  (dispose old-opts)
  (register c new-opts))

;;
;; * Component
;;

(defn code-editor
  [{:keys [file] :as opts}]
  {:pre [file]}
  (reagent/create-class
   {:component-did-mount
    (fn [c] (register c opts))
    :component-will-unmount
    (fn [c] (dispose (reagent/props c)))
    :component-will-receive-props
    (fn [c [_ new-opts]] (recycle c (reagent/props c) new-opts))
    :render
    (fn [_] [:div.code-editor])}))
