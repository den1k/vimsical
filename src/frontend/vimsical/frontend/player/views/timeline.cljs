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
            [vimsical.frontend.ui.subs :as ui.subs]
            [vimsical.frontend.user.handlers :as user.handlers]
            [vimsical.frontend.user.subs :as user.subs]
            [re-com.core :as re-com]))

(defn time->pct [duration playhead]
  (util/clamp (-> playhead (/ duration) (* 100)) 0 100))

(defn perc->time [duration perc]
  (-> perc (/ 100) (* duration)))

(defn e->time [c e dur]
  (->> (util.dom/rel-component-mouse-coords-percs c e)
       first
       (perc->time dur)))

(def handlers
  (let [last-touch-time (atom 0)]
    (fn [c vims dur]
      {:on-mouse-enter
       (e> (re-frame/dispatch [::handlers/on-mouse-enter vims (e->time c e dur)]))
       :on-mouse-move
       (e> (re-frame/dispatch [::handlers/on-mouse-move vims (e->time c e dur)]))
       :on-mouse-leave
       (e> (re-frame/dispatch [::handlers/on-mouse-leave vims]))
       :on-click
       (e> (re-frame/dispatch [::handlers/on-click vims (e->time c e dur)]))

       :on-touch-move
       (e> (let [time (e->time c (util.dom/first-touch->e e) dur)]
             (reset! last-touch-time time)
             (re-frame/dispatch [::handlers/on-mouse-move vims time])))
       :on-touch-end
       (e> (re-frame/dispatch
            [::handlers/on-click vims (perc->time dur @last-touch-time)])
           (re-frame/dispatch [::handlers/on-mouse-leave vims]))})))


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

(defn speed-control [{:keys [vims]}]
  (let [{:as            settings
         :settings/keys [playback-speed]
         :or            {playback-speed 1}} (<sub [::user.subs/settings])]
    [:div.speed-control
     {:on-click (e> (re-frame/dispatch
                     [::user.handlers/playback-speed settings :inc]))}
     (str playback-speed "x")]))

(defn time-or-speed-control []
  (let [show-speed? (reagent/atom false)
        on-mobile?  (<sub [::ui.subs/on-mobile?])]
    (fn [{:keys [vims] :as opts}]
      (let [time (util/time-ms->fmt-time (<sub [::timeline.subs/time vims]))]
        [:div.time-or-speed-control.ac.jc
         {:on-mouse-over  (e> (reset! show-speed? true))
          :on-mouse-leave (e> (reset! show-speed? false))}
         (if (or on-mobile? (not @show-speed?))
           [:div.time time]
           [speed-control opts])]))))

(defn timeline []
  (reagent/create-class
   {:render
    (fn [c]
      (let [{:keys [vims]} (reagent/props c)
            dur           (<sub [::timeline.subs/duration vims])
            playhead      (<sub [::timeline.subs/playhead vims])
            skimhead      (<sub [::timeline.subs/skimhead vims])
            playhead-perc (str (time->pct dur playhead) "%")
            skimhead-perc (when skimhead (str (time->pct dur skimhead) "%"))
            ;; Urgh Flexbox behaves differently in Safari and Chrome inside
            ;; absolutely positioned elements
            ios?          (= :ios util.dom/operating-system)]
        [:div.timeline.ac.f1
         (cond-> (handlers c vims dur)
           ios? (assoc :class "dc jc"))
         [:div.time.left
          {:style (when ios? {:margin-top -2})}] ; .progress class clashes with bootstrap
         [:div.time.passed
          {:style (cond-> {:width playhead-perc} ios? (assoc :margin-top -2))}]
         [:div.head
          {:class (if skimhead "skimhead" "playhead")
           :style {:left       (or skimhead-perc playhead-perc)
                   :margin-top (when ios? (if skimhead -9 -8))}}]]))}))

(defn timeline-bar [opts]
  [re-com/h-box
   :class "bar timeline-container"
   :justify :center
   :align :center
   :gap "18px"
   :children [[play-pause opts]
              [timeline opts]
              [time-or-speed-control opts]]])
