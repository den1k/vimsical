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
            [vimsical.frontend.ui.subs :as ui.subs]
            [vimsical.frontend.vims.subs :as vims.subs]
            [vimsical.frontend.live-preview.views :as live-preview.views]
            [vimsical.frontend.player.views.player :as player]
            [vimsical.frontend.vims.handlers :as vims.handlers]
            [re-frame.core :as re-frame]
            [re-com.core :as re-com]
            [vimsical.frontend.landing.handlers :as handlers]
            [vimsical.frontend.router.routes :as router.routes]))

(def landing-vims->uid
  {:bug         (uuid "5947d83b-602f-4c0a-83e5-88c03281c0a0")
   :hello-world (uuid "594b29e0-f21c-4753-9836-d9d45b4b4809")
   :shines      (uuid "594b3b7e-8b61-4d4f-a36d-99282559c7c3")
   :owl         (uuid "594b509c-c801-4abf-9e6d-5f03aed1771a")
   :tree        (uuid "594c137a-c665-4eeb-8b25-e183fd000c80")
   :tree-light  (uuid "594d58c7-6268-4ae2-af3a-fd48edf23d0d")})

(defn load-landing-vims []
  (doseq [[_ uid] landing-vims->uid]
    (re-frame/dispatch [::vims.handlers/load-vims uid])))

(defn link-for-vims [title-kw]
  (-> {:db/uid (get landing-vims->uid title-kw)}
      (router.routes/vims-uri)))

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

;;
;; Wrappers
;;

(defn credit [vims-title-kw title author]
  ;; TODO add links to author's sites
  [:div.credit
   "Adapted from " [:span.title title] " by " [:span.author author]
   " ∙ "
   [:span.explore
    {:on-click (e> (util.dom/open (link-for-vims vims-title-kw)))}
    "explore"]])

(defn credit-wrapper [vims-title-kw title author content {:keys [above?]}]
  (let [credit-view [credit vims-title-kw title author]]
    [:div.credit-wrapper
     (when above? credit-view)
     content
     (when-not above? credit-view)]))

;;
;; Vims Preview
;;

(defn- vims-preview [{:keys [ui-key vims-uid scroll-skim? from-snapshot?]}]
  (let [scroll-skim-ratio (when scroll-skim? (interop/ratom 0))]
    (fn []
      (when-let [vims (<sub [::vims.subs/vcs-vims vims-uid])]
        (let [view [live-preview.views/live-preview
                    {:ui-key         ui-key
                     :static?        (not scroll-skim?)
                     :from-snapshot? from-snapshot?
                     :vims           vims}]]
          (if-not scroll-skim?
            view
            (letfn [(dispatch-fn [ratio]
                      (re-frame/dispatch
                       [::handlers/set-vims-preview-throttle vims ratio]))]
              [ui.views/viewport-ratio dispatch-fn true view])))))))

;;
;; Video Player
;;

(defn video-player [{:keys [class loop? src]
                     :or   {loop? true}}]
  (let [node          (interop/ratom nil)
        on-vis-change (fn [visible?]
                        (if visible?
                          (doto @node
                            (aset "currentTime" 0)
                            (.play))
                          (.pause @node)))]
    (fn []
      [ui.views/visibility {:on-visibility-change on-vis-change}
       [:video.video
        {:class    class
         :ref      (fn [vid-node]
                     (reset! node vid-node))
         :controls false
         ;:auto-play false
         ;; necessary to autoplay on iOS
         :muted    true
         ;; necesssary to not enter full-screen mode in iOS
         ;; but seeming not currently supported in react
         ;:plays-inline true
         :loop     loop?
         :preload  "auto"
         :src      src}]])))

;;
;; Scroll Player
;;

(defn scroll-player [{:keys [ui-key vims-uid scroll-skim?]}]
  (let [scroll-skim-ratio (when scroll-skim? (interop/ratom 0))]
    (fn []
      (when-let [vims (<sub [::vims.subs/vcs-vims vims-uid])]
        (let [view [player/player {:vims        vims
                                   :orientation :landscape
                                   :show-info?  false
                                   :read-only?  true
                                   :ui-key      ui-key}]]
          (if-not scroll-skim?
            view
            (letfn [(dispatch-fn [ratio]
                      (re-frame/dispatch
                       [::handlers/set-player-preview vims ratio]))]
              [ui.views/viewport-ratio dispatch-fn true view])))))))

;;
;; Landing Sections
;;


(defn page-header []
  [:div.stmt-wrapper.jsb.ac
   [:div.vimsical-stmt
    [:h1.header.vimsical "Vimsical"]
    [:h2.subheader
     "Your coding playground"]
    [:div.join
     {:on-click (e> (scroll-to-waitlist))}
     "Join our Journey"]]])

(defn create-section []
  [:div.create.section.dc
   [:h2.header "Create"]
   [:h3.subheader
    "Vimsical turns your coding process into an interactive tutorial. Automatically."]
   [:div.video-wrapper
    [credit-wrapper
     :owl
     "Trail"
     "Lee Hamsmith"
     [video-player
      {:class "create-video"
       :src   "video/create.m4v"}]]]])

(defn explore []
  [:div.explore.section.dc
   [:h2.header "Explore"]
   [:h3.subheader
    "See your favorite projects take shape. And make edits with one click."]
   [:div.video-wrapper
    [credit-wrapper
     :owl
     "The Bug"
     "Ana Tudor"
     [video-player
      {:class "explore-video"
       :src   "video/explore.m4v"}]]]])

(defn mission-section []
  [:div.mission-section.dc.ac.section
   [ui.views/visibility
    {:once? true}
    [:div.logo-and-slogan.jsb.ac
     [icons/logo-and-type]
     [:h2.learnable "make it learnable."]]]
   [:p.stmt
    "Our mission is to nurture understanding, accelerate learning and ease teaching"
    [:br]
    "By providing tools to record, share and explore our process."]])

(defn player-section []
  ;; todo skim vims by scrolling
  [:div.player-section.dc.ac.section
   [:h2.header "Player"]
   [:h3.subheader "Take your creations places."]
   [credit-wrapper :owl "Wormhole" "Jack Aniperdo"
    (when-let [vims (<sub [::vims.subs/vcs-vims (:owl landing-vims->uid)])]
      [player/player
       (cond-> {:vims       vims
                :show-info? false
                :read-only? true}
         (not (<sub [::ui.subs/on-mobile?])) (assoc :orientation :landscape))])]
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
                    "We can't wait to see what you'll come up with."]
           error [:div.result.error
                  "Oh no!"
                  [:br] "Something went wrong. Please try again."])]))))

;;
;; * Component
;;

(defn landing []
  (load-landing-vims)
  (fn []
    [:div.landing.asc.dc.ac.ais
     [:div.wrapper
      [page-header]
      [create-section]
      [explore]
      [player-section]                  ;; emoji predictor
      #_[:div.section
         [:h3 "TODO Creations here"]
         ]
      [mission-section]

      #_[:div.section
         [:h3 "TODO Creations here"]]

      [:div.bottom-waitlist.dc.ac.section
       [:h1.join "Join our Journey"]
       [waitlist]]
      #_[icons/logo-and-type
         {:class "footer-logo jc ac"}]]
     #_(when-not (<sub [::ui.subs/on-mobile?])
         [:div.preview-wrap
          (when-let [vims (<sub [::vims.subs/vcs-vims (:tree-light landing-vims->uid)])]
            [live-preview.views/live-preview
             {:ui-key         ::page-header
              :static?        true
              :from-snapshot? true
              :vims           vims}]
            )])]))