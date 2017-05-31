(ns vimsical.frontend.modal.style)

(def overlay
  [:&.modal-overlay
   {:filter "blur(3px)"}
   [:&:after
    {:content    "''"
     :position   :absolute
     :z-index    :10
     :top        0
     :left       0
     :height     :100%
     :width      :100%
     :background "rgba(255,255,255,0.9)"}]])

(def modal
  [:.modal-container
   {:margin-top :60px}])
