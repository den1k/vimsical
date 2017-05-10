(ns vimsical.frontend.core
  (:require
   [clojure.spec.test :as st]
   [re-frame.core :as re-frame]
   [reagent.core :as reagent]
   [vimsical.frontend.app.views :refer [app]]
   [vimsical.frontend.code-editor.core :as code-editor.core]
   [vimsical.frontend.db :as db]
   [vimsical.frontend.vcs.handlers :as vcs.handlers]))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [app]
                  (.getElementById js/document "app")))

(defn ^:export init
  "Called from index.html"
  []
  #_(st/instrument)
  (code-editor.core/require-monaco
   #(do
      (re-frame/dispatch-sync [::db/init])
      (re-frame/dispatch-sync [::vcs.handlers/init-vims])
      (mount-root))))
