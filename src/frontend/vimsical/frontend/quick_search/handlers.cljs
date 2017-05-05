(ns vimsical.frontend.quick-search.handlers
  (:require [re-frame.core :as re-frame]
            [com.stuartsierra.mapgraph :as mg]
            [vimsical.frontend.app.handlers :as app.handlers]
            [vimsical.common.util.core :as util]))

(defn move-filters [quick-search filters dir]
  (let [{:quick-search/keys [filter-idx]} quick-search
        max-idx  (dec (count filters))
        next-idx (case dir
                   :left (if (or (nil? filter-idx) (zero? filter-idx))
                           nil
                           (dec filter-idx))
                   :right (cond
                            (= max-idx filter-idx) nil
                            (nil? filter-idx) 0
                            :else (inc filter-idx)))]
    (assoc quick-search :quick-search/filter-idx next-idx)))

(defn move-results
  [{:quick-search/keys [result-idx filter-idx] :as quick-search} results dir]
  (let [max-idx  (dec (count results))
        next-idx (case dir
                   :up (if (zero? result-idx) max-idx (dec result-idx))
                   :down (if (= max-idx result-idx) 0 (inc result-idx)))]
    (assoc quick-search :quick-search/result-idx next-idx)))

(defn move-filter-result
  [{:as                quick-search
    :quick-search/keys [filter-idx filter-result-idx filter-category-idx]} filters dir]
  (let [max-idx  (some-> filters
                         (get-in [filter-idx 1 filter-category-idx 1])
                         count
                         dec)
        next-idx (case dir
                   :up (cond
                         (zero? filter-result-idx) nil
                         (nil? filter-result-idx) max-idx
                         :else (dec filter-result-idx))
                   :down (cond
                           (= max-idx filter-result-idx) nil
                           (nil? filter-result-idx) 0
                           :else (inc filter-result-idx)))]
    (assoc quick-search :quick-search/filter-category-idx 0
                        :quick-search/filter-result-idx next-idx)))

(defn move-filter-category
  [{:as quick-search :quick-search/keys [filter-idx filter-category-idx]}
   filters dir]
  (let [max-idx  (some-> filters (get filter-idx) second count dec)
        next-idx (case dir
                   :left (if (zero? filter-category-idx)
                           max-idx
                           (dec filter-category-idx))
                   :right (if (= max-idx filter-category-idx)
                            0
                            (inc filter-category-idx)))]
    (assoc quick-search :quick-search/filter-category-idx next-idx
                        :quick-search/filter-result-idx 0)))

;; todo hook up :.selected highlight

(defn move
  [dir {:keys [quick-search results filters]}]
  (let [{:quick-search/keys [result-idx filter-idx filter-result-idx]} quick-search
        horizontal? (#{:left :right} dir)
        vertical?   (#{:up :down} dir)]
    (cond
      (and filter-result-idx horizontal?) (move-filter-category quick-search filters dir)
      (and filter-idx vertical?) (move-filter-result quick-search filters dir)
      horizontal? (move-filters quick-search filters dir)
      vertical? (move-results quick-search results dir))))

(re-frame/reg-event-ctx
 ::clear-console
 (fn [_]
   (js/console.clear)))

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
 (fn [db [_ dir opts]]
   {:pre [(contains? #{:up :down :left :right} dir)]}
   (mg/add db (move dir opts))))

(re-frame/reg-event-db
 ::update-result-idx
 (fn [db [_ quick-search results res]]
   (let [idx (util/index-of results res)]
     (mg/add db (assoc quick-search :quick-search/result-idx idx)))))