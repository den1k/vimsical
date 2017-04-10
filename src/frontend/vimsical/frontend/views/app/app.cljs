(ns vimsical.frontend.views.app.app
  (:require [vimsical.frontend.views.vcr.vcr :refer [vcr]]))

(defn app []
  (fn []
    [:div "Hello from Jon"
     [vcr]]))