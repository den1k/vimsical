(ns vimsical.frontend.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [vimsical.frontend.config :as config]
            [vimsical.frontend.views.app.app :as app]))

(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [app/app]
                  (.getElementById js/document "app")))

(defn on-reload
  "Called by figwheel on reload. See project.clj."
  []
  (mount-root))

(defn ^:export init
  "Called from index.html"
  []
  ;(re-frame/dispatch-sync [:initialize-db])
  (dev-setup)
  (mount-root))