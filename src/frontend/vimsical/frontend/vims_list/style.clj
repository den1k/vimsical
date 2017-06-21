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
     :z-index  1
     :width    :100%
     :height   :100%}]
   ;["&:hover:before"] ; for overlay
   ;; scale iframe contents for preview
   [:.iframe
    {:position         :absolute
     :width            :200%
     :height           :200%
     :transform        "scale(0.5)"
     :transform-origin "top left"}]])

(def vims-list-item
  [:.vims-list-item
   {:padding       "15px"
    :cursor        :pointer
    :width         :600px
    :border-radius :5px}
   [:&:first-child {:margin-top 0}]
   vims-preview
   [:&:hover
    {:background (:darkgrey-trans colors)}
    [:.live-preview
     {:transform "scale(1)"}]]
   [:.live-preview
    {:box-shadow "0 2px 5px hsla(0,0%,0%,0.3)"}]
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
   {:margin "20px 0"}
   [:.title
    {:margin-left :15px}]
   [:.list-box
    {:padding "15px 20px"
     :width   :90%}
    [:.chevron
     {:font-size :120px}]
    [:.list vims-list-item]]])
