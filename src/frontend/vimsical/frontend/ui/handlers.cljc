(ns vimsical.frontend.ui.handlers
  (:require [re-frame.core :as re-frame]
            [vimsical.frontend.util.dom :as util.dom]))

(re-frame/reg-event-fx
 ::init
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} _]
   #?(:cljs
      (let [{:keys [width height]} (util.dom/body-rect)
            orientation (if (> width height) :landscape :portrait)]
        {:ui-db (assoc ui-db :orientation orientation)}))))

(re-frame/reg-event-fx
 ::on-resize
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ {:keys [width height] :as bounding-rect}]]
   (let [orientation (if (> width height) :landscape :portrait)]
     {:ui-db (assoc ui-db :orientation orientation)})))