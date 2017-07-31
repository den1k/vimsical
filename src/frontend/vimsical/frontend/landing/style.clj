(ns vimsical.frontend.landing.style
  (:require [garden.stylesheet :refer [at-media]]
            [vimsical.frontend.styles.color :refer [colors]]
            [vimsical.frontend.styles.media :as media]))

(def credit
  [[:.credit-wrapper
    {:display        :inline-flex
     :flex-direction :column}]
   [:.credit
    {:text-align :right
     :font-size  :12px
     :color      (:grey colors)}
    [:.title :.author :.explore
     {:cursor :pointer}
     [:&:hover
      {:color :black}]]
    [:.explore
     {:font-weight    500
      :letter-spacing :0.3px}]]
   (media/on-mobile
    [:.credit
     {:font-size :8px}])])

(def headers
  [[:.header
    {:font-size   :40px
     :font-weight :600}]
   [:.subheader
    {:margin-top     :0
     :font-size      :20px
     :font-weight    :400
     :line-height    :1.2
     :letter-spacing :0.02em}]
   (media/on-mobile
    [:.header
     {:font-size :30px}]
    [:.subheader
     {:margin-top :0
      :font-size  :15px}])])

(def sub-statement
  [:.sub-stmt
   {:font-weight    200
    :letter-spacing :.4px
    :font-size      :25px
    :text-align     :center}
   [:.bold
    {:font-weight 400}]])

(def vimsical-stmt
  [:.page-header-section
   {:padding "6vh 0 8vh"}
   [:.sub-section
    {:flex-direction  :row
     :justify-content :space-between}
    [:.vimsical-stmt
     {:letter-spacing :.004em
      :flex           0.5}
     [:.header.vimsical
      {:font-size     :80px
       :margin-bottom :5px}]
     [:.subheader
      {;:white-space   :nowrap
       :font-size     :28px
       :margin-bottom :5px}]
     [:.join
      {:cursor :pointer}]
     (media/on-mobile
      [:.header.vimsical
       {:font-size     :40px
        :margin-bottom :5px}]
      [:.subheader
       {:font-size     :16px
        :margin-bottom :5px}]
      [:.join
       {:font-size :8px
        :cursor    :pointer}])]]
   [:.preview-wrapper
    {:flex 0.5}
    [:.credit-wrapper
     {:width :100%}
     [:.vims-preview
      {:height :400px}
      [:.live-preview
       {:width         :100%
        :height        :100%
        :border-radius :3px}]]]]
   (media/on-mobile
    [:.vims-preview
     {:height :300px}])])

