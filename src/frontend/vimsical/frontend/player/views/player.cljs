(ns vimsical.frontend.player.views.player
  (:require
   [vimsical.frontend.views.splits :as splits]
   [vimsical.frontend.player.views.elems :as elems]
   [vimsical.frontend.player.views.preview :refer [social-bar preview-panel preview-container]]
   [vimsical.frontend.player.views.info-and-editor :refer [info-and-editor-container]]
   [vimsical.frontend.player.views.timeline :refer [timeline-bar]]))

(defn landscape []
  [splits/n-h-split
   :class "landscape-split"
   :panels
   [[preview-panel]
    [info-and-editor-container]]
   :splitter-size "1px"
   :splitter-child [elems/resizer]
   :initial-split 60
   :margin "0"])

(defn portrait []
  [:div.portrait-split
   [social-bar {:show-logo? true}]
   [preview-container]
   [timeline-bar]
   [info-and-editor-container]])

(defn player
  ([] (player {}))
  ([{:keys [orientation]
     :or   {orientation :portrait}}]
   {:pre (contains? #{:landscape :portrait} orientation)}
   [:div.vimsical-frontend-player
    {:class (name orientation)}
    (case orientation
      :landscape [landscape]
      :portrait [portrait])]))