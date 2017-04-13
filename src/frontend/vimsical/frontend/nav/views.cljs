(ns vimsical.frontend.nav.views
  (:require [re-com.box :refer [v-box]]
            [vimsical.frontend.views.icons :refer [vimsical-logo vimsical-type]]))

(defn nav []
  [:div.nav
   [:div.logo-and-type
    [:span.logo vimsical-logo]
    [:span.type vimsical-type]]])