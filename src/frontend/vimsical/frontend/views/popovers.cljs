(ns vimsical.frontend.views.popovers
  (:require [re-com.core :as re-com]
            [reagent.core :as reagent]
            [vimsical.frontend.util.dom :refer-macros [e>]]))

(defn- popover-view [{:keys [showing? body position-injected on-cancel] :as opts}]
  [re-com/popover-content-wrapper
   :arrow-gap 2
   :arrow-width 20
   :on-cancel on-cancel
   :arrow-length 10
   :body body
   :showing-injected? showing?
   :position-injected position-injected])

(defn popover
  [{:as   opts
    :keys [showing? anchor style child position on-cancel]
    :or   {position :below-center}}]
  {:pre [anchor child showing?]}
  [re-com/popover-anchor-wrapper
   :showing? showing?
   :anchor anchor
   :position position
   :style style
   :popover [popover-view
             {:on-cancel         (or on-cancel #(reset! showing? false))
              :showing?          showing?
              :position-injected (reagent/atom position)
              :body              child}]])

(defn tooltip
  [{:keys [label showing? anchor position]
    :or   {position :below-center}}]
  [re-com/popover-tooltip
   :label label
   :position position
   :showing? showing?
   :anchor anchor])