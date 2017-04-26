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
     :display         :flex
     :flex-direction  :column
     :justify-content :flex-start
     :background      :#fff}]


   ;; flex-box

   [:.jc
    {:display         :flex
     :justify-content :center}]

   [:.jsb
    {:display         :flex
     :justify-content :space-between}]

   [:.jsa
    {:display         :flex
     :justify-content :space-around}]

   [:.ac
    {:display     :flex
     :align-items :center}]

   [:.asc
    {:display    :flex
     :align-self :center}]

   [:.dc
    {:display        :flex
     :flex-direction :column}]

   [:.f1
    {:display :flex
     :flex    :1}]

   [:a
    {:color           "#337ab7 !important"
     :text-decoration :none}]

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

   [:.popover
    {:font-family (str app-font-family " !important")}]

   [:.button :.input-button
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
     {:color  (str (:darkgrey colors) " !important")
      :border (str "1px solid " (:grey colors))}]
    [:.btn-title
     {:white-space :nowrap}]]
   ["input:-webkit-autofill"
    {:box-shadow "0 0 0px 1000px white inset"}]])
