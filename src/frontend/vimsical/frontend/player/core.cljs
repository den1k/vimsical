(ns vimsical.frontend.player.core
  "Core namespace for standalone player."
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [vimsical.frontend.code-editor.core :as code-editor.core]
            [vimsical.frontend.player.views.player :refer [player]]
            [vimsical.frontend.player.embed :as player.embed]
            [vimsical.frontend.db :as db]
            [vimsical.frontend.ui.handlers :as ui.handlers]
            [vimsical.frontend.util.re-frame :refer [<sub]]
            [vimsical.frontend.app.subs :as app.subs]
            [vimsical.frontend.app.handlers :as app.handlers])
  (:refer-clojure :exclude [uuid]))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [player {:standalone? true}]
                  (.getElementById js/document "app")))

(defn ^:export init
  "Called from index.html"
  []
  (code-editor.core/require-monaco
   #(do
      (re-frame/dispatch-sync [::db/init])
      (re-frame/dispatch-sync [::ui.handlers/init])
      ;; fixme temp vims creation
      (re-frame/dispatch-sync [::app.handlers/new-vims (<sub [::app.subs/user])])
      ;; (re-frame/dispatch-sync [::db/init])
      (mount-root))))
