(ns vimsical.frontend.app.style)

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
     :background      :#fff}]])
