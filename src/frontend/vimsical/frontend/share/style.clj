(ns vimsical.frontend.share.style
  (:require [vimsical.frontend.styles.color :refer [colors]]))

(def share
  [:.share
   {:margin "50px 0"}
   [:.share-options
    [:.embed
     {:display :flex
      :width   :700px}
     [:.player
      {:margin-top :15px
       :box-shadow "0 10px 30px hsla(0,0%,0%,0.3)"}]
     [:.markup-and-copy
      {:margin-top :50px}
      [:.embed-markup
       {:width                      :640px
        :margin                     0
        :white-space                :pre-wrap
        :border-top-right-radius    0
        :border-bottom-right-radius 0
        :align-self                 :stretch}]
      [:.rc-point-wrapper
       {:flex-flow "none !important"}]
      [:.button
       {:width                     :60px
        :border-left               :none
        :border-top-left-radius    0
        :border-bottom-left-radius 0
        :align-self                :stretch}
       [:&:hover
        {:border      (str "1px solid " (:lightgrey colors))
         :border-left :none}]]]]]])
