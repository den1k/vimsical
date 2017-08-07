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
            [vimsical.frontend.vcs.subs :as vcs.subs]
            [vimsical.frontend.live-preview.handlers :as live-preview.handlers]
            [vimsical.frontend.live-preview.views :as live-preview.views]
            [vimsical.frontend.player.views.player :as player]
            [vimsical.frontend.vims.handlers :as vims.handlers]
            [re-frame.core :as re-frame]
            [re-com.core :as re-com]
            [vimsical.user :as user]
            [vimsical.vims :as vims]
            [vimsical.vcs.branch :as vcs.branch]
            [vimsical.frontend.landing.handlers :as handlers]
            [vimsical.frontend.router.routes :as router.routes]
            [vimsical.frontend.styles.color :refer [colors]]
            [vimsical.frontend.config :as config]))

(def on-prod? (not config/debug?))

(def vims-kw->info
  (->>
   {:bezier         {:vims-uid     nil
                     :title        "Bézier Curve"
                     :author       {::user/first-name "Kynd"
                                    ::user/twitter    "https://twitter.com/kyndinfo"}
                     :original-src "https://codepen.io/kynd/pen/bREZXv"
                     :img-src      "https://www.dropbox.com/s/mi70cypdot7bld5/Screenshot%202017-06-24%2010.41.39.png?dl=1"}

    :fireworks      {:vims-uid     nil
                     :title        "Anime.js Fireworks"
                     :author       {::user/first-name "Julian" ::user/last-name "Garnier"
                                    ::user/twitter    "https://twitter.com/juliangarnier"}
                     :original-src "https://codepen.io/juliangarnier/pen/gmOwJX"
                     :img-src      "https://www.dropbox.com/s/b9665muqf4kbkhk/Screenshot%202017-06-24%2010.33.03.png?dl=1"}

    :strandbeast    {:vims-uid      (uuid "5956e2fe-f747-4efb-86a6-20c39d0a38b1")
                     :vims-uid-prod (uuid "5954f930-a46a-437a-8307-936671d30465")
                     :title         "The Mighty Strandbeest"
                     :author        {::user/first-name "Brandel" ::user/last-name "Zachernuk"
                                     ::user/twitter    "https://twitter.com/zachernuk"}
                     :original-src  "https://codepen.io/zachernuk/pen/RRLxLR"
                     :img-src       "https://www.dropbox.com/s/jmgq7ghrnxhzp0y/Screenshot%202017-06-24%2019.19.24.png?dl=1"}
    :trail          {:vims-uid         nil
                     :title            "Trail"
                     :video-poster-src "/video/explore-still.png"
                     :video-src        "/video/explore.m4v"
                     :author           {::user/first-name "Hakim" ::user/middle-name "El" ::user/last-name "Hattab"
                                        ::user/twitter    "https://twitter.com/hakimel"}
                     :original-src     "https://codepen.io/hakimel/pen/KanIi"}
    :hello-vimsical {:vims-uid-prod    (uuid "5969777d-8dae-48a6-8b6c-838928b59382")
                     :vims-uid         (uuid "5969743d-3ab1-49e9-b95f-894190369986")
                     :player-opts      {:autoplay-from (util/time-units->ms 1 10)}
                     :player-opts-prod {:autoplay-from (util/time-units->ms 5 36)}
                     :title            "Montserrat"
                     :explore?         true
                     :author           {::user/first-name "Claire" ::user/last-name "Larsen"
                                        ::user/twitter    "https://codepen.io/ClaireLarsen/"}
                     :original-src     "https://codepen.io/ClaireLarsen/pen/XmVyVX/"}
    :tree           {:vims-uid         nil
                     :title            "Fractal Tree (L-System)"
                     :video-poster-src "/video/create-still.png"
                     :video-src        "/video/create.m4v"
                     :author           {::user/first-name "Patrick" ::user/last-name "Stillhart"
                                        ::user/website    "https://stillh.art/"}
                     :original-src     "https://codepen.io/arcs/pen/mEdqQX/"}
    :joy-division   {:vims-uid     nil
                     :title        "Interactive Joy Division"
                     :author       {::user/first-name "Mark" ::user/last-name "Benzan"
                                    ::user/twitter    "https://twitter.com/clawtros"}
                     :original-src "https://codepen.io/clawtros/pen/YWgmRR"
                     :img-src      "https://www.dropbox.com/s/rxivavc1aw18bbk/Screenshot%202017-06-24%2020.51.57.png?dl=1"}
    :test           {:vims-uid     (uuid "5956de33-b46c-48dc-83d5-ea60eeef7d83")
                     :title        "Test test test"
                     :author       {::user/first-name "Test" ::user/last-name "Best"
                                    ::user/twitter    "https://twitter.com/clawtros"}
                     :original-src "https://codepen.io/clawtros/pen/YWgmRR"
                     :img-src      "https://www.dropbox.com/s/rxivavc1aw18bbk/Screenshot%202017-06-24%2020.51.57.png?dl=1"}}
   (util/map-vals
    (fn [{:keys [vims-uid vims-uid-prod] :as info}]
      (cond-> info
        on-prod? (assoc :vims-uid vims-uid-prod))))))


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

