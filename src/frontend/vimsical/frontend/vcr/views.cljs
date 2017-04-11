(ns vimsical.frontend.vcr.views
  (:require
   [re-com.core :refer [v-box h-box box]]
   [vimsical.frontend.views.splits :refer [n-h-split n-v-split]]))

(defn vcr []
  [v-box
   :class "vcr"
   :size "1"
   :children
   [[:div "timeline"]
    [n-h-split
     :panels [[:h1 "live-preview"]
              [n-v-split
               :height "100%"
               ;:splitter-size "30px"
               :panels [[:div 1] [:div 2] [:div 3]]
               :margin "0"]]
     :splitter-child [:div "resizer"]
     ;:splitter-size "100px"
     :initial-split 60
     :margin "0"]]])
