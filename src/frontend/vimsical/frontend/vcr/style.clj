(ns vimsical.frontend.vcr.style
  (:require [vimsical.frontend.timeline.style :refer [timeline]]
            [vimsical.frontend.code-editor.style :refer [code-editor]]
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
         color/type->colors-editors)
   [:&.disabled
    {:background-color "rgba(219, 216, 217, 0.9)"}
    [:.tab-checkbox {:background :white}]]])

(def editor-tabs
  [:.editor-tabs {:width           :24px
                  :display         :flex
                  :flex-direction  :column
                  :justify-content :center}
   editor-tab])

(def editor-header
  [:.editor-header
   {:display        :flex
    :flex-shrink    0
    :flex-direction :row
    :align-items    :center
    ;; height set on component due requirement of precise height by
    ;; split component
    ;:padding        "6px 16px"
    :text-align     :center
    :background     :#ffffff
    :border-bottom  "solid 2px #eceff3"}
   [:.title {:flex-grow      :1
             :font-size      :20.7px
             :letter-spacing :0}]
   (mapv #(color/type-child-style (first %)
                                  :&.
                                  :.title
                                  :color
                                  (second %))
         color/type->colors-editors)])

(def live-preview-and-editors
  [:.live-preview-and-editors
   {:border-top "solid 2px #eceff3"}
   [:.rc-n-v-split
    [:.split-panel
     [:&:first-child
      {:z-index    :inherit
       :border     :none
       :box-shadow :none}]
     {:z-index    :1
      :border-top "solid 2px #eceff3"
      :box-shadow "0 -4px 6px 0 rgba(143, 144, 150, 0.2)"}]]
   [:.rc-n-h-split-splitter
    {:display         :flex
     :flex            "1"
     :flex-direction  :column
     :justify-content :space-around
     :align-items     :center
     :box-sizing      :border-box
     :padding         :4px
     ;; width set on component due requirement of precise height by
     ;; split component
     :background      :white
     :border-left     "solid 2px #eceff3"
     :border-right    "solid 2px #eceff3"
     :cursor          :col-resize}
    editor-tabs]
   editor-header
   code-editor])

(def vcr
  [:.route-vims
   {:height :100vh}
   [:.vcr
    ;; Styles to prevent code-editor from overflowing beyond VCR's boundaries
    ;; Set here instead of on code-editor to allow widget overflow
    {:position :relative
     :overflow "hidden"}
    playback
    live-preview-and-editors]])
