(ns vimsical.frontend.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [vimsical.frontend.config :as config]))

(defn temp-root []
  (fn []
    [:div "Hello from Johnny"]))

(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [temp-root]
                  (.getElementById js/document "app")))

(defn on-reload
  "Called by figwheel on reload. See project.clj."
  [])

(defn ^:export init
  "Called from index.html"
  []
  ;(re-frame/dispatch-sync [:initialize-db])
  (dev-setup)
  (mount-root))