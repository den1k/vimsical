(ns vimsical.frontend.landing.style
  (:require [garden.stylesheet :refer [at-media]]))

(def landing
  [:.landing
   {:min-width :960px
    :max-width (str (* 1.5 960) "px")}
   [:.container
    ["> div"
     {:margin-top :200px}]
    [:.codecasts-easy-stmt
     [:.codecasts
      {:font-weight :500}]
     {:font-weight    :200
      :font-size      :80px
      :text-align     :center
      :margin         "180px 0 100px"
      :letter-spacing :.004em}]
    [:.waitlist
     {:margin-top :1em}
     [:.form
      [:.email
       {:height :50px
        :border "1px solid hsl(0, 0%, 93%)"}]
      [:.button
       {:margin-left :20px
        :height      :50px}]]
     [:.result
      {:font-size   :1rem
       :margin-top  :20px
       :line-height :20px
       :text-align  :center}]]
    [:.video-and-waitlist
     {:padding "0 40px"}
     [:.demo-video-wrapper
      {:flex       :0.7
       :height     :100%
       :box-shadow "0 5px 30px hsla(0,0%,0%,0.3)"}
      [:video.demo-video
       {:width   :100%
        :display :block}]]
     [:.text-and-waitlist
      {:flex            :0.3
       :justify-content :space-around
       :margin-left     :60px}
      [:.text
       {:font-size      :20px
        :letter-spacing :.004em}
       [:.just-code
        {:font-weight :bold
         :font-size   :50px}]
       [:.auto-rec
        {:margin-top :1em}]]
      [:.top-waitlist
       [:.join-prompt
        {:font-size      :25px
         :margin-top     :1rem
         :letter-spacing :.02em
         :font-weight    :400}]]]]
    [:.community-codecasts-stmt
     [:.community-created
      {:font-weight :500}]
     {:font-weight :200
      :font-size   :50px
      :text-align  :center}]
    [:.platform
     [:.text
      {:flex    :0.4
       :padding "0 20px"}
      [:.title
       {:font-size :50px}]
      [:.desc
       {:margin-top  :20px
        :font-weight :300
        :font-size   :24px}]]
     [:.img-wrapper
      {:height     :100%
       :display    :flex
       :flex       :0.6
       :overflow   :hidden
       :box-shadow "0 5px 15px hsla(0,0%,0%,0.3)"}
      [:.img
       {:width :152%}]]]
    (at-media
     {:screen    true
      :min-width :1100px}
     [:.player
      {:box-shadow ["0 10px 30px hsla(0,0%,0%,0.3)"]}])
    [:.player
     {:margin  "0 auto"
      :width   :960px
      :padding "30px 40px"}
     [:&:after
      {:margin-bottom :-70px
       :content       "''"
       :width         :1020px
       :height        :150px
       :background    "linear-gradient(to bottom,
                        hsla(0,0%,100%,0) 10%,
                        white 70%,
                        white 100%)"}]
     [:.text
      [:.title
       {:font-size :50px}]
      [:.desc
       {:margin-top  :20px
        :font-size   :24px
        :font-weight :300}
       [:&.summary
        {:margin-top :30px}]]]
     [:.img
      {:margin-top :25px
       :width      :100%
       :border     "1px solid hsla(0, 0%, 0%, 0.1)"}]]
    [:.bottom-waitlist
     {:margin-top    :80px
      :margin-bottom :200px}
     [:.join-prompt
      {:font-size      :60px
       :letter-spacing :.02em
       :font-weight    :400}]
     [:.waitlist
      {:margin-top :1.5rem}]]
    [:.footer-logo
     {:margin "15px auto"}
     [:.vimsical-logo
      {:height :45px
       :width  :45px}]
     [:.vimsical-type
      {:width       :140px
       :margin-left :23px
       :fill        :black}]]]])