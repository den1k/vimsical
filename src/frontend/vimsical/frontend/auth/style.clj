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
       :line-height :22px}]]
    ]
   [:&.logout]])