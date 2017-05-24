(ns vimsical.frontend.ui.handlers
  (:require [re-frame.core :as re-frame]
            [vimsical.frontend.config :as config]
   #?(:cljs [vimsical.frontend.util.dom :as util.dom])))

(re-frame/reg-event-fx
 ::init
 [(re-frame/inject-cofx :ui-db)]
 #?(:cljs
    (fn [{:keys [ui-db]} _]
      {:ui-db (assoc ui-db :orientation (util.dom/orientation)
                           :on-mobile? (util.dom/on-mobile?))})))

(re-frame/reg-event-fx
 ::on-resize
 [(re-frame/inject-cofx :ui-db)]
 #?(:cljs
    (fn [{:keys [ui-db]} _]
      {:ui-db (cond-> ui-db
                true (assoc :orientation (util.dom/orientation))
                ; check to switch the app between views in device dev simulator
                config/debug? (assoc :on-mobile? (util.dom/on-mobile?)))})))