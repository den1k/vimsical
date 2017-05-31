(ns vimsical.frontend.vims-list.style
  (:require [vimsical.frontend.styles.color :refer [colors]]))

(def vims-preview
  [:.live-preview
   {:width            :200px
    :height           :130px
    :flex             :none             ; overwrite live-preview defaults
    :transform-origin :left
    :transform        "scale(0.98)"
    :transition       "transform 0.5s ease"
    :position         :relative}
   ;; keep iframe from stealing mouse interactions
   ["&:before"
    {:content  "''"
     :position :absolute
     :cursor   :pointer
     :width    :100%
     :height   :100%}]
   ;["&:hover:before"] ; for overlay
   ])

(def vims-list-item
  [:.vims-list-item
   {:padding :10px
    :cursor  :pointer}
   ;[:&:first-child {:padding-top 0}]
   vims-preview
   [:&:hover
    {:background    (:lightgrey colors)
     :border-radius :5px}
    [:.live-preview
     {:transform "scale(1)"}]]
   [:.vims-title-and-delete
    {:flex-grow 1
     :padding   :25px}
    [:.vims-title
     {:font-size   :20px
      :font-weight :500}]
    [:.delete-button
     {:margin-left :30px
      :visibility  :hidden
      :color       :grey
      :position    :relative}
     [:&:hover
      {:color :black}]
     [:.delete-x
      {:transform-origin :center
       :transform        "rotate(45deg)"
       :font-size        :25px}]]
    [:.popover-content-wrapper
     {:margin "-9px 0 0 -1px"}]
    [:&:hover
     [:.delete-button
      {:visibility :visible}]]]
   [:.delete-warning
    {:flex-grow 1
     :padding   :15px}
    [:.warning]
    [:.actions
     {:width :200px}]]])

(def vims-list
  [:.vims-list
   {:margin-top :100px
    :height     :80vh
    :overflow   :hidden}
   [:.title
    {:margin-left :15px}]
   [:.list-box
    [:.list vims-list-item]]
   #_[:.no-vimsae
      {:display         :flex
       :justify-content :center}]])
