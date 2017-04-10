(ns vimsical.frontend.views.vcr.vcr
  (:require [vimsical.frontend.views.splits :refer [n-v-split]]))

(defn vcr []
  [:div
   [:div "timeline"]
   [n-v-split
    :height "100%"
    :panels [[:div 1] [:div 2] [:div 3]]]])
