(ns vimsical.frontend.code-editor.style)

(def code-editor
  [:.code-editor
   {:width :100%}
   [:.monaco-editor
    [:.monaco-editor-background
     {:background :white}]
    [:.scrollbar.vertical
     [:.slider
      {:border-radius :5px}]]
    [:.monaco-editor-hover-content
     [".hover-row:nth-child(n+2)"
      {:display :none}]]
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
