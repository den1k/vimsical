(ns vimsical.frontend.app.handlers
  (:require [re-frame.core :as re-frame]
   ;; alias often used mapgraph stuff
            [com.stuartsierra.mapgraph :as mg]))

(re-frame/reg-event-db
 ::open-vims
 (fn [db [_ vims]]
   (assoc db :app/vims (mg/ref-to db vims))))

#_(re-frame/reg-cofx
   ::open-vims-time
   (fn [cofx arg]
     #?(:cljs (js/console.debug :INJECT cofx "ARG" arg))
     (assoc cofx :time 123)))

#_(re-frame/reg-event-fx
   ::open-vims
   [(re-frame/inject-cofx ::open-vims-time :CFX-ARG)]
   (fn [{:keys [db time] :as cofx} [_ vims]]

     {:db (assoc db :app/vims (mg/ref-to db vims))}))
