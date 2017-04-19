(ns vimsical.frontend.views.user
  (:require [reagent.core :as reagent]
            [re-com.core :as re-com]))

(defn avatar [{:keys [class img-url on-click size]
               :or   {size "40px"}}]
  {:pre [img-url]}
  [:div.avatar
   {:class    class
    :on-click on-click
    :style    {:width size :height size}}

   [:img.pic {:src img-url}]])
