(ns vimsical.frontend.vims-list.style
  (:require [vimsical.frontend.styles.color :refer [colors]]))

(def vims-preview
  [:.vims-preview
   {:position :relative
    :display  :inline-block}
   [:.live-preview-wrap
    {:transition       "transform 0.5s ease"
     :transform-origin :left}
    ;; keep iframe from stealing mouse interactions
    ["&:before"
     {:content  "''"
      :position :absolute
      :cursor   :pointer
      :width    :100%
      :height   :100%}]
    ;["&:hover:before"] ; for overlay
    [:.iframe
     {:border     "none"
      :margin     0
      :padding    0
      :display    :block
      :background "white"
      :width      :230px
      :height     :150px}]]])

(def vims-list-item
  [:.vims-list-item
   {:padding          :20px
    :cursor           :pointer
    :transform-origin :left
    ;; FIXME transform breaks tooltip positioning
    ;; temp fix below: adding scale to title only
    ;:transform        "scale(0.95)"
    ;:transition       "transform 0.5s ease"
    }
   #_[:&:hover
      {:transform "scale(1)"}
      [:.live-preview-wrap
       {:box-shadow "0 3px 10px 1 lightgrey"}]]
   vims-preview
   [:&:hover
    [:.vims-title-and-delete
     [:.vims-title
      {:transform "scale(1)"}]]]
   [:.vims-title-and-delete
    {:flex-grow 1
     :padding   :25px}
    [:.vims-title
     {:font-size   :20px
      :font-weight :500}
     {:transform  "scale(0.95)"
      :transform-origin :left
      :transition "transform 0.5s ease"}]
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
   [:&:first-child {:margin-top 0}]
   [:.delete-warning
    {:flex-grow 1
     :padding   :15px}
    [:.warning]
    [:.actions
     {:width :200px}]]])

(def vims-list
  [:.vims-list
   {:background (:beatwhite colors)
    :max-height :600px
    :overflow-y :auto}
   [:.title
    {:margin-left :15px}]
   [:.list-box
    [:.list vims-list-item]]
   #_[:.no-vimsae
      {:display         :flex
       :justify-content :center}]])
