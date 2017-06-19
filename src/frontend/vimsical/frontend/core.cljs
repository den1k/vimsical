(ns vimsical.frontend.core
  (:require
   #_[clojure.spec.test.alpha :as st]
   [re-frame.core :as re-frame]
   [reagent.core :as reagent]
   [vimsical.frontend.remotes.fx]       ;; side-effects
   [vimsical.frontend.remotes.backend]  ;; side-effects
   [vimsical.frontend.app.views :refer [app]]
   [vimsical.frontend.code-editor.core :as code-editor.core]
   [vimsical.frontend.db :as db]
   [vimsical.frontend.router.handlers :as router.handlers]
   [vimsical.frontend.ui.handlers :as ui.handlers]
   [vimsical.frontend.user.handlers :as user.handlers]))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [app] (.getElementById js/document "app")))

(defn ^:export init
  "Called from index.html"
  []
  #_(st/instrument)
  (code-editor.core/require-monaco
   #(do
      (re-frame/dispatch-sync [::db/init])
      (re-frame/dispatch-sync [::router.handlers/init])
      (re-frame/dispatch-sync [::ui.handlers/init])
      (re-frame/dispatch [::user.handlers/me])
      (mount-root))))
