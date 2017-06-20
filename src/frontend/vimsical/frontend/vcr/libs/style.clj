(ns vimsical.frontend.vcr.libs.style
  (:require [vimsical.frontend.styles.color :refer [colors]]))

(def libs
  (let [border-radius :5px]
    [:.libs
     {:align-self    :center
      :border-radius border-radius
      ;:max-height    :300px
      :margin        "50px 0"
      ;:width         :100%
      :padding       :10px
      ;:background    :black
      }
     [:.lib-search
      {:max-width :700px}
      [:.input
       {:background    :black
        :padding       :5px
        ;:color         :snow
        :outline       :none
        :border        :none
        :border-radius border-radius
        :height        :50px
        :width         :100%
        :text-align    :baseline
        :font-size     :1.6rem}]]
     [:.results
      {:display      :table
       :table-layout :fixed
       :width        "calc(50% - 30px)" ; gap size
       ;:border       "1px solid orange"
       :font-size    :16px}
      [:.title
       {:display        :table-caption
        :font-weight    200
        :letter-spacing :1.1px
        :font-size      :25px
        :height         :40px}]
      (let [border-radius :2px]
        [:.res-row
         {:display :table-row
          :cursor  :pointer}
         [:&:hover :&.selected
          {:background (:darkgrey-trans colors)}]
         [:.cell
          {:display        :table-cell
           ; :border         "1px solid green"
           :padding        "4px 0"
           :vertical-align :middle}]
         [:.name
          {:width                     :50%
           :border-top-left-radius    border-radius
           :border-bottom-left-radius border-radius}]
         [:.version
          {:width :30%}]

         [:.added
          {:border-top-right-radius    border-radius
           :border-bottom-right-radius border-radius}
          [:&.cell
           {:width :20px}]
          [:.icon
           {:height      0              ; avoid padding
            :line-height 0
            :font-size   :20px}]]])]]))
