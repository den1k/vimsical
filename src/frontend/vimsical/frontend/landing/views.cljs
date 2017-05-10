(ns vimsical.frontend.landing.views
  (:require [vimsical.frontend.views.icons :as icons]
            [vimsical.frontend.util.dom :refer-macros [e>]]
            [goog.net.XhrIo]
            [reagent.core :as reagent]))

;;
;; * Waitlist Form
;;

(def endpoint "https://di60oevsv1.execute-api.us-west-2.amazonaws.com/prod/handler")

(defn post-data [email]
  (str "email=" (js/encodeURIComponent email)))

(defn handle-success [state _]
  (swap! state assoc :success true))

(defn handle-error [state _]
  (swap! state assoc :error true))

(defn submit! [state]
  (let [email   (:email @state)
        data    (post-data email)
        headers #js {"Content-Type" "application/x-www-form-urlencoded"}]
    (doto (goog.net.XhrIo.)
      (goog.events.listen goog.net.EventType.SUCCESS (partial handle-success state))
      ;; handling as success also, b/c we're getting
      ;; an error even tough the email gets logged
      (goog.events.listen goog.net.EventType.ERROR (partial handle-success state))
      (.send endpoint "POST" data headers))))


(defn waitlist []
  (let [state (reagent/atom {:success nil :error nil :email ""})]
    (fn []
      (let [{:keys [success error email]} @state]
        [:div.waitlist
         [:div.form.ac
          [:input.email {:type        "email"
                         :name        "email"
                         :placeholder "Email address"
                         :on-change   (e> (swap! state assoc :email value))}]
          [:div.button
           {:on-click (fn [_] (submit! state))}
           [:div.btn-content "Sign up"]]]
         (cond
           success [:div.result.success
                    "Thank You!"
                    [:br]
                    "We'll let you know as soon as we're live!"]
           error   [:div.result.error
                    "Oh no!"
                    [:br] "Something went wrong. Please try again."])]))))

;;
;; * Component
;;

(defn landing []
  [:div.landing.asc.dc.ac
   [:div.container
    [:div.codecasts-easy-stmt
     [:span.codecasts
      "Codecasts. "]
     "Finally easy."]
    [:div.video-and-waitlist.f1
     [:div.demo-video-wrapper.ac
      [:video.demo-video
       {:controls  false
        :auto-play true
        ;; necessary to autoplay on iOS
        :muted     true
        ;; necesssary to not enter full-screen mode in iOS
        ;; but seeming not currently supported in react
                                        ;:plays-inline true
        :loop      true
        :preload   "auto"
        :src       "video/watch-and-interact.m4v"}]]
     [:div.text-and-waitlist.dc.ac
      [:div.text
       [:span.just-code
        "Code & Interact."]
       [:br]
       [:div.auto-rec
        "Vimsical automatically records as you speak or type."]]
      [:div.top-waitlist.dc.ac
       [:div.join-prompt
        "Join the waitlist"]
       [waitlist]]]]
    [:div.community-codecasts-stmt
     [:span.community-created
      "Community-Created "]
     "Interactive Codecasts"]
    [:div.platform.ac
     [:div.text
      [:div.title
       "Platform"]
      [:div.desc
       "The web is full of amazing projects made by amazing people."
       [:br]
       [:br]
       "But how did they get from A → B?"
       [:br]
       [:br]
       "Join a community of coders who empower each other by talking "
       [:i "with"]
       " code."]]
     [:div.img-wrapper.dc.jc
      [:img.img
       {:src "img/platform-crop.png"}]]]
    [:div.process-control-stmt]
    [:div.player.dc.ac
     [:div.text
      [:div.title
       "Player"]
      [:div.desc.embed
       "Embed on any website with a simple script tag. Also, we’re adding
       support for Twitter, Medium, Reddit and blogs."]]
     [:img.img
      {:src "img/ep.png"}]
     [:div.text.summary
      [:div.desc
       "Under the hood Vimsical is powered by a full-fledged version control
        system that records every change you make. "
       "And more. Stay tuned for integrations to watch and record in your
        favorite editor."]]]
    [:div.bottom-waitlist.dc.ac
     [:div.join-prompt
      "Join the waitlist"]
     [waitlist]]
    [icons/logo-and-type
     {:class "footer-logo jc ac"}]]])


