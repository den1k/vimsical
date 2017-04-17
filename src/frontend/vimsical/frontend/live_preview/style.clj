(ns vimsical.frontend.live-preview.style)

(def live-preview
  [:.live-preview
   {:flex    1
    :display :flex}
   [:.iframe
    {:width      :100%
     :border     "none"
     :background "white"}]])