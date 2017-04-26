(ns vimsical.frontend.user.style)

(def avatar
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

(def user
  [avatar
   [:.user
    [:.name
     {:display     :flex
      :margin-left :10px
      :cursor      :pointer}
     [:.last-name
      {:margin-left :5px}]]]])
