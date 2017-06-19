(ns vimsical.frontend.landing.style
  (:require [garden.stylesheet :refer [at-media]]
            [vimsical.frontend.styles.color :refer [colors]]))

(def credit
  [[:.credit-wrapper {:display :inline-block}]
   [:.credit
    {:text-align :right
     :font-size  :12px
     :color      (:grey colors)}
    [:.title :.author
     {:cursor :pointer}
     [:&:hover
      {:color :black}]]]])

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
  [:.vimsical-stmt
   {:margin         "180px 0 100px"
    :letter-spacing :.004em}
   [:.header
    {:font-size :65px}]
   [:.subheader
    {:font-size :28px}]
   [:.join
    {:cursor :pointer}]])

(def explore-and-create
  [:.explore
   {:text-align :right}])

(def player
  [:.player-section
   [:.player
    {:margin-top :20px}
    {:border (str "1px solid " (:lightgrey colors))}]
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
    [:.logo-and-slogan
     {:width      :420px
      :transition "width 1s ease"}
     [:.learnable
      {:margin      0
       :font-size   :26px
       :font-weight 400}]
     [:.logo-and-type
      {:transform "scale(1.4)"}]]
    [:&.visible
     [:.logo-and-slogan
      {:width :520px}]]]
   [:.stmt
    {:font-size   :22px
     :margin-top  :50px
     :line-height 1.4
     :font-weight 300}]
   [:.act
    {:margin-top :100px}]])

(def waitlist
  [[:.bottom-waitlist
    {:margin-bottom :200px}
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
  [:.landing
   {:min-width :960px
    :max-width (str (* 1.5 960) "px")}
   [:.wrapper
    [:.section
     {:margin-top :200px}]

    ;; temp
    [:.lp-vims
     {:width      :400px
      :height     :300px
      :background :tomato}]
    [:.lp-vims-lg
     {:width      :700px
      :height     :400px
      :background :lightgreen}]
    credit
    headers
    vimsical-stmt
    explore-and-create
    player
    mission
    waitlist]

   footer])