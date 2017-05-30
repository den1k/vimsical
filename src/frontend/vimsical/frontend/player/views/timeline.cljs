(ns vimsical.frontend.player.views.timeline
  (:require [vimsical.frontend.timeline.subs :as timeline.subs]
            [vimsical.frontend.util.dom :as util.dom :refer-macros [e> e->]]
            [vimsical.frontend.util.re-frame :refer [<sub]]
            [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [vimsical.frontend.vcr.handlers :as vcr.handlers]
            [vimsical.frontend.player.handlers :as handlers]
            [vimsical.frontend.player.views.elems :as elems]
            [vimsical.common.util.core :as util]
            [re-com.core :as re-com]))

(defn time->pct [duration playhead]
  (-> playhead (/ duration) (* 100)))

(defn perc->time [duration perc]
  (-> perc (/ 100) (* duration)))

(defn e->time [c e dur]
  (->> (util.dom/rel-component-mouse-coords-percs c e)
       first
       (perc->time dur)))

(defn handlers [c vims dur]
  {:on-mouse-enter (e> (re-frame/dispatch [::handlers/on-mouse-enter vims (e->time c e dur)]))
   :on-mouse-move  (e> (re-frame/dispatch [::handlers/on-mouse-move vims (e->time c e dur)]))
   :on-mouse-leave (e> (re-frame/dispatch [::handlers/on-mouse-leave vims]))
   :on-click       (e> (re-frame/dispatch [::handlers/on-click vims (e->time c e dur)]))
   :on-touch-move  (e> (re-frame/dispatch [::handlers/on-click vims (e->time c (util.dom/first-touch->e e) dur)]))})


(defn play-pause [{:keys [vims]}]
  (let [playing? (<sub [::timeline.subs/playing? vims])]
    [:svg.play-pause
     {:view-box "0 0 100 100"
      :on-click (e> (re-frame/dispatch
                     [(if playing? ::vcr.handlers/pause ::vcr.handlers/play) vims]))}
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

(defn time-or-speed-control [{:keys [vims]}]
  (let [show-speed? (reagent/atom false)
        speed-range (reagent/atom [1.0 1.5 1.75 2 2.25 2.5])]
    (fn [{:keys [vims]}]
      (let [time (util/time-ms->fmt-time (<sub [::timeline.subs/time vims]))]
        [:div.time-or-speed-control.ac.jc
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

(defn timeline []
  (reagent/create-class
   {:render
    (fn [c]
      (let [{:keys [vims]} (reagent/props c)
            dur           (<sub [::timeline.subs/duration vims])
            playhead      (<sub [::timeline.subs/playhead vims])
            skimhead      (<sub [::timeline.subs/skimhead vims])
            playhead-perc (str (time->pct dur playhead) "%")
            skimhead-perc (when skimhead (str (time->pct dur skimhead) "%"))]
        [:div.timeline.ac.f1
         (handlers c vims dur)
         [:div.time.left]               ; .progress class clashes with bootstrap
         [:div.time.passed
          {:style {:width playhead-perc}}]
         [:div.head
          {:class (if skimhead "skimhead" "playhead")
           :style {:left (or skimhead-perc playhead-perc)}}]]))}))

(defn timeline-bar [{:keys [vims]}]
  [re-com/h-box
   :class "bar timeline-container"
   :justify :center
   :align :center
   :gap "18px"
   :children [[play-pause {:vims vims}]
              [timeline {:vims vims}]
              [time-or-speed-control {:vims vims}]]])
