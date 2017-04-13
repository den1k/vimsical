(ns vimsical.frontend.vcr.style
  (:require [vimsical.frontend.live-preview.style :refer [live-preview]]
            [vimsical.frontend.timeline.style :refer [timeline]]
            [vimsical.frontend.styles.color :as color :refer [colors]]))

(def play-pause
  [:&.play-pause
   [:.icon
    [:&.play {:height :23px
              :width  :23px}]
    [:&.pause {:height :21px
               :width  :21px}]]])

(def speed-control
  [:&.speed
   {:display         :flex
    :justify-content :space-between
    :align-items     :center}
   [:.speed-triangle
    {:width  :11px
     :height :11px}]])

(def playback
  [:.playback {:display         :flex
               :flex-direction  :row
               :justify-content :center
               :align-items     :center
               :padding         "13px 24px"}
   [:.control {:margin          "10px"
               :display         :flex
               :justify-content :center
               :align-items     :center
               :height          :24px
               :width           :85px
               :color           (:grey colors)}
    play-pause
    speed-control

    [:.icon {:fill   (:grey colors)
             :stroke (:grey colors)
             :cursor :pointer}
     [:&:hover
      [:* {:fill   :black
           :stroke :black}]]]]
   timeline])

(def editor-tab
  [:.editor-tab
   {:width         :24px
    :height        :24px
    :box-sizing    :border-box
    :padding       :5px
    :margin-top    :6.4px
    :margin-bottom :6.4px
    :border-radius :2.3px
    :background    :white
    :cursor        :pointer
    :box-shadow    "inset 0 2px 3px 0 rgba(171,165,165,0.5)"}
   [:.tab-checkbox
    {:width         :14px
     :height        :14px
     :border-radius :1.6px
     :background    :#f0805f
     :box-shadow    "0 2px 3px 0 rgba(107,94,94,0.5)"}]
   (mapv #(color/type-style (first %)
                            :.tab-checkbox.
                            :background
                            (second %))
         color/type-colors-editors)
   [:&.disabled
    {:background-color "rgba(219, 216, 217, 0.9)"}
    [:.tab-checkbox {:background :white}]]])

(def editor-tabs
  [:.editor-tabs {:width           :24px
                  :display         :flex
                  :flex-direction  :column
                  :justify-content :center}
   editor-tab])

(def n-h-split
  [:.rc-n-h-split
   {:border-top "solid 2px #eceff3"}
   live-preview
   [:.rc-n-h-split-splitter
    {:display      :flex
     :box-sizing   :border-box
     :padding      :4px
     ;; width set on component
     :background   :#ffffff
     :border-left  "solid 2px #eceff3"
     :border-right "solid 2px #eceff3"
     :cursor       :col-resize}
    {:flex            "1"
     :display         :flex
     :flex-direction  :column
     :justify-content :space-around
     :align-items     :center}
    editor-tabs]])

(def vcr
  [:.vcr
   ;; Styles to prevent code-editors' (Monaco) to overflow and cause scroll bars
   {:display  "relative"
    :overflow "hidden"}
   playback
   n-h-split])
