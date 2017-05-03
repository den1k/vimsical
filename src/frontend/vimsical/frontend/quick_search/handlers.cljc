(ns vimsical.frontend.quick-search.handlers
  (:require [re-frame.core :as re-frame]
            [com.stuartsierra.mapgraph :as mg]
            [vimsical.frontend.app.handlers :as app.handlers]))

(defn move [quick-search results dir]
  (let [{:quick-search/keys [result-idx]} quick-search
        max-idx  (dec (count results))
        next-idx (case dir
                   :up (if (zero? result-idx) max-idx (dec result-idx))
                   :down (if (= max-idx result-idx) 0 (inc result-idx)))]
    (assoc quick-search :quick-search/result-idx next-idx)))

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

(re-frame/reg-event-db
 ::set-query
 (fn [db [_ query quick-search]]
   (let [quick-search (assoc quick-search :quick-search/query query
                                          :quick-search/result-idx 0)]
     (mg/add db quick-search))))

(re-frame/reg-event-fx
 ::run-cmd
 (fn [_ [_ {:keys [dispatch close?] :as cmd}]]
   (let [dispatches (cond-> [dispatch]
                      close? (conj [::close]))]
     {:dispatch-n dispatches})))

(re-frame/reg-event-db
 ::move
 (fn [db [_ quick-search results dir]]
   {:pre [(contains? #{:up :down} dir)]}
   (mg/add db (move quick-search results dir))))