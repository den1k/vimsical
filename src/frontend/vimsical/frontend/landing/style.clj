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

(def vimsical-stmt
  [:.stmt-wrapper
   {:margin "180px 0 100px"}
   [:.vimsical-stmt
    {:letter-spacing :.004em
     :flex           0.5}
    [:.header.vimsical
     {:font-size     :65px
      :margin-bottom :5px}]
    [:.subheader
     {:font-size :28px}]
    [:.join
     {:cursor :pointer}]]
   [:.preview-wrapper
    {:position :relative}
    [:.live-preview
     {:flex     0.4
      :position :absolute
      :height   :300px}]]])

(def explore-and-create
  [[:.create
    {:align-items :flex-end
     :text-align  :right}
    [:.video
     {:align-self :flex-end}]]
   [:.video-wrapper
    {:width :80%}
    [:.video
     {:width  :100%
      :border "1px solid hsl(0, 0%, 93%)"}]]])

(def player
  [:.player-section
   [:.credit-wrapper
    {:width :100%}
    [:.player
     {:height :40vh}
     {:margin-top :10px}
     {:border (str "1px solid " (:lightgrey colors))}]
    (media/on-mobile
     [:.player
      {:height :70vh}])]
   [:.embed-stmt
    {:font-weight    200
     :letter-spacing :.4px
     :font-size      :25px
     :margin-top     :120px
     :text-align     :center}
    [:.bold
     {:font-weight 400}]]

   [:.desc
    {:margin-top :50px
     :text-align :center
     :width      :400px
     :font-size  :20px}]])

(def mission
  [:.mission-section
   {:text-align :center}
   [:.visibility
    {:width       :100%
     :align-items :center}
    [:.logo-and-slogan
     {:width      :380px
      :transition "width 1s ease"}
     [:.learnable
      {:margin      0
       :font-size   :26px
       :font-weight 400}]]
    [:&.visible
     [:.logo-and-slogan
      {:max-width :520px
       :width     :100%}]]]
   [:.stmt
    {:font-size   :22px
     :margin-top  :50px
     :line-height 1.4
     :font-weight 300}]
   [:.act
    {:margin-top :100px}]])

(def waitlist
  [[:.bottom-waitlist
    {:margin-top    :300px
     :margin-bottom :400px}
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

(def landing
  [(media/not-on-mobile
    [:.landing
     {:min-width :960px}])
   [:.landing
    {:width     :100%
     ;:overflow-x  :hidden                 ; hide page-vims
     :max-width (str (* 1.4 960) "px")
     :position  :relative}
    [:.wrapper
     {:width      :85%
      :align-self :center
      :z-index    1}
     [:.section
      {:margin-top :20vh}]
     [:.visibility
      {:display        :inline-flex
       :flex-direction :column
       :vertical-align :top}]
     credit
     headers
     vimsical-stmt
     explore-and-create
     player
     mission
     waitlist]
    #_[:.preview-wrap
       {:position  :absolute
        :width     :100%
        :height    :100%
        :top       :-75px
        :left      :-198px
        :transform "rotateZ(45deg)"}
       [:.live-preview
        {:width  :100%
         :height :100%}]]

    footer]])