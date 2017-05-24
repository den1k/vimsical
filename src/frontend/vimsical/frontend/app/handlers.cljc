(ns vimsical.frontend.app.handlers
  (:require [re-frame.core :as re-frame]
            [com.stuartsierra.mapgraph :as mg]))

(re-frame/reg-event-db
 ::open-vims
 (fn [db [_ vims]]
   (assoc db :app/vims (mg/ref-to db vims))))

(re-frame/reg-event-db
 ::route
 (fn [db [_ route]]
   (assoc db :app/route route)))