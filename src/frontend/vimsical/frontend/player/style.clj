(ns vimsical.frontend.player.style
  (:require [vimsical.frontend.app.style :as app]
            [vimsical.frontend.live-preview.style :refer [live-preview]]
            [vimsical.frontend.code-editor.style :refer [code-editor]]
            [vimsical.frontend.user.style :refer [user]]
            [vimsical.frontend.views.style :as views]
            [vimsical.frontend.styles.color :as color]
            [vimsical.frontend.styles.media :as media]))

(def timeline
  [:.timeline
   {:position :relative
    :height   :100%
    :cursor   :pointer}
   [:.time
    {:height        :4px
     :width         :100%
     :border-radius :4px
     :position      :absolute}
    [:&.left
     {:background :lightgrey}]
    [:&.passed
     {:background :black}]]
   [:.head
    {:background          :black
     :position            :absolute
     :transition          "all 0.2s ease" ; `all` is overridden by transition-prop
     :transition-property "margin-left, width, height, border-radius"}
    [:&.playhead
     {:height        :16px
      :width         :16px
      :margin-left   :-8px
      :border-radius :50%}]
    [:&.skimhead
     {:height        :18px
      :width         :4px
      :margin-left   :-2px
      :border-radius 0}]]])

(def timeline-container
  [[:.timeline-container
    {:padding "0 18px"
     :width   "100%"}                   ; less pad than top bar
    [:.play-pause
     {:height :20px
      :cursor :pointer}]
    timeline
    [:.time-or-speed-control
     {:cursor      :pointer
      ;; monospaced to accurately measure width
      :font-family "Droid Sans Mono"
      ;; hardcode width of 4 characters to avoid shifting timeline
      ;; time default, e.g. `3:42`
      :width       :33px}]]
   (media/on-mobile
    [:.time-or-speed-control
     {:font-size :18px}])])

(def preview-panel
  [:.preview-panel
   {:width :100%}])

(def preview-container
  [[:.preview-container
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
   [:.portrait :.preview-container
    {:flex :2}]])

(def play-pause-buttons
  [[:.play-button
    {:width     :15%
     :min-width :60px
     :max-width :80px}]
   [:.play-symbol :.pause-symbol
    {:fill   :black                     ;; temp
     :stroke :black
     :cursor :pointer}]])

(def explore
  [:.explore
   {:font-weight 500
    :cursor      :pointer
    :font-size   :18px}])

(def preview
  "Styles for preview area. Not nested to allow for reuse between landscape and
  portrait orientation."
  [preview-panel
   preview-container                    ; inside panel in landscape
   timeline-container
   live-preview

   play-pause-buttons
   explore])

(def editor-panel
  [(media/on-mobile
    [:.info-and-editor-panel
     [:.info
      ["&::-webkit-scrollbar"           ; wider on mobile
       {:width "9px !important"}]]])
   [:.info-and-editor-panel
    {:flex     1
     :position :relative
     :overflow :hidden}
    [:&.show-info
     [:.info
      {:max-height                 :60%
       :overflow-y                 :scroll
       :padding-top                :5px
       ;; momentum scrolling for iOS
       ;; https://css-tricks.com/snippets/css/momentum-scrolling-on-ios-overflow-elements/
       :-webkit-overflow-scrolling :touch}]
     [:.code-editor
      {:margin-top :15px}]]
    [:.info
     {:padding       "0px 22px"
      :max-height    0
      :margin-right  :1px               ; margin for scrollbar
      :margin-bottom 0}
     {:transition          "all 0.3s ease"
      :transition-property "max-height, padding-top"}
     ; light scrollbar to match code-editor
     ["&::-webkit-scrollbar"
      {:background :white
       :width      :7px}]
     ["&::-webkit-scrollbar-thumb"
      {:background :white
       :border     "1px solid lightgrey"}]
     [:.header
      [:.avatar
       {:height :50px
        :width  :50px}]
      [:div.title-and-creator
       {:margin-left    :10px
        :line-height    1.2
        :letter-spacing :0.5px}
       [:.title
        {:font-size   :18px
         :font-weight :600}]
       [:.creator
        {:font-size :14px
         :color     :grey}]]]
     [:.desc
      {:margin-top  :12px
       :font-size   :1rem
       :line-height :1.45}]]
    [:.code-editor
     {:flex          1
      ;; height of .bar
      :margin-bottom :50px}]
    code-editor
    [:.code-editor
     [:.slider
      {:background :white
       :border     "1px solid lightgray"}]]
    [:.logo-and-file-type.bar
     {:padding    "0 18px"
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
        :border-color (:javascript color/type->colors-editors)}]]]]])

(def landscape-split
  [:.landscape-split
   ;; todo splitter-child line color
   {:height :100%}
   [:.rc-n-h-split-splitter
    {:position :relative}
    [:.resizer
     {:z-index 1}                       ;; lift above editor
     [:.stretcher
      {:position    :absolute
       :width       :20px
       :margin-left :-10px              ;; half-width to center
       :height      :100%}]
     [:.divider-line
      {:position :absolute
       :height   :100%
       :width    :1px}]]]])

(def portrait-split
  [:.portrait-split
   {:height         :100%
    :display        :flex
    :flex-direction :column}])

(def bar
  [[:&.portrait
    [:.bar
     {:padding "0 18px"}]]
   [:&.landscape
    [:.bar
     {:padding "0 22px"}]]
   [:.bar
    {:height          "50px"
     :display         :flex
     :justify-content :space-between
     :align-items     :center}
    [:&.social
     [:.social-buttons
      {:display :flex}]]]])

(def player
  [:.vimsical-frontend-player
   {:background :white
    :height     :100%}
   [:&.landscape
    {:display   :flex
     :min-width :700px
     :max-width :1200px}
    landscape-split
    [:.explore
     {:margin-left :18px}]]
   [:&.portrait
    {:display        :flex
     :flex-direction :column
     :width          :100%}
    portrait-split]
   bar
   preview
   editor-panel])

(def embed-styles
  "Standalone styles for the embedded version of Player.
  Required in project.clj."
  [app/defaults
   views/icons
   user
   player
   ;; overwrite player styles to fill entire iframe
   [:.vimsical-frontend-player
    {:max-width :initial
     :width     :100vw
     :height    :100vh}]])