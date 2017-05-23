(ns vimsical.frontend.ui.handlers
  (:require [re-frame.core :as re-frame]
            [vimsical.frontend.util.dom :as util.dom]))

(re-frame/reg-event-fx
 ::init
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} _]
   {:ui-db (assoc ui-db :orientation (util.dom/orientation))}))

(re-frame/reg-event-fx
 ::on-resize
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} _]
   {:ui-db (assoc ui-db :orientation (util.dom/orientation)
                        :on-mobile? (util.dom/on-mobile?))}))