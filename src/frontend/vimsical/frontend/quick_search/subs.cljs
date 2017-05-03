(ns vimsical.frontend.quick-search.subs
  (:require [re-frame.core :as re-frame]
            [com.stuartsierra.mapgraph :as mg]
            [vimsical.frontend.util.re-frame :refer [<sub-query]]
            [vimsical.frontend.util.search :as util.search]
            [vimsical.frontend.db :as db]))

;; todo ::app/route subs

(re-frame/reg-sub
 ::quick-search
 (fn [db [_ ?pattern]]
   (db/pull* db [:app/quick-search (or ?pattern '[*])])))

(re-frame/reg-sub
 ::search-results
 :<- [::quick-search]
 (fn [{:quick-search/keys [query commands result-idx]} _]
   (util.search/search query commands)))

(re-frame/reg-sub
 ::default-results
 :<- [::quick-search]
 (fn [{:quick-search/keys [commands]} _]
   (-> commands vals vec)))

(re-frame/reg-sub
 ::results
 :<- [::quick-search [:quick-search/result-idx]]
 :<- [::search-results]
 :<- [::default-results]
 (fn [[{:quick-search/keys [result-idx]} results defaults] _]
   (assoc-in (or results defaults) [result-idx :selected?] true)))

