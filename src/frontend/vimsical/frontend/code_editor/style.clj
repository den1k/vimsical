(ns vimsical.frontend.code-editor.style)

(def code-editor
  [:.code-editor
   {:width :100%}
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
      :left       "initial !important"}]]])
