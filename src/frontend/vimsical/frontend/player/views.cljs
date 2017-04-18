(ns vimsical.frontend.player.views
  (:require [re-com.core :as re-com]
            [vimsical.frontend.live-preview.views :refer [live-preview]]
            [vimsical.frontend.code-editor.views :refer [code-editor]]
            [vimsical.frontend.views.splits :as splits]
            [vimsical.frontend.views.shapes :as shapes]
            [vimsical.frontend.views.user :as user]
            [vimsical.frontend.util.dom :refer-macros [e-handler e->]]
            [reagent.core :as reagent]
            [vimsical.frontend.views.icons :as icons]))

(defn play-symbol [opts]
  [shapes/triangle
   (merge {:class           "play-symbol"
           :origin          [50 50]
           :height          50
           :stroke-linejoin "round"
           :stroke-width    8
           :rotate          90}
          opts)])

;; share with vcr
(defn pause-symbol
  "Pause Symbol to be used within SVG. Uses rect instead of line to for border-
  radius control."
  [{:keys [origin bar-width gap-width height border-radius class]
    :or   {origin        [50 50]
           gap-width     20
           border-radius 3
           class         "pause-symbol"}
    :as   opts}]
  (let [[origin-x origin-y] origin
        y               (- origin-y (/ height 2))
        offset-x-origin (- origin-x (/ bar-width 2))
        half-gap        (/ gap-width 2)
        attrs           {:y  y :width bar-width :height height
                         :rx border-radius :ry border-radius}]
    [:g {:class class}
     [:rect (merge {:x (- offset-x-origin half-gap)} attrs)]
     [:rect (merge {:x (+ offset-x-origin half-gap)} attrs)]]))

(defn play-button []
  [:svg.play-button
   {:view-box "0 0 100 100"}
   [:circle.button-circle
    {:r  50
     :cx 50
     :cy 50}]
   [play-symbol
    {:origin       [55 50]
     :height       50
     :stroke-width 8}]])

(defn preview-container []
  (let [liked    (reagent/atom false)
        playing? (reagent/atom false)]
    (fn []
      [:div.preview-panel
       [:div.bar.social
        [:div.social-buttons
         [re-com/md-icon-button
          :md-icon-name (if-not @liked "zmdi-favorite-outline" "zmdi-favorite")
          :on-click (e-handler (swap! liked not)) :class "favorite"]
         [re-com/md-icon-button
          :md-icon-name "zmdi-share" :tooltip "share" :class "share"]
         [re-com/md-icon-button
          :md-icon-name "zmdi-time" :tooltip "watch later" :class "watch-later"]]
        [:div.edit
         "Edit on Vimsical"]]
       [:div.preview-container
        [:div.play-button-overlay
         [play-button]]
        [live-preview]]
       [:div.bar.timeline-container
        [:svg.play-pause
         {:view-box "0 0 100 100"
          :on-click (e-handler (swap! playing? not))}
         (if-not @playing?
           [play-symbol
            {:origin       [50 50]
             :height       100
             :stroke-width 20}]
           [pause-symbol
            {:origin        [50 50]
             :height        100
             :bar-width     30
             :gap-width     60
             :border-radius 10}])]
        (let [dur 1e2]
          [:div.timeline
           [:div.progress.left]
           [:div.progress.passed]
           [:div.playhead]
           [:div.skimhead]])
        [:div.speed-control
         "1.5x"]]]))
  )

;; temp
(def person-url
  "https://encrypted-tbn1.gstatic.com/images?q=tbn:ANd9GcRw8FAuvFJSJIcHOPrGy_yhrpuQTDJOKQEJbggJneasIvQT1YHvY-cBxQ")

(defn info-and-editor-container []
  [:div.info-and-editor-panel
   [:div.info
    [:div.header
     [user/avatar
      {:img-url person-url
       :size    "50px"}]
     [:div.title-and-creator
      [:div.title "Pinterest CSS Layout"]
      [:div.creator "Jane Blacksmith"]]]
    [:div.desc
     "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Praesent ut leo sit amet mauris tempus luctus. Nullam placerat metus leo, et faucibus odio mattis a. Nulla ac quam magna. Aliquam vestibulum enim dolor, sit amet volutpat arcu egestas nec. Nullam bibendum volutpat ultricies. Nunc nec vehicula ante. Nam est massa, sollicitudin sit amet felis a, vestibulum viverra nunc. Mauris et tristique turpis, non eleifend nulla. Mauris commodo blandit justo id imperdiet. Phasellus varius eget turpis interdum sodales. Vestibulum ac efficitur metus."]
    ]
   [code-editor {:file-type :html
                 :compact?  true}]
   [:div.logo-and-file-type.bar
    [icons/logo-and-type]
    ;; todo dynamic
    [:div.active-file-type.css
     ;; todo dynamic
     "HTML"]]])

(defn player []
  [:div.vimsical-frontend-player
   [splits/n-h-split
    :panels
    [[preview-container]
     [info-and-editor-container]]
    :splitter-child [re-com/line]
    :initial-split 70
    :margin "0"]])