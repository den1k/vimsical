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
      :letter-spacing :0.3px}]]])

(def headers
  [[:.header
    {:font-size   :40px
     :font-weight :600}]
   [:.subheader
    {:margin-top     :0
     :font-size      :20px
     :font-weight    :400
     :line-height    :1.2
     :letter-spacing :0.02em}]])

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
      {:white-space   :nowrap
       :font-size     :28px
       :margin-bottom :5px}]
     [:.join
      {:cursor :pointer}]]]
   [:.preview-wrapper
    {:flex 0.5}
    [:.live-preview
     {:width         :100%
      :border-radius :3px
      ;:position :absolute
      ;:height :300px
      }]]])

(def create-and-explore
  [[:.create-section
    {:color      :white
     :background "linear-gradient(to bottom, rgb(0, 0, 0), rgb(67, 67, 67))"
     ;:background :black
     }
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
    {:background (:beatwhite colors)    ;:#F2F2F2
     }
    [:.video-wrapper
     {:transform-origin :left}
     [:.video
      {:border "1px solid hsl(0, 0%, 93%)"}]]]
   [:.explore-section :.create-section
    [:.visibility
     [:&.visible
      [:.video-wrapper
       {:transform "scale(1)"}
       #_{:width :85%}]]]
    [:.header
     {:font-size :80px}]
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
   {:background :mintcream}
   [:.header
    {:font-size :80px}]
   [:.visibility
    {:align-items :center}
    [:.player-wrapper
     {:transform  "scale(0.95)"
      :transition "transform 0.5s ease"}]
    [:&.visible
     [:.player-wrapper
      {:transform "scale(1)"}]]]
   [:.credit-wrapper
    {:width :100%}]
   [:.player-wrapper
    {:margin-top :10px
     :box-shadow "0 10px 20px hsla(0, 0%, 20%, 0.3)"}]
   [:.player
    {:max-width  :100%
     :max-height :100%}]
   [:.sub-stmt
    {:margin-top "5vh"}]
   (media/on-mobile
    [:.player
     {:height :70vh}])])

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
      {:width :80px}]
     [:.logo-and-slogan]]]
   (media/on-mobile
    [:.stmt
     {:font-size :14px}])
   (media/not-on-mobile
    [:.stmt
     {:font-size :22px}])
   [:.stmt
    {:margin-top  :50px
     :line-height 1.4
     :font-weight 300}]
   [:.act
    {:margin-top :100px}]])

(def waitlist
  [[:.bottom-waitlist
    {;:padding-top    :300px
     ;:padding-bottom :400px
     }
    [:.join
     {:font-size      :60px
      :letter-spacing :.02em
      :font-weight    :400}]]
   [:.waitlist
    {:margin-top :1em}

    [:.form
     [:.email
      {:height :50px
       :border "1px solid hsl(0, 0%, 93%)"}]
     [:.button
      {:margin-left :20px
       :height      :50px}]]
    [:.result
     {:font-size   :1rem
      :margin-top  :20px
      :line-height :20px
      :text-align  :center}]]])

(def footer
  [:.footer-logo
   {:margin "15px auto"}
   [:.vimsical-logo
    {:height :45px
     :width  :45px}]
   [:.vimsical-type
    {:width       :140px
     :margin-left :23px
     :fill        :black}]])

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

(def landing
  [(media/not-on-mobile
    [:.landing
     {:min-width :960px}])
   [:.landing
    {:color     :black
     :width     :100%
     :max-width (str (* 1.4 960) "px")
     :position  :relative}
    [:.section
     {:display         :flex
      :justify-content :space-around
      :align-items     :center
      :padding-top     :5vh
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
    vimsical-stmt
    create-and-explore
    player
    mission
    waitlist
    vims-preview-sections
    footer]])