(ns vimsical.frontend.views.style)

(def icons
  [:.logo-and-type {:display     :flex
                    :align-items :center
                    :cursor      :pointer}
   [:.vimsical-logo {:height :36px
                     :width  :36px}]
   [:.vimsical-type {:width       :125px
                     :fill        :#8F9096
                     :margin-left :15px}]])

(def views
  [icons])
