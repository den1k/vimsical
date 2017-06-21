(ns vimsical.frontend.app.style
  (:require [vimsical.frontend.styles.color :refer [colors]]
            [vimsical.frontend.modal.style :as modal]
            [vimsical.frontend.styles.media :as media]))

(def app-font-family
  "-apple-system, BlinkMacSystemFont, \".SFNSText-Regular\", \"San Francisco\", \"Roboto\", \"Segoe UI\", \"Helvetica Neue\", \"Lucida Grande\", sans-serif")

(def app-text-color :#4c4f59)

(def defaults
  [#_["@font-face"
      {:font-family "FiraCode-Retina"
       :src         "url(\"/fonts/FiraCode-Retina.otf\") format(\"opentype\")"}]

   [:html :body
    {:margin  0
     :padding 0}]

   [:html
    ;; https://www.paulirish.com/2012/box-sizing-border-box-ftw/
    {:box-sizing  :border-box
     :user-select :none}]

   (media/on-phone
    ; see index.html for scaling factor
    [:html
     {:font-size :20px}])

   [:* :*:before :*:after
    {:box-sizing  :inherit
     :user-select :inherit}]

   [:body :#app :.app :.main
    {:height :100%}]

   [:input :textarea
    {:user-select :text}]

   [:.app
    {:font-family     app-font-family
     :color           app-text-color
     :display         :flex
     :flex-direction  :column
     :justify-content :flex-start
     :align-items     :stretch
     :background      :#fff}]

   [:.main
    {:position        :relative         ; needed for modal-overlay
     :display         :flex
     :flex-direction  :column
     :justify-content :flex-start}
    modal/overlay]

   ;; on mobile height is set by ui.handlers
   (media/on-mobile
    [:.app
     {:transition "height 0.5s ease"}])

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
     :background     :#F8F8F8
     :border-bottom  "1px solid #E6E6E6"
     :border-style   :none
     :border-image   :none

     :font-weight    :300
     :letter-spacing :0.5px
     :font-size      :18px
     :padding        "11px 15px"}
    ["&::placeholder"
     {:color (:grey colors)}]
    ["&:focus"
     {:outline :none}]]

   ;; disable autofill styles in webkit browsers
   ["input:-webkit-autofill" "input:-webkit-autofill:hover" "input:-webkit-autofill:focus"
    {:box-shadow              "0 0 0px 1000px #F8F8F8 inset" ; like :background
     :-webkit-text-fill-color app-text-color ; like :color
     }]

   [:.popover
    {:font-family (str app-font-family " !important")}]

   [:.button :.input-button
    {:color           (:grey colors)
     :cursor          :pointer
     :background      :white
     :border          (str "1px solid " (:lightgrey colors))
     :box-shadow      :none
     :display         :flex
     :flex-shrink     0
     :justify-content :center
     :align-items     :center
     :border-radius   :5px
     :padding         "5px 10px"
     :outline         :none
     :white-space     :nowrap}
    [:&:hover
     {:color  (str (:darkgrey colors) " !important")
      :border (str "1px solid " (:grey colors))}]]])