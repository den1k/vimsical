(ns vimsical.frontend.quick-search.style
  (:require [vimsical.frontend.styles.color :refer [colors]]))

(def quick-search
  (let [border-radius :5px]
    [:.quick-search
     {:position       :absolute
      :margin         :auto
      :top            :25%
      :left           0
      :right          0
      :z-index        :100
      :display        :flex
      :flex-direction :column
      :align-items    :center
      :border         :none
      :border-radius  border-radius
      :max-height     :300px
      :width          :500px
      :padding        :10px
      :background     :black}
     [:.input-and-filters
      {:width :100%}
      [:.input
       {:background    :black
        :padding       :5px
        :color         :snow
        :outline       :none
        :border        :none
        :border-radius border-radius
        :height        :50px
        :width         :100%
        :text-align    :baseline
        :font-size     :1.6rem}]
      [:.filters
       [:.title-bubble
        {:color         :snow
         :padding       "3px 7px"
         :border-radius "6px"}
        [:&.selected
         {:background (:darkgrey colors)}]]]]
     [:.search-results :.filter-results
      {:display        :flex
       :flex-direction :column
       :align-items    :stretch
       :width          :100%}
      [:.category-box
       {:flex 1}
       [:.title
        {:color          :snow
         :font-weight    100
         :letter-spacing :1px
         :font-size      :1rem}]]
      [:.search-result
       {:width           :100%
        :padding         :10px
        :display         :flex
        :justify-content :space-between
        :align-items     :center
        :color           :snow
        :background      :black
        :cursor          :pointer}
       [:&.selected
        {:background :grey}]]]]))
