(ns vimsical.frontend.views.style)

(def user
  [:.avatar
   {:position    :relative
    :flex-shrink 0
    :cursor      :pointer}
   [:.pic
    {:position      :absolute
     :object-fit    :cover
     ;; inherit size of parent (see view)
     :width         :inherit
     :height        :inherit
     :overflow      :hidden
     :border-radius :50%}]
   [:.notifications-indicator
    {:position      :absolute
     :background    :#7ED321
     :border-radius :50%
     :width         :14px
     :height        :15px
     :margin-left   :72%}]])

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
  [user
   icons])
