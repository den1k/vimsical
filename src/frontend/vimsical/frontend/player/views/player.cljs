(ns vimsical.frontend.player.views.player
  (:require
   [vimsical.frontend.util.re-frame :refer [<sub]]
   [vimsical.frontend.ui.subs :as ui.subs]
   [vimsical.frontend.views.splits :as splits]
   [vimsical.frontend.player.views.elems :as elems]
   [vimsical.frontend.player.views.preview :refer [social-bar preview-panel preview-container]]
   [vimsical.frontend.player.views.info-and-editor :refer [info-and-editor-container]]
   [vimsical.frontend.player.views.timeline :refer [timeline-bar]]))

(defn landscape [{:keys [vims]}]
  [splits/n-h-split
   :class "landscape-split"
   :panels
   [[preview-panel {:vims vims}]
    [info-and-editor-container {:vims vims}]]
   :splitter-size "1px"
   :splitter-child [elems/resizer]
   :initial-split 60
   :margin "0"])

(defn portrait [{:keys [vims]}]
  [:div.portrait-split
   [social-bar]
   [preview-container]
   [timeline-bar]
   [info-and-editor-container]])

(defn player [{:keys [vims]}]
  (let [orientation (<sub [::ui.subs/orientation])]
    [:div.vimsical-frontend-player
     {:class (name orientation)}
     [(case orientation
        :landscape landscape
        :portrait portrait)
      {:vims vims}]]))