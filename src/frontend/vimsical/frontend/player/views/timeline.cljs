(ns vimsical.frontend.player.views.timeline
  (:require [vimsical.frontend.timeline.subs :as timeline.subs]
            [vimsical.frontend.util.dom :as util.dom :refer-macros [e> e->]]
            [vimsical.frontend.util.re-frame :refer [<sub]]
            [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [vimsical.frontend.player.handlers :as handlers]))

(defn time->pct [duration playhead]
  (-> playhead (/ duration) (* 100)))

(defn perc->time [duration perc]
  (-> perc (/ 100) (* duration)))

(defn e->time [c e dur]
  (->> (util.dom/rel-component-mouse-coords-percs c e)
       first
       (perc->time dur)))

(defn handlers [c dur]
  {:on-mouse-enter (e> (re-frame/dispatch [::handlers/on-mouse-enter (e->time c e dur)]))
   :on-mouse-move  (e> (re-frame/dispatch [::handlers/on-mouse-move (e->time c e dur)]))
   :on-mouse-leave (e> (re-frame/dispatch [::handlers/on-mouse-leave]))
   :on-click       (e> (re-frame/dispatch [::handlers/on-click (e->time c e dur)]))})

(defn timeline []
  (reagent/create-class
   {:render
    (fn [c]
      (let [dur           (<sub [::timeline.subs/duration])
            playhead      (<sub [::timeline.subs/playhead])
            skimhead      (<sub [::timeline.subs/skimhead])
            playhead-perc (str (time->pct dur playhead) "%")
            skimhead-perc (when skimhead (str (time->pct dur skimhead) "%"))]
        [:div.timeline.ac.f1
         (handlers c dur)
         [:div.progress.left]
         [:div.progress.passed
          {:style {:width playhead-perc}}]
         [:div.head
          {:class (if skimhead "skimhead" "playhead")
           :style {:left (or skimhead-perc playhead-perc)}}]]))}))
