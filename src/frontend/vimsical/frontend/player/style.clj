(ns vimsical.frontend.player.style
  (:require [vimsical.frontend.live-preview.style :refer [live-preview]]
            [vimsical.frontend.code-editor.style :refer [code-editor]]
            [vimsical.frontend.styles.color :as color]))

(def preview-panel
  [:.preview-panel
   {:width           :100%
    :display         :flex
    :flex-direction  :column
    :justify-content :space-between}

   [:.play-symbol :.pause-symbol
    {:fill   :black                     ;; temp
     :stroke :black
     :cursor :pointer}]

   [:.bar
    [:&.social
     [:.social-buttons
      {:display :flex}]]
    [:&.timeline-container
     [:.play-pause
      {:height :20px}]

     [:.timeline
      {:display     :flex
       :align-items :center
       :flex        1
       :position    :relative}
      [:.progress
       {:height        :8px
        :width         :100%
        :border-radius :4px
        :position      :absolute}
       [:&.left
        {:background :lightgrey}]
       [:&.passed
        {:background :black
         :width      :55px}]]

      [:.playhead
       {:left          :50px
        :width         :16px
        :height        :16px
        :border-radius :50%
        :background    :black
        :position      :absolute}]]]]
   [:.preview-container
    {:display  :flex
     :position :relative
     :flex     1}
    [:.play-button-overlay
     {:position        :absolute
      :width           :100%
      :height          :100%
      :display         :flex
      :justify-content :center
      :align-items     :center
      :pointer-events  :none}

     [:.play-button
      {:cursor         :pointer
       :height         :90px
       :pointer-events :all}
      [:.button-circle
       {:fill "rgba(200,200,200,0.4)"}]]]]
   live-preview])

(def editor-panel
  [:div.info-and-editor-panel
   {:display        :flex
    :flex-direction :column
    :overflow       :hidden}
   [:.info
    {:height        :50%
     :padding       :15px
     :margin-bottom :15px               ;; is this ok?
     :overflow-y    :scroll}
    [:.header
     {:display     :flex
      :align-items :center}
     [:div.title-and-creator
      {:margin-left :10px
       :line-height 1.2}
      [:.title
       {:font-size   :20px
        :font-weight :bold}]
      [:.creator
       {:font-size :16px
        :color     :grey}]]]
    [:.desc
     {:margin-top :12px
      }]]
   [:.code-editor
    {:flex  1
     :width :100%}]
   code-editor
   [:.logo-and-file-type.bar]
   [:.active-file-type
    {:padding       "1px 13px"
     :border        "1px solid"
     :border-radius :15px}
    ;; todo dry
    [:&.html
     {:color        (:html color/type->colors-editors)
      :border-color (:html color/type->colors-editors)}]
    [:&.css
     {:color        (:css color/type->colors-editors)
      :border-color (:css color/type->colors-editors)}]
    [:&.js
     {:color        (:javascript color/type->colors-editors)
      :border-color (:javascript color/type->colors-editors)}]]])

(def player
  [:.vimsical-frontend-player
   {:margin-top "20px"                  ;; temp
    :height     "100vh"}
   [:.rc-n-h-split
    ;; todo splitter-child line color
    {:height :100%}]
   [:.bar
    {:height          "50px"
     :display         :flex
     :justify-content :space-between
     :align-items     :center}]
   preview-panel
   editor-panel])