(ns vimsical.frontend.nav.views
  (:require [vimsical.frontend.views.icons :as icons]))

(defn nav []
  [:div.nav
   [:div.logo-and-type
    [:span.logo icons/vimsical-logo]
    [:span.type icons/vimsical-type]]])