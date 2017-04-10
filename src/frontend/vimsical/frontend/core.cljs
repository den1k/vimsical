(ns vimsical.frontend.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]))

(defn -main []
  ;; Called by figwheel
  (prn "CALLED BY FIGHWEEL")
  )

(defn temp-root []
  (fn []
    [:div "Hello from Johnny"]))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [temp-root]
                  (.getElementById js/document "app")))

(defn ^:export init []
  ;(re-frame/dispatch-sync [:initialize-db])
  ;(dev-setup)
  (mount-root))