(ns vimsical.frontend.code-editor.views
  (:require [reagent.core :as r]))

(defn editor-opts
  "https://microsoft.github.io/monaco-editor/api/interfaces/monaco.editor.ieditorconstructionoptions.html"
  [file-type]
  {:editor {:value                (name file-type)
            :language             (name file-type)
            :readOnly             false ; default

            :theme                "vs"  ; default
            :fontFamily           "FiraCode-Retina"
            :fontLigatures        true
            ;; two chars gap between line nums and code
            :lineDecorationsWidth "2ch"
            :lineNumbersMinChars  1
            ;; "none" | "gutter" | "line" | "all"
            :renderLineHighlight  "line"
            :renderIndentGuides   false ; default

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
            :wrappingIndent       "same"
            :useTabStops          false ; use spaces
            }
   ;; https://microsoft.github.io/monaco-editor/api/interfaces/monaco.editor.itextmodelupdateoptions.html
   :model  {:tabSize 2}})

(defn new-editor [c {:keys [editor model] :as opts}]
  (doto (js/monaco.editor.create (r/dom-node c) (clj->js editor))
    (.. getModel (updateOptions (clj->js model)))))

(defn dispose-editor [editor]
  (.dispose editor))

(defn code-editor [file-type]
  (let [editor-instance (r/atom nil)]
    (r/create-class
     {:component-did-mount
      (fn [c]
        (let [editor (new-editor c (editor-opts file-type))]
          (js/console.debug editor)
          (reset! editor-instance editor)))
      :component-will-unmount
      (fn [_]
        (dispose-editor @editor-instance))
      :reagent-render
      (fn [file-type]
        [:div {:style {:display    "flex"
                       :flex       "auto"
                       :background :tomato}}])})))


