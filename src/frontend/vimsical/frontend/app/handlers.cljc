(ns vimsical.frontend.app.handlers
  (:require [re-frame.core :as re-frame]
            [com.stuartsierra.mapgraph :as mg]
            [vimsical.vims :as vims]
            [vimsical.user :as user]
            [vimsical.common.uuid :refer [uuid]]
            [vimsical.frontend.vcs.handlers :as vcs.handlers])
  (:refer-clojure :exclude [uuid]))

(re-frame/reg-event-db
 ::open-vims
 (fn [db [_ vims]]
   (assoc db :app/vims (mg/ref-to db vims))))

(re-frame/reg-event-db
 ::route
 (fn [db [_ route]]
   (assoc db :app/route route)))

(re-frame/reg-event-fx
 ::new-vims
 (fn [{:keys [db]} [_ owner {:keys [open?]}]]
   (let [vims  (-> (vims/new-vims (uuid) owner)
                   (vimsical.vcs/init-vims))
         owner (update owner ::user/vimsae conj vims)]
     (cond-> {:db (mg/add db owner)}
       open? (assoc :dispatch [::open-vims vims])))))