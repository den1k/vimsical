(ns vimsical.frontend.ui.handlers
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-event-fx
 ::on-resize
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ {:keys [width height] :as bounding-rect}]]
   (let [orientation (if (> width height) :landscape :portrait)]
     {:ui-db (assoc ui-db :orientation orientation)})))