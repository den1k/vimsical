(ns vimsical.frontend.auth.style)

(def auth
  [:.auth
   [:&.login
    {:width :250px}
    [:.form
     {
      ;:display         :flex
      ;:flex-direction  :column
      ;:justify-content :space-between
      :height :200px}
     [:.cookie-and-forgot-pass
      [:.cookie
       [:.checkbox
        {:margin "0 5px 0 0"}]]]
     [:.login-button
      {:height :50px}]]
    [:.closed-beta
     {:margin-top :40px}
     [:.title
      {:font-size      :20px
       :letter-spacing :0.7px}]
     [:.stmt
      {:margin-top  :10px
       :line-height :22px}]]]
   [:&.logout
    {:cursor :pointer}]
   [:&.signup
    {:align-self :center
     :position   :relative
     :margin-top :100px}
    [:.beta-signup
     {:font-size   :60px
      :font-weight :600}]
    #_[:.oauth
       {:display         :flex
        :justify-content :space-between
        :align-self      :stretch}]
    [:.form
     {:width      :350px
      :margin-top :60px
      :min-height :270px}
     [:.first-last
      [:.first :.last
       {:width :48%}]]
     [:.signup-button
      {:height     :50px
       :width      :130px
       :margin-top :10px}]]]])
