(ns vimsical.frontend.player.views.player
  (:require
   [vimsical.frontend.views.splits :as splits]
   [vimsical.frontend.player.views.elems :as elems]
   [vimsical.frontend.player.views.preview :refer [preview-container]]
   [vimsical.frontend.player.views.info-and-editor :refer [info-and-editor-container]]))

(defn player []
  [:div.vimsical-frontend-player
   [splits/n-h-split
    :panels
    [[preview-container]
     [info-and-editor-container]]
    :splitter-size "1px"
    :splitter-child [elems/resizer]
    :initial-split 70
    :margin "0"]])
