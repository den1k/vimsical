(ns vimsical.frontend.nav.handlers
  (:require [re-frame.core :as re-frame]
            [com.stuartsierra.mapgraph :as mg]
            [vimsical.vims :as vims]))

(re-frame/reg-event-db
 ::set-vims-title
 (fn [db [_ vims title]]
   (mg/add db (assoc vims ::vims/title title))))
