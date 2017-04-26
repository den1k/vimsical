(ns vimsical.frontend.quick-search.handlers
  (:require [re-frame.core :as re-frame]
            [com.stuartsierra.mapgraph :as mg]
            [vimsical.frontend.app.handlers :as app.handlers]))

(re-frame/reg-event-ctx
 ::clear-console
 (fn [_]
   #?(:cljs (js/console.clear))))

(re-frame/reg-event-db
 ::toggle
 (fn [db _]
   (let [link (:app/quick-search db)]
     (update-in db [link :quick-search/show?] not))))

(re-frame/reg-event-db
 ::close
 (fn [db _]
   (let [link (:app/quick-search db)]
     (assoc-in db [link :quick-search/show?] false))))

(re-frame/reg-event-fx
 ::go-to
 (fn [{:keys [db]} [_ route]]
   {:db       db
    :dispatch [::app.handlers/route route]}))