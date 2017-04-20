(ns vimsical.frontend.nav.style
  (:require [vimsical.frontend.styles.color :refer [colors]]))

(def nav
  ;; bootstrap clear-fix breaks styles for .nav class
  [:.main-nav
   {:border-bottom   "solid 2px #eceff3"
    :background      :white
    :display         :flex
    :align-items     :center
    :padding         "8px 25px"
    :justify-content :space-between}
   [:.vims-info {:display         :flex
                 :justify-content :center
                 :align-items     :center
                 :margin-left     :80px}
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
   [:.user
    [:.avatar
     {:width  :30px
      :height :30px}]]])