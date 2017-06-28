(ns vimsical.frontend.player.views.player
  (:require
   [vimsical.frontend.util.re-frame :refer [<sub]]
   [vimsical.frontend.vcs.subs :as vcs.subs]
   [vimsical.frontend.ui.subs :as ui.subs]
   [vimsical.frontend.views.splits :as splits]
   [vimsical.frontend.player.views.elems :as elems]
   [vimsical.frontend.player.views.preview :refer [social-bar preview-panel preview-container]]
   [vimsical.frontend.player.views.info-and-editor :refer [info-and-editor-container]]
   [vimsical.frontend.player.views.timeline :refer [timeline-bar]]
   [vimsical.frontend.window-listeners.views :refer [window-listeners]]
   [vimsical.frontend.app.subs :as app.subs]))

(defn landscape [opts]
  [splits/n-h-split
   :class "landscape-split"
   :panels
   [[preview-panel opts]
    [info-and-editor-container opts]]
   :splitter-size "1px"
   :splitter-child [elems/resizer]
   :initial-split 60
   :margin "0"])

(defn portrait [opts]
  [:div.portrait-split
   ;; [social-bar opts]
   [preview-container opts]
   [timeline-bar opts]
   [info-and-editor-container opts]])

(defn player [{:keys [vims standalone? orientation show-info? ui-key] :as opts}]
  (let [vims        (if standalone? (<sub [::app.subs/vims]) vims)
        files       (when vims (<sub [::vcs.subs/files vims]))
        orientation (or orientation (<sub [::ui.subs/orientation]))
        opts        (merge {:vims        vims
                            :files       files
                            :orientation orientation
                            :ui-key      :player
                            :show-info?  true} opts)]
    (when vims                          ; fixme player should have loader
      [:div.vimsical-frontend-player.player
       {:class (name orientation)}
       [(case orientation
          :landscape landscape
          :portrait  portrait)
        opts]
       (when standalone?
         [window-listeners])])))
