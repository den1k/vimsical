(ns vimsical.frontend.live-preview.style
  "Used in VCR, Player and VimsList")

(def live-preview
  [:.live-preview
   {:flex    1
    :display :flex}
   [:.iframe
    {:width      :100%
     :border     "none"
     :background "white"}]])