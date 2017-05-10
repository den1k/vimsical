(ns vimsical.frontend.player.style
  (:require [vimsical.frontend.app.style :as app]
            [vimsical.frontend.live-preview.style :refer [live-preview]]
            [vimsical.frontend.code-editor.style :refer [code-editor]]
            [vimsical.frontend.user.style :refer [user]]
            [vimsical.frontend.views.style :as views]
            [vimsical.frontend.styles.color :as color]))

(def preview-panel
  [:.preview-panel
   {:width :100%}

   [:.play-symbol :.pause-symbol
    {:fill   :black                     ;; temp
     :stroke :black
     :cursor :pointer}]

   [:.bar
    {:padding "0 25px"}
    [:&.social
     [:.social-buttons
      {:display :flex}]
     [:.edit
      {:font-weight 300
       :font-size   :22px}]]
    [:&.timeline-container
     [:.play-pause
      {:height :20px}]

     [:.timeline
      {:position :relative}
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
    {:position :relative}
    [:.play-button-overlay
     {:position       :absolute
      :width          :100%
      :height         :100%
      :pointer-events :none}

     [:.play-button
      {:cursor         :pointer
       :height         :90px
       :pointer-events :all}
      [:.button-circle
       {:fill "rgba(200,200,200,0.4)"}]]]]
   live-preview])

(def editor-panel
  [:div.info-and-editor-panel
   {:position :relative
    :overflow :hidden}
   [:.info
    {:transition    "all 0.5s ease"
     :max-height    :60%
     :overflow-y    :hidden
     :padding       "5px 22px 0px 22px"
     :margin-bottom :15px}
    [:&.pan-out
     {:max-height    0
      :margin-bottom 0}]
    [:.header
     [:.avatar
      {:height :50px
       :width  :50px}]
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
     {:margin-top    :12px
      :font-size     :1rem
      :line-height   :1.45
      :overflow      :hidden
      :text-overflow :ellipsis}]]
   [:.code-editor
    {:flex          1
     ;; height of .bar
     :margin-bottom :50px}]
   code-editor
   [:.logo-and-file-type.bar
    {:padding    "0 10px"
     :position   :absolute
     :width      :100%
     :bottom     0
     :background :white}
    [:.logo-and-type
     {:width :60%}]
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
       :border-color (:javascript color/type->colors-editors)}]]]])

(def player
  [:.vimsical-frontend-player
   {:min-width :700px                   ; max embed width on medium
    :max-width :1200px
    :height    "100vh"}
   [:.rc-n-h-split
    ;; todo splitter-child line color
    {:height :100%}
    [:.rc-n-h-split-splitter
     {:position :relative}
     [:.resizer
      {:z-index 1}                      ;; lift above editor
      [:.stretcher
       {:position    :absolute
        :width       :20px
        :margin-left :-10px             ;; half-width to center
        :height      :100%}]
      [:.divider-line
       {:position :absolute
        :height   :100%
        :width    :1px}]]]]
   [:.bar
    {:height          "50px"
     :display         :flex
     :justify-content :space-between
     :align-items     :center}]
   preview-panel
   editor-panel])

(def embed-styles
  "Standalone styles for the embedded version of Player.
  Required in project.clj."
  [:#player
   {:width  :100vw
    :height :100vh}
   [app/defaults
    views/icons
    user
    player]
   ;; overwrite player styles to fill entire iframe
   [:.vimsical-frontend-player
    {:max-width :initial
     :width     :100vw
     :height    :100vh}]])