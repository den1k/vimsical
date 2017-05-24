(ns vimsical.frontend.views.popovers
  (:require [re-com.core :as re-com]
            [reagent.core :as reagent]))

(defn- popover-view [{:keys [showing? body position-injected] :as opts}]
  [re-com/popover-content-wrapper
   :arrow-gap 2
   :arrow-width 20
   :on-cancel (fn [])                   ; needed render background overlay
   :arrow-length 10
   :body body
   :showing-injected? showing?
   :position-injected position-injected])

(defn popover [{:keys [showing? anchor style child position]
                :or   {position :below-center}
                :as   opts}]
  {:pre [anchor child showing?]}
  [re-com/popover-anchor-wrapper
   :showing? showing?
   :anchor anchor
   :position position
   :style style
   :popover [popover-view
             {:showing?          showing?
              :position-injected (reagent/atom position)
              :body              child}]])
