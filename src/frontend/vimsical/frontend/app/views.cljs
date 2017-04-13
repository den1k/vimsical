(ns vimsical.frontend.app.views
  (:require
   [re-com.core :refer [v-box h-box box]]
   [vimsical.frontend.nav.views :refer [nav]]
   [vimsical.frontend.vcr.views :refer [vcr]]))

(defn app []
  (fn []
    [v-box
     :class "app"
     :children [[nav]
                [vcr]]]))