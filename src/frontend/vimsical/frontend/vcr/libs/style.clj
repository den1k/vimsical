(ns vimsical.frontend.vcr.libs.style
  (:require [vimsical.frontend.styles.color :refer [colors]]))

(def libs
  (let [border-radius :5px]
    [:.libs
     {:align-self    :center
      :border-radius border-radius
      :margin        "50px 0"
      :min-width     :700px
      :max-width     :800px}
     [:.custom
      [:.add
       [:.src-input
        {:width :80%}]
       [:.add-button
        {:width :18%}
        [:&.invalid
         {:color        (:darkgrey colors)
          :border-color :red}]]]
      [:.added
       {:margin-top :10px
        :width      :80%}
       [:.added-lib
        {:cursor        :pointer
         :border-radius :2px
         :padding       :2px}
        [:&:hover
         {:background (:darkgrey-trans colors)}
         [:.close-icon
          {:visibility :visible}]]
        [:.close-icon
         {:visibility :hidden}]]]]
     [:.catalogue
      {:margin-top :40px}]
     [:.columns
      [:.column
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
         :height         :33px}]
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
            {:height      0             ; avoid padding
             :line-height 0
             :font-size   :20px}]]])]]]))
