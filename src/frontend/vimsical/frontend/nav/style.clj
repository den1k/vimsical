(ns vimsical.frontend.nav.style
  (:require [vimsical.frontend.styles.color :refer [colors]]
            [garden.stylesheet :as garden]
            [vimsical.frontend.styles.media :as media]))

(def nav
  [[:.route-landing :route-signup
    [:.main-nav
     {:border :none}]]
   ;; bootstrap clear-fix breaks styles for .nav class
   [:.main-nav
    {:border-bottom "solid 2px #eceff3"
     :background    :white
     :flex-shrink   0
     :padding       "8px 25px"
     :z-index       15}
    [:&.no-border
     {:border :none}]
    [:&.logged-in
     (media/on-mobile
      [:.vimsical-type
       {:display :none}])]
    (garden/at-media
     {:screen    true
      :max-width :900px}
     [:.user
      [:.name
       [:.last-name {:display :none}]]])
    [:.vims-info
     {:flex-shrink 0}
     [:* {:white-space :nowrap}]
     [:.title {:font-size      :1rem
               :font-weight    :500
               :letter-spacing :0.4px
               :text-align     :center
               :outline        :none
               :position       :relative
               :color          (:darkblue colors)

               ;; shrink title space when not editing
               :overflow       :hidden
               :text-overflow  :ellipsis
               :max-width      :100%    ; needed for truncate
               ;; needed to show caret when empty
               :padding-left   :1px}
      [:&.editing :&:hover
       ;; preserves spaces in innerHTML (avoids non-breaking-space)
       {:white-space :pre-wrap
        :overflow    :visible}]
      [:&.untitled {:color (:grey colors)}]]]
    [:.new-and-my-vims.button-group
     {:display       :flex
      :overflow      :auto
      :flex-shrink   0
      :border-radius :5px
      :border        (str "1px solid " (:lightgrey colors))
      :align-items   :center}
     [:.button {;;; divider btw button groups
                :border                     :none
                :border-radius              0
                :border-top-right-radius    0
                :border-bottom-right-radius 0
                :border-right               (str "1px solid " #_(:lightgrey colors))}
      [:&:last-child {:border-right :none}]]]
    [:.user
     [:.avatar
      {:width  :30px
       :height :30px}]]]])