(ns vimsical.frontend.timeline.handlers
  (:require
   [re-frame.core :as re-frame]
   [com.stuartsierra.mapgraph :as mg]
   [com.stuartsierra.subgraph :as sg]))


;;
;; * UI Db
;;

(re-frame/reg-event-fx
 ::register-svg
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ node]]
   {:ui-db (assoc ui-db ::svg node)}))

(re-frame/reg-event-fx
 ::dispose-svg
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ node]]
   {:ui-db (dissoc ui-db ::svg node)}))


;;
;; * Heads
;;

(re-frame/reg-event-fx
 ::chunk-mouse-move
 [(re-frame/inject-cofx :ui-db)]
 (fn [{:keys [ui-db]} [_ xy-coords coords->time]]
   (let [svg-node (get ui-db ::svg)
         t        (coords->time svg-node xy-coords)]
     (println t))))
