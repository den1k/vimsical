(ns vimsical.frontend.app.style
  (:require [vimsical.frontend.styles.color :refer [colors]]))

(def app-font-family
  "-apple-system, BlinkMacSystemFont, \".SFNSText-Regular\", \"San Francisco\", \"Roboto\", \"Segoe UI\", \"Helvetica Neue\", \"Lucida Grande\", sans-serif")

(def defaults
  [#_["@font-face"
      {:font-family "FiraCode-Retina"
       :src         "url(\"/fonts/FiraCode-Retina.otf\") format(\"opentype\")"}]

   [:html :body
    {:margin  0
     :padding 0}]

   ;; https://www.paulirish.com/2012/box-sizing-border-box-ftw/
   [:html
    {:box-sizing  :border-box
     :user-select :none}]

   [:* :*:before :*:after
    {:box-sizing  :inherit
     :user-select :inherit}]

   [:input :textarea
    {:user-select :text}]

   [:.app
    {:font-family     app-font-family
     :color           :#4c4f59
     :width           :100vw
     :height          :100vh
     :display         :flex
     :flex-direction  :column
     :justify-content :flex-start
     :background      :#fff}]

   [:input
    {:border-radius  :5px
     ;:border-width  0
     :border-style   :none
     :border-image   :none
     :border         (str "1.5px solid " (:grey colors))
     :font-weight    :300
     :letter-spacing :0.5px
     :font-size      :18px
     :padding        "13px 20px"}]

   [:.button
    {:color           (:grey colors)
     :cursor          :pointer
     :background      :white
     :border          (str "1px solid " (:lightgrey colors))
     :height          :36px
     :display         :flex
     :justify-content :center
     :align-items     :center
     :border-radius   :5px
     :padding         "10px 14px"
     :outline         :none}
    [:&:hover
     {:color (str (:darkgrey colors) " !important")}]
    [:&.active
     {:color (str (:darkgrey colors) " !important")}]
    [:.btn-title
     {:white-space :nowrap}]]])
