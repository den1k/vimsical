(ns vimsical.frontend.landing.views
  (:require [vimsical.frontend.views.icons :as icons]
            [vimsical.frontend.ui.views :as ui.views]
            [vimsical.frontend.util.dom :refer-macros [e>]]
            [goog.net.XhrIo]
            [reagent.core :as reagent]
            [vimsical.common.util.core :as util]
            [re-frame.interop :as interop]
            [vimsical.frontend.util.dom :as util.dom]
            [vimsical.frontend.util.re-frame :refer [<sub]]
            [vimsical.frontend.vims.subs :as vims.subs]
            [vimsical.frontend.live-preview.views :as live-preview.views]
            [vimsical.frontend.player.views.player :as player]
            [vimsical.frontend.vims.handlers :as vims.handlers]
            [re-frame.core :as re-frame]))

(def landing-vims->uid
  {:bug (uuid "5947d83b-602f-4c0a-83e5-88c03281c0a0")})

(defn load-landing-vims []
  (doseq [[_ uid] landing-vims->uid]
    (re-frame/dispatch [::vims.handlers/load-vims uid])))

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

(def ^:private waitlist-signup-id "waitlist")

(defn scroll-to-waitlist []
  (-> (.getElementById js/document waitlist-signup-id)
      (util.dom/scroll-to)))

(defn credit [title author]
  [:div.credit
   "Adapted from " [:span.title title] " by " [:span.author author]])

(defn credit-wrapper [title author content {:keys [above?]}]
  [:div.credit-wrapper
   (when above? [credit title author])
   content
   (when-not above? [credit title author])])

(defn- vims-preview [{:keys [vims-uid]}]
  (when-let [vims (<sub [::vims.subs/vcs-vims vims-uid])]
    [live-preview.views/live-preview
     {:ui-key  ::page-header
      :static? true
      ;:from-snapshot? true
      :vims    vims}]))

(defn page-header []
  [:div.stmt-wrapper.jsb.ac
   [:div.vimsical-stmt
    [:h1.header "Vimsical"]
    [:h2.subheader
     "Your educational playground"
     [:br]
     "To explore and create."]
    [:div.join
     {:on-click (e> (scroll-to-waitlist))}
     "Join our Journey"]]
   [vims-preview {:vims-uid (:bug landing-vims->uid)}]])

(defn create []
  [:div.create.section
   [:h2.header "Create"]
   [:h3.subheader
    "Vimsical turns your coding process into an interactive tutorial. Automatically."]
   [credit-wrapper
    "Trail"
    "Lee Hamsmith"
    [:div.lp-vims-lg]]])

(defn explore []
  [:div.explore.section
   [:h2.header "Explore"]
   [:h3.subheader
    "See your favorite projects take shape. And make edits with one click."]
   [credit-wrapper
    "The Bug"
    "Ana Tudor"
    [:div.lp-vims-lg]]])

(defn mission-section []
  [:div.mission-section.dc.ac.section
   [ui.views/visibility
    {:once? true
     :child
            [:div.logo-and-slogan.jsb.ac
             [icons/logo-and-type]
             [:h2.learnable "make it learnable."]]}]
   [:p.stmt
    "Our mission is to nurture understanding, accelerate learning and ease teaching"
    [:br]
    "By providing tools to record, share and explore our process."]])

(defn player-section []
  ;; todo skim vims by scrolling
  [:div.player-section.dc.ac.section
   [:h2.header "Player"]
   [:h3.subheader "Take your creations places."]
   [credit-wrapper "Wormhole" "Jack Aniperdo"
    (when-let [vims (<sub [::vims.subs/vcs-vims (:bug landing-vims->uid)])]
      [player/player {:vims       vims :orientation :landscape
                      :show-info? false}])]
   [:p.embed-stmt
    "Empower others with an immersive learning experience"
    [:br]
    "Embed"
    [:span.bold " Player "]
    "on your website, Twitter or use it in your classroom."]])

(defn waitlist []
  (let [state (reagent/atom {:success nil :error nil :email ""})]
    (fn []
      (let [{:keys [success error email]} @state]
        [:div.waitlist.section
         {:id waitlist-signup-id}
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
           error [:div.result.error
                  "Oh no!"
                  [:br] "Something went wrong. Please try again."])]))))

;;
;; * Component
;;

(defn landing []
  (load-landing-vims)
  (fn []
    [:div.landing.asc.dc.ac
     [:div.wrapper
      [page-header]
      [create]
      [explore]
      [player-section]
      [:div.section
       [:h3 "TODO Creations here"]]
      [mission-section]

      [:div.section
       [:h3 "TODO Creations here"]]

      [:div.bottom-waitlist.dc.ac
       [:h1.join "Join our Journey"]
       [waitlist]]
      [icons/logo-and-type
       {:class "footer-logo jc ac"}]]]))