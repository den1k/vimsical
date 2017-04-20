(ns vimsical.frontend.quick-search.handlers
  (:require [re-frame.core :as re-frame]
            [com.stuartsierra.mapgraph :as mg]))

(re-frame/reg-event-ctx
 ::clear-console
 (fn [& args]
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