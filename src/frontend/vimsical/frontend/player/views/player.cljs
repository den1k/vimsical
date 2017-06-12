(ns vimsical.frontend.player.views.player
  (:require
   [vimsical.frontend.util.re-frame :refer [<sub]]
   [vimsical.frontend.ui.subs :as ui.subs]
   [vimsical.frontend.views.splits :as splits]
   [vimsical.frontend.player.views.elems :as elems]
   [vimsical.frontend.player.views.preview :refer [social-bar preview-panel preview-container]]
   [vimsical.frontend.player.views.info-and-editor :refer [info-and-editor-container]]
   [vimsical.frontend.player.views.timeline :refer [timeline-bar]]
   [vimsical.frontend.window-listeners.views :refer [window-listeners]]
   [vimsical.frontend.app.subs :as app.subs]))

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
   ;[social-bar {:vims vims}]
   [preview-container {:vims vims}]
   [timeline-bar {:vims vims}]
   [info-and-editor-container {:vims vims}]])

(defn player [{:keys [vims standalone? orientation]}]
  (let [orientation (or orientation (<sub [::ui.subs/orientation]))
        vims        (if standalone? (<sub [::app.subs/vims]) vims)]
    (when vims                          ; fixme player should have loader
      [:div.vimsical-frontend-player.player
       {:class (name orientation)}
       [(case orientation
          :landscape landscape
          :portrait portrait)
        {:vims vims}]
       (when standalone?
         [window-listeners])])))