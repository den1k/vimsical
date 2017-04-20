(ns vimsical.frontend.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [re-frisk.core :refer [enable-re-frisk!]]
            [vimsical.frontend.config :as config]
            [vimsical.frontend.db :as db]
            [vimsical.frontend.app.views :refer [app]]))

(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (enable-re-frisk!)
    (println "dev mode")))

(defn require-js-libs [cb]
  (js/require (array "vs/editor/editor.main") cb))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [app]
                  (.getElementById js/document "app")))

(defn on-reload
  "Called by figwheel on reload. See project.clj."
  []
  (mount-root))

(defn ^:export init
  "Called from index.html"
  []
  (require-js-libs
   #(do
      (re-frame/dispatch-sync [::db/init])
      (dev-setup)
      (mount-root))))