(defn credit [{:keys [title author original-src vims-uid explore?] :as vims-info}]
  ;; TODO add links to author's sites
  [:div.credit
   "Adapted from " [:span.title
                    {:on-click (e> (util.dom/open original-src))}
                    title] " by "
   [:span.author
    {:on-click (e> (util.dom/open (or (::user/twitter author) (::user/website author))))}
    (user/full-name author)]
   (when explore?
     [:span
      " ∙ "
      [:span.explore
       {:on-click (e> (util.dom/open (router.routes/vims-uri {:db/uid vims-uid})))}
       "explore"]])])

(defn credit-wrapper [vims-info child {:keys [above?]}]
  (let [credit-view [credit vims-info]]
    [:div.credit-wrapper
     (when above? credit-view)
     child
     (when-not above? credit-view)]))

;;
;; Vims Preview
;;

(defn- vims-preview [{:keys [class vims-title-kw] :as opts}]
  (let [{:keys [img-src vims-uid] :as vims-info} (get vims-kw->info vims-title-kw)
        lp-opts {:ui-key vims-title-kw :static? true :vims {:db/uid vims-uid}}]
    (when vims-uid (re-frame/dispatch [::vims.handlers/vims vims-uid]))
    (fn []
      (when (<sub [::vcs.subs/snapshots {:db/uid vims-uid}])
        [ui.views/visibility
         {:range-pred
          (fn [ratio]
            (or #_(<= 0 ratio 0.2)
             (<= 0.5 ratio 1)))
          :on-visibility-change
          (fn [visible?]
            (when vims-uid
              (re-frame/dispatch
               [(if visible? ::live-preview.handlers/defreeze
                             ::live-preview.handlers/freeze) lp-opts])))}
         [:div.vims-preview
          {:class class}
          (if vims-uid
            [live-preview.views/live-preview lp-opts]
            [:img.live-preview          ;; temp
             {:src img-src}])]]))))

(defn vims-preview-section [{:keys [class vims-title-kw] :as opts} child]
  (let [{:keys [img-src vims-uid] :as vims-info} (get vims-kw->info vims-title-kw)
        lp-opts {:ui-key vims-title-kw :static? true :vims {:db/uid vims-uid}}]
    (when vims-uid (re-frame/dispatch [::vims.handlers/vims vims-uid]))
    (fn []
      (when (<sub [::vcs.subs/snapshots {:db/uid vims-uid}])
        (re-frame.loggers/console :log :snap (<sub [::vcs.subs/snapshots {:db/uid vims-uid}]))
        [ui.views/visibility
         {:range-pred
          (fn [ratio]
            (or #_(<= 0 ratio 0.2)
             (<= 0.5 ratio 1)))
          :on-visibility-change
          (fn [visible?]
            (when vims-uid
              (re-frame/dispatch
               [(if visible? ::live-preview.handlers/defreeze
                             ::live-preview.handlers/freeze) lp-opts])))}
         [:div.section.vims-preview-section
          {:class class}
          child
          [:div.vims-preview
           (if vims-uid
             [live-preview.views/live-preview
              lp-opts]
             [:img.live-preview         ;; temp
              {:src img-src}])]]]))))

