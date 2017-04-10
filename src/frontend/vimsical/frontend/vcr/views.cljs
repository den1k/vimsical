(ns vimsical.frontend.vcr.views
  (:require
   [re-com.core :refer [v-box h-box box]]
   [vimsical.frontend.views.splits :refer [h-split n-v-split]]))

(defn vcr []
  [v-box
   :class "vcr"
   :size "1"
   :children
   [[:div "timeline"]
    [h-split
     :panel-1 [:h1 "live-preview"]
     :panel-2 [n-v-split
               :height "100%"
               :panels [[:div 1] [:div 2] [:div 3]]
               :margin "0"]
     :splitter-child [:div "resizer"]
     :initial-split 60
     :margin "0"]]])
