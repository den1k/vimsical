(ns vimsical.frontend.code-editor.style
  (:require [vimsical.frontend.styles.color :refer [colors]]))

(def code-editor
  [:.code-editor-wrapper
   {:width :100%}

   [:.insert-warning
    {:padding     :10px
     :flex-shrink 0
     :background  (:mediumgrey colors)}
    [:.msg]
    [:.action.button
     {:margin-top :5px}]]
   [:.code-editor
    {:width  :100%
     :height :100%}

    [:.monaco-editor
     [:.monaco-editor-background
      {:background :white}]
     [:.scrollbar.vertical
      {:margin-right :1px}
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
        :left       "initial !important"}]]]]])
