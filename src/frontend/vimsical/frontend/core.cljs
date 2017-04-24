(ns vimsical.frontend.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [vimsical.frontend.db :as db]
            [vimsical.frontend.ui-db]   ; require to reg-fx
            [vimsical.frontend.app.views :refer [app]]))


(defn require-js-libs [cb]
  (js/require (array "vs/editor/editor.main") cb))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [app]
                  (.getElementById js/document "app")))

(defn ^:export init
  "Called from index.html"
  []
  (require-js-libs
   #(do
      (re-frame/dispatch-sync [::db/init])
      (mount-root))))