(def create-and-explore
  [[:.create-section
    {:color      :white
     :background "linear-gradient(to bottom, rgb(0, 0, 0), rgb(100, 100, 100))"}
    [:.credit
     {:color :lightgrey}
     [:.title :.author :.explore
      [:&:hover
       {:color (:grey colors)}]]]
    [:.visibility
     {:align-items :flex-end}]
    [:.sub-section
     {:align-items :flex-end
      :text-align  :right}]
    [:.video-wrapper
     {:transform-origin :right}
     [:.video
      {:border-radius :3px}]]]
   [:.explore-section
    {:background :black
     :color      :white}
    [:.credit
     [:.title :.author :.explore
      [:&:hover
       {:color :white}]]]
    [:.video-wrapper
     {:transform-origin :left}
     [:.video
      {:border "1px solid white"}]]]
   [:.explore-section :.create-section
    [:.visibility
     [:&.visible
      [:.video-wrapper
       {:transform "scale(1)"}
       #_{:width :85%}]]]
    [:.header
     {:font-size :80px}]
    (media/on-mobile
     [:.header
      {:font-size :40px}]
     [:.subheader
      {:font-size :12px}])
    [:.video
     {:align-self :flex-end}]
    [:.video-wrapper
     {:width :85%}
     {:transform  "scale(0.95)"
      :transition "transform 0.5s ease-out"}
     [:.video
      {:width :100%}]]]])

(def player
  [:.player-section
   {:background  :#ffee58
    :position    :relative
    :padding-top :7vh}
   [:.header
    {:font-size :50px}]
   [:.try-cta-box
    {:font-size :18px
     :position  :relative}
    [:.coder-emojis
     {:display :none}
     {:font-size :22px}]
    [:.pointer
     {:font-size :25px
      :position  :absolute
      :right     :-20px
      :bottom    :-7px
      :transform "scaleX(-1) rotate(25deg)"}]]
   [:.credit
    {:position :absolute
     :right    0
     :bottom   0
     :margin   :5px}]
   [:.visibility
    {:align-items :center
     :width       :100%}
    [:.player-wrapper
     {:width      :80%
      :transform  "scale(0.95)"
      :transition "transform 0.5s ease"}]
    (media/on-mobile
     [:.player-wrapper
      {:width :100%}])
    [:&.visible
     [:.player-wrapper
      {:transform "scale(1)"}]]]
   [:.credit-wrapper
    {:width :100%}]
   [:.player-wrapper
    {:margin-top :10px}]
   [:.player
    {:max-width  :100%
     :height     :500px
     :box-shadow "0 10px 20px hsla(0, 0%, 20%, 0.3)"}]
   [:.sub-stmt
    {:margin-top "5vh"}]
   (media/on-mobile
    [:.player
     {:height :80vh}
     [:.live-preview
      {:position :relative}
      [:.iframe
       {:position         :absolute
        :width            :200%
        :height           :200%

        :transform        "scale(0.5)"
        :transform-origin "top left"}]]])])

(def mission
  [:.mission-section
   {:text-align     :center
    :padding-bottom :5vh}
   [:.visibility
    {:width       :100%
     :align-items :center}
    [:.logo-and-slogan
     [:.stretcher
      {:width      :0
       :transition "all 1s ease"}]
     [:.learnable
      {:margin      0
       :font-size   :26px
       :flex-shrink 0
       :font-weight 400}]]
    [:&.visible
     [:.stretcher
      {:width :80px}]]]
   [:.stmt
    {:font-size   :14px
     :margin-top  :50px
     :line-height 1.4
     :font-weight 300}]
   (media/on-mobile
    [:.visibility
     [:.logo-and-slogan
      {:max-width :80%}
      [:.logo-and-type
       {:transform        "scale(0.7)"
        :transform-origin "center left"}]
      [:.learnable
       {:font-size :15px}]]])
   (media/not-on-mobile
    [:.stmt
     {:font-size :22px}])
   [:.act
    {:margin-top :100px}]])

(def waitlist
  [[:.bottom-waitlist
    {:padding "20vh 0"}
    [:.join
     {:letter-spacing :.02em
      :font-weight    :400}]
    [:.get-demo-box
     {:margin-top :20px}
     [:.get-demo
      {:font-weight :500}]
     (media/not-on-mobile
      [:.get-demo
       {:font-size :45px}])
     [:.waitlist
      {:position :relative
       :padding  0
       :margin   0}]]]
   (media/on-mobile
    [:.bottom-waitlist
     {:padding-top :70px}
     [:.join
      {:font-size :30px}]
     [:.get-demo
      {:font-size :25px}]
     ])
   [:.waitlist
    {:margin-top :2em}

    [:.form
     [:.email
      {:height :50px
       :border "1px solid hsl(0, 0%, 93%)"}]
     [:.button
      {:margin-left :20px
       :height      :50px}]]
    [:.result
     {:font-size   :1rem
      :position    :absolute
      :width       :100%
      :margin-top  :10px
      :line-height :20px
      :text-align  :center}]]])

(def footer
  [:.footer
   {:padding "0px 15px"
    :color   (:grey colors)}
   [:.footer-box
    {:width :250px}]
   [:.built-contact.mobile
    {:width         :100%
     :margin-bottom :20px}]
   [:.contact:hover
    {:cursor :pointer
     :color  :black}]
   [:.footer-logo
    {:padding-bottom "5px"}
    [:.vimsical-logo
     {:height :38px
      :width  :38px}]
    [:.vimsical-type
     {:width       :130px
      :margin-left :17px
      :fill        :black}]]])

(defn vims-preview-section
  ([class defaults] (vims-preview-section class defaults nil))
  ([class defaults in-range]
   (let [class (str "&." class)]
     [:.visibility
      [:.vims-preview-section
       (into [class] defaults)]
      (when in-range
        [:&.in-range
         [:.vims-preview-section
          (into [class] in-range)]])])))

#_[:.vims-preview-section
   [:&.teach-by-doing
    {:color      :black
     :background :white}
    [:.live-preview
     {:height :400px}]]
   [:&.create-watch-explore
    {:background :black
     :color      :white}
    [:.live-preview
     {:height :400px}]]]

(def vims-preview-sections
  [(vims-preview-section
    "bezier"
    [{:height     :500px
      :background "rgb(15, 243, 208)"}
     [:.sub-stmt
      {:font-weight 400}]
     [:.vims-preview]]
    [[:.vims-preview
      {:opacity 0.4}]])
   (vims-preview-section
    "teach-by-doing"
    [{:height     :500px
      :background :white}
     [:.sub-stmt
      {:font-weight 500}]])
   (vims-preview-section
    "create-watch-explore"
    [{:height     :500px
      :background :black}
     [:.sub-stmt
      {:color :white}
      {:font-weight 500}]])

   [:.visibility
    [:&.in-range
     [:.vims-preview-section
      [:.sub-stmt
       {:visibility :visible}]
      [:.vims-preview
       {:filter  "blur(10px)"
        :opacity 0.3}]]]

    [:.vims-preview-section
     {:padding         "10vh 0"
      :justify-content :center}
     [:.sub-stmt
      {:visibility :hidden}]
     [:&.trail
      {:height :400px}]

     [:.vims-preview
      {:z-index    -1
       :position   :absolute
       :width      :80%
       :height     :80%
       :transition "all 0.5s ease"}
      [:.live-preview
       {:width  :100%
        :height :100%}
       [:.iframe
        {:background :transparent}]]]]]])

(def product-stmts-section
  [:.product-stmts-section
   {:background :black
    :color      :white}
   [:.product-stmts
    [:.product-stmt-wrapper
     [:.icon {:flex-shrink 0}
      [:.stmt-icon
       {:stroke :white
        :width  :100px                  ; crane
        :height :90px}
       [:&.deer
        {:stroke-width :1.3px}]
       [:&.monkey
        {:height    :80px
         :transform "rotateY(180deg)"}]]]
     [:.product-stmt
      {:margin-left :50px}
      [:.title
       {:font-weight 600
        :font-size   :36px}]
      [:.stmt
       {:font-size :20px}]]
     (media/on-mobile
      [:.product-stmt
       {:margin-left :20px}
       [:.title {:font-size :20px}]
       [:.stmt {:font-size :16px}]])]]])

(def landing
  [(media/not-on-mobile
    [:.landing
     {:min-width :960px}])
   [:.landing
    {:flex-shrink 0
     :color       :black
     :width       :100%
     :position    :relative}
    [:.section
     {:display         :flex
      :justify-content :space-around
      :align-items     :center
      :padding-top     :10vh
      :padding-bottom  :10vh
      :z-index         1
      :position        :relative}
     [:.sub-section
      {:display        :flex
       :flex-direction :column
       :width          :85%}]]
    [:.visibility
     {:display        :inline-flex
      :flex-direction :column
      :vertical-align :top}]
    credit
    headers
    sub-statement
    ;vimsical-stmt
    ;create-and-explore
    product-stmts-section
    player
    ;mission
    waitlist
    ;vims-preview-sections
    footer]])