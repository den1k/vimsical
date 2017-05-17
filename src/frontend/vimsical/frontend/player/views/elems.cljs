(ns vimsical.frontend.player.views.elems
  (:require [vimsical.frontend.views.shapes :as shapes]
            [re-com.core :as re-com]))

(defn play-symbol [opts]
  [shapes/triangle
   (merge {:class           "play-symbol"
           :origin          [50 50]
           :height          50
           :stroke-linejoin "round"
           :stroke-width    8
           :rotate          90}
          opts)])

;; share with vcr
(defn pause-symbol
  "Pause Symbol to be used within SVG. Uses rect instead of line to for border-
  radius control."
  [{:keys [origin bar-width gap-width height border-radius class]
    :or   {origin        [50 50]
           gap-width     20
           border-radius 3
           class         "pause-symbol"}
    :as   opts}]
  (let [[origin-x origin-y] origin
        y               (- origin-y (/ height 2))
        offset-x-origin (- origin-x (/ bar-width 2))
        half-gap        (/ gap-width 2)
        attrs           {:y  y :width bar-width :height height
                         :rx border-radius :ry border-radius}]
    [:g {:class class}
     [:rect (merge {:x (- offset-x-origin half-gap)} attrs)]
     [:rect (merge {:x (+ offset-x-origin half-gap)} attrs)]]))

(defn play-button []
  [:svg.play-button
   {:view-box "0 0 100 100"}
   [:circle.button-circle
    {:r  50
     :cx 50
     :cy 50}]
   [play-symbol
    {:origin       [55 50]
     :height       50
     :stroke-width 8}]])

(defn resizer
  "Consists of a line and an invisible stretch element to widen the draggable
  area without taking up space."
  []
  [:div.resizer
   [:div.stretcher]
   [re-com/line
    :class "divider-line"]])