(ns vimsical.frontend.nav.style
  (:require [vimsical.frontend.styles.color :refer [colors]]
            [vimsical.frontend.vims-list.style :refer [vims-list]]))

(def nav
  [[:.route-landing
    [:.main-nav
     {:border :none}]]
   ;; bootstrap clear-fix breaks styles for .nav class
   [:.main-nav vims-list
    {:border-bottom "solid 2px #eceff3"
     :background    :white
     :padding       "8px 25px"}
    [:&.no-border
     {:border :none}]
    [:.vims-info
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
               :max-width      :280px
               ;; needed to show caret when empty
               :padding-left   :1px}
      [:&.editing :&:hover
       ;; preserves spaces in innerHTML (avoids non-breaking-space)
       {:white-space :pre-wrap
        :overflow    :visible}]
      [:&.untitled {:color (:grey colors)}]]]
    [:.button-group {:display       :flex
                     :border-radius :5px
                     :border        (str "1px solid " (:lightgrey colors))
                     :align-items   :center}
     [:.button {;;; divider btw button groups
                :border                     :none
                :border-top-right-radius    0
                :border-bottom-right-radius 0
                :border-right               (str "1px solid " (:lightgrey colors))}
      [:&:last-child {:border-right  :none
                      :border-radius :5px}]]]
    [:.user
     [:.avatar
      {:width  :30px
       :height :30px}]]]])