;;
;; Video Player
;;

(defn video-player [{:keys [class loop? vims-kw]
                     :or   {loop? true}}]
  (let [node          (interop/ratom nil)
        {:keys [video-src video-poster-src]} (get vims-kw->info vims-kw)
        on-vis-change (fn [visible?]
                        (when-not (<sub [::ui.subs/on-mobile?])
                          (if visible?
                            (doto @node
                              (aset "currentTime" 0)
                              (.play))
                            (.pause @node))))]
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
         :poster   video-poster-src
         :src      video-src}]])))

(defn video-player-wrapper [{:keys [class vims-kw] :as opts}]
  [:div.video-wrapper
   [credit-wrapper
    (get vims-kw->info vims-kw)
    [video-player opts]]])

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
  [:div.page-header-section.jc.section
   [:div.sub-section.aifs
    [:div.vimsical-stmt.dc.jc
     [:h1.header.vimsical "Vimsical"]
     [:h2.subheader
      "Your learning playground"]
     [:div.join
      {:on-click (e> (scroll-to-waitlist))}
      "Join our Journey"]]

    (let [{:keys [img-src] :as vims-info} (:strandbeast vims-kw->info)]
      [:div.preview-wrapper
       [credit-wrapper vims-info
        [vims-preview {:vims-title-kw :strandbeast}]]])]])

(defn create-section []
  [:div.create-section.section
   [:div.sub-section.aife
    [:h2.header "Create"]
    [:h3.subheader
     ;"You do the work, we make the tutorial. Automatically.
     "Vimsical turns your coding process into an interactive tutorial. Automatically."]
    [ui.views/visibility {}
     [video-player-wrapper
      {:class   "create-video"
       :vims-kw :tree}]]]])

(defn explore-section []
  [:div.explore-section.section
   [:div.sub-section
    [:h2.header "Explore"]
    [:h3.subheader
     "See how projects come together. And make edits with one click."]
    [ui.views/visibility {}
     [video-player-wrapper
      {:class   "explore-video"
       :vims-kw :trail}]]]])

(defn mission-section []
  [:div.mission-section.dc.ac.section
   [ui.views/visibility
    {:once? true}
    ;{}
    #_[:div.logo-and-slogan.ac.jsb
       [icons/logo-and-type]
       [:div.stretcher]
       [:h2.learnable "make it learnable."]]]
   [:p.stmt
    "Our mission is to nurture understanding, accelerate learning and ease teaching"
    [:br]
    "by providing tools to record, share and explore our process."]])

(defn player-load-vims [{vims-uid :db/uid :as vims} player-opts]
  (re-frame/dispatch [::vims.handlers/load-vims vims-uid :player-vims])
  (fn []
    (when-let [vims (<sub [::vims.subs/vcs-vims vims-uid])]
      [player/player
       (cond-> {:vims       {:db/uid vims-uid}
                :show-info? false
                :read-only? true}
         true (merge player-opts)
         (not (<sub [::ui.subs/on-mobile?])) (assoc :orientation :landscape))])))


(def coder-emojis-str "\uD83D\uDC69\u200D\uD83D\uDCBB \uD83D\uDC68\u200D\uD83D\uDCBB")
(defn coder-emojis [] [:span.coder-emojis coder-emojis-str])

