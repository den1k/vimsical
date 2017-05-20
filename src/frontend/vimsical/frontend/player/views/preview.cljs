(ns vimsical.frontend.player.views.preview
  (:require [vimsical.frontend.views.splits :as splits]
            [vimsical.frontend.player.views.elems :as elems]
            [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [re-com.core :as re-com]
            [vimsical.frontend.util.dom :as util.dom :refer-macros [e> e->]]
            [vimsical.frontend.util.re-frame :refer [<sub]]
            [vimsical.frontend.live-preview.views :refer [live-preview]]
            [vimsical.frontend.vcs.subs :as vcs.subs]
            [vimsical.frontend.timeline.subs :as timeline.subs]
            [vimsical.frontend.player.subs :as subs]
            [vimsical.frontend.player.views.timeline :refer [timeline]]
            [vimsical.frontend.player.handlers :as handlers]
            [vimsical.common.util.core :as util]))

(defn central-play-button []
  (let [unset? (<sub [::subs/playback-unset?])]
    (when unset?
      [:div.play-button-overlay.jc.ac
       {:on-click (e> (re-frame/dispatch [::handlers/play]))}
       [elems/play-button]])))

(defn play-pause []
  (let [playing? (<sub [::timeline.subs/playing?])]
    [:svg.play-pause
     {:view-box "0 0 100 100"
      :on-click (e> (re-frame/dispatch [(if playing? ::handlers/pause ::handlers/play)]))}
     (if-not playing?
       [elems/play-symbol
        {:origin       [50 50]
         :height       100
         :stroke-width 20}]
       [elems/pause-symbol
        {:origin        [50 50]
         :height        100
         :bar-width     30
         :gap-width     60
         :border-radius 10}])]))

(defn time-or-speed-control []
  (let [show-speed? (reagent/atom false)
        speed-range (reagent/atom [1.0 1.5 1.75 2 2.25 2.5])]
    (fn []
      (let [time (util/time-ms->fmt-time (<sub [::timeline.subs/playhead]))]
        [:div.time-or-speed-control.ac
         {:on-mouse-enter (e> (reset! show-speed? true))
          :on-mouse-out   (e> (reset! show-speed? false))}
         (if-not @show-speed?
           [:div.time time]
           [re-com/popover-tooltip
            :label "speed control"
            :position :above-left
            :showing? show-speed?
            :anchor [:div.speed-control
                     {:on-click (e> (swap! speed-range util/rotate))}
                     (str (first @speed-range) "x")]])]))))

(defn preview-container []
  (let [liked    (reagent/atom false)
        playing? (reagent/atom false)]
    (fn []
      (let [branch (<sub [::vcs.subs/branch])]
        [:div.preview-panel.jsb.dc
         [:div.bar.social
          [re-com/h-box
           :gap "40px"
           :children [[re-com/md-icon-button
                       :md-icon-name (if-not @liked "zmdi-favorite-outline" "zmdi-favorite")
                       :on-click (e> (swap! liked not)) :class "favorite"]
                      [re-com/md-icon-button
                       :md-icon-name "zmdi-share" :tooltip "share" :class "share"]
                      [re-com/md-icon-button
                       :md-icon-name "zmdi-time" :tooltip "watch later" :class "watch-later"]]]
          [:div.edit
           "Edit on Vimsical"]]
         [:div.preview-container.f1
          [central-play-button]
          [live-preview {:branch branch}]]
         [re-com/h-box
          :class "bar timeline-container"
          :justify :center
          :align :center
          :gap "18px"
          :children [[play-pause]
                     [timeline]
                     [time-or-speed-control]]]]))))
