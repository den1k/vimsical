(ns vimsical.frontend.modal.style
  (:require [vimsical.frontend.vims-list.style :refer [vims-list]]))

(def overlay
  [:&.modal-overlay
   {:filter "blur(3px)"}
   [:&:after
    {:content    "''"
     :position   :absolute
     :z-index    10
     :top        0
     :left       0
     :height     :100%
     :width      :100%
     :background "rgba(250,250,250,0.9)"}]])

(def modal
  [:.modal-container
   vims-list
   {:position   :absolute
    :width      :100%
    :z-index    11
    :overflow-y :auto}])