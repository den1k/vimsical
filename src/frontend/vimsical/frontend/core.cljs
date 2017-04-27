(ns vimsical.frontend.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [clojure.spec.test :as st]
            [vimsical.frontend.db :as db]
            [vimsical.frontend.vcs.handlers :as vcs.handlers]
            [vimsical.frontend.ui-db]   ; require to reg-fx
            [vimsical.frontend.app.views :refer [app]]
            [vimsical.frontend.code-editor.util :as code-editor.util]))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [app]
                  (.getElementById js/document "app")))

(defn ^:export init
  "Called from index.html"
  []
  #_(st/instrument)
  (code-editor.util/require-monaco
   #(do
      (re-frame/dispatch-sync [::db/init])
      (re-frame/dispatch-sync [::vcs.handlers/init-vims])
      (mount-root))))
