(ns vimsical.frontend.quick-search.style)

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
     [:.search-result
      {:width           :100%
       :padding         :10px
       :display         :flex
       :justify-content :space-between
       :color           :snow
       :background      :black
       :cursor          :pointer}
      [:&.selected :&:hover
       {:background :grey}]]]))
