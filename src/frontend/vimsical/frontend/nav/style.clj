(ns vimsical.frontend.nav.style)

(def nav
  [:.nav
   {:border-bottom "solid 2px #eceff3"
    :background    :white
    :display       :flex
    :align-items   :center
    :padding       "8px 25px"}
   [:.logo-and-type {:display     :flex
                     :align-items :center
                     :cursor      :pointer}
    [:.logo {:height :36px
             :width  :36px}]
    [:.type {:width       :125px
             :fill        :#8F9096
             :margin-left :15px}]]])