(defn player-section []
  [:div.player-section.section
   [ui.views/visibility {}
    [:div.dc.ac.sub-section
     ;[:h2.header "Create. Watch. Explore."]
     [:h2.header "You do the work,\nVimsical makes the tutorial."]
     (when-not (<sub [::ui.subs/on-mobile?])
       [re-com/h-box
        :class "try-cta-box"
        :gap "4px"
        :children [[:span.try-cta.ac "It's automagic! Try changing the code"]
                   [:span.pointer-wrapper [:span.pointer "☟"]]
                   [coder-emojis]]])
     ;; todo credit
     [:div.player-wrapper
      (let [{:keys [vims-uid player-opts player-opts-prod]} (:hello-vimsical vims-kw->info)]
        [player-load-vims
         {:db/uid vims-uid}
         (if on-prod? player-opts-prod player-opts)])]
     #_[:p.sub-stmt
        "Embed"
        [:span.bold " Player "]
        "and bring powerful learning experiences"
        [:br]
        "to your website, blog and classroom."]
     [credit (:hello-vimsical vims-kw->info)]
     ]]])

(defn waitlist []
  (let [state (reagent/atom {:success nil :error nil :email ""})]
    (fn []
      (let [{:keys [success error email]} @state]
        [:div.waitlist
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
                    "Thanks! We'll reach out to you soon."]
           error [:div.result.error
                  "Oh no!"
                  [:br] "Something went wrong. Please try again."])]))))

(defn product-stmts []
  [:div.product-stmts-section.section
   [re-com/v-box
    :class "product-stmts sub-section"
    :gap "60px"
    :children
    [[:div.product-stmt-wrapper.ac
      [icons/deer {:class "stmt-icon deer"}]
      [:div.product-stmt.dc.jc
       [:div.title "Code and Create."]
       [:div.stmt "Vimsical turns your coding process into an interactive tutorial. Automatically."]]]
     [:div.product-stmt-wrapper.ac
      [icons/monkey {:class "stmt-icon monkey"}]
      [:div.product-stmt.dc.jc
       [:div.title "Beyond Video: \uD83D\uDCAF Interactive."]
       [:div.stmt "See how your favorite projects come together. And make edits with one click."]]]
     [:div.product-stmt-wrapper.ac
      [icons/crane {:class "stmt-icon crane"}]
      [:div.product-stmt.dc.jc
       [:div.title "Explore Anywhere, Teach Everywhere."]
       [:div.stmt "Vimsical's Embedded Player brings powerful learning experiences to your website, company and classroom."]]]]]])

(defn contact-us []
  [:span.contact
   {:on-click (e> (util.dom/open "mailto:dennis@vimsical.com?subject=Vimsical%20Demo"))}
   "Contact us"])

;;
;; * Component
;;

(defn landing []
  [:div.landing.asc.dc.ac.ais
   ;[page-header]
   [player-section]
   [product-stmts]

   ;[create-section]

   ;[explore-section]


   ;; Todo Education section?
   #_[vims-preview-section {:vims-title-kw :trail :class "teach-by-doing"}
      [:div.sub-stmt
       "Teach, by doing."]]



   #_[vims-preview-section {:vims-title-kw :fireworks :class "create-watch-explore"}
      [:div.sub-stmt
       "Create. Watch. Explore."]]

   #_[vims-preview-section {:vims-title-kw :test :class "create-watch-explore"}
      [:div.sub-stmt
       "Create. Watch. Explore."]]

   [:div.bottom-waitlist.dc.ac.section
    [:div.sub-section.aic
     [:h2.join "Interested in how Vimsical could help you or your company?"]
     [re-com/h-box
      :class "get-demo-box"
      :gap "30px"
      :align :center
      :style {:flex-flow :wrap}
      :children [[:span.get-demo "Get a demo:"] [waitlist]]]]]
   ;[mission-section]
   (if (<sub [::ui.subs/on-mobile?])
     [:footer.footer.jsb.ac.dc
      [re-com/h-box
       :class "built-contact mobile"
       :justify :around
       :children
       [[:span "Built in NYC"]
        [contact-us]]]
      [icons/logo-and-type {:class :footer-logo}]]
     [:footer.footer.jsb.ac
      [:div.footer-box [:span "Copyright © 2017 Vimsical Inc."]]
      [icons/logo-and-type
       {:class :footer-logo}]
      [re-com/h-box
       :class "built-contact footer-box"
       :justify :end
       :gap "10px"
       :children
       [[:span "Built in NYC"]
        [contact-us]]]])])