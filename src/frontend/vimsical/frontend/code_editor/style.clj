(ns vimsical.frontend.code-editor.style)

(def code-editor
  [:.code-editor
   {:width :100%}
   [:.monaco-editor
    [:.monaco-editor-background
     {:background :white}]
    ;; todo scrollbar def
    [:.decorationsOverviewRuler
     ;; hides the border around scrollbar
     ;; unfortunately also the relative current-line marker
     ;; https://github.com/Microsoft/monaco-editor/issues/422
     {:display :none}]
    [:.scrollbar.vertical
     [:.slider
      {:border-radius :5px}]]

    [:.view-overlays
     [:.current-line
      {:border :none}]
     [:&.focused
      [:.current-line
       {:background :#f8f8f8}]]]
    [".margin-view-overlays > div"
     {:display         :flex
      :justify-content :center}
     [:.line-numbers
      {:text-align :right
       :left       "initial !important"}]]]])
