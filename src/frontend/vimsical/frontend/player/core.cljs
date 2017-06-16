(ns vimsical.frontend.player.core
  "Core namespace for standalone player."
  (:require
   [re-frame.core :as re-frame]
   [reagent.core :as reagent]
   [vimsical.frontend.remotes.fx]      ;; side-effects
   [vimsical.frontend.remotes.backend] ;; side-effects
   [vimsical.frontend.code-editor.core :as code-editor.core]
   [vimsical.frontend.db :as db]
   [vimsical.frontend.player.views.player :refer [player]]
   [vimsical.frontend.router.handlers :as router.handlers]
   [vimsical.frontend.ui.handlers :as ui.handlers])
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
      (re-frame/dispatch-sync [::router.handlers/init])
      ;; (re-frame/dispatch-sync [::app.handlers/new-vims (<sub [::app.subs/user])])
      (mount-root))))
