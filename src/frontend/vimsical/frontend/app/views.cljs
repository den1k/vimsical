(ns vimsical.frontend.app.views
  (:require
   [re-com.core :as re-com]
   [vimsical.frontend.nav.views :refer [nav]]
   [vimsical.frontend.vcr.views :refer [vcr]]))

(defn app []
  (fn []
    [re-com/v-box
     :class "app"
     :children [[nav]
                [vcr]]]))