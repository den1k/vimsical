(ns vimsical.frontend.player.core
  "Core namespace for standalone player."
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [vimsical.frontend.code-editor.util :as code-editor.util]
            [vimsical.frontend.player.views :refer [player]]
            [vimsical.frontend.player.embed :as player.embed]))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [player]
                  (.getElementById js/document "player")))

(defn ^:export init
  "Called from index.html"
  []
  (code-editor.util/require-monaco
   #(do
      ;(re-frame/dispatch-sync [::db/init])
      (mount-root))))