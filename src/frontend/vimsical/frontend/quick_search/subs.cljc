(ns vimsical.frontend.quick-search.subs
  (:require [re-frame.core :as re-frame]
            [vimsical.subgraph :as sg]
            [vimsical.frontend.util.subgraph :as util.sg]
            [vimsical.frontend.util.search :as util.search]
            [vimsical.frontend.db :as db]
            [vimsical.frontend.app.subs :as app.subs]
            [vimsical.common.util.core :as util :include-macros true]
            [vimsical.vcs.lib :as lib]
            [vimsical.frontend.vcs.handlers :as vcs.handlers]
            [vimsical.vcs.compiler :as compiler]))

(re-frame/reg-sub
 ::quick-search
 (fn [db [_ ?pattern]]
   (util.sg/pull* db [:app/quick-search (or ?pattern '[*])])))

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

(defmulti filters->categories first)


(def sort-subtypes
  (let [sub-types [:html :css :js]
        sort-map  (into {} (map-indexed (fn [idx v] (vector v (inc idx))) [:html :css :javascript]))]
    (fn [sub-type]
      (get sort-map sub-type))))

(defn ordered-by-subtype
  "Given a map with sub-types as its keys, orders sub-types like
  :html < :css < :js."
  [m]
  (sort-by (comp sort-subtypes key) m))

(defn filters->categories* [group-key title-key dispatch-key filters opts]
  (some->> (group-by group-key filters)
           not-empty
           (util/map-vals
            (fn [filters]
              (into {}
                    (map (fn [filter]
                           (let [title (title-key filter)]
                             [[title] {:title    title
                                       :dispatch [dispatch-key filter opts]}])))
                    filters)))
           ordered-by-subtype
           vec))

(defmethod filters->categories :libs
  [[type libs] opts]
  (filters->categories* ::lib/sub-type ::lib/name ::vcs.handlers/add-lib libs opts))

(defn filters [types-filters opts]
  (vec
   (not-empty
    (for [[type filters :as type-filters] types-filters
          :let [categories (filters->categories type-filters opts)]
          :when categories]
      [{:title (name type)} categories]))))

(re-frame/reg-sub
 ::filters
 :<- [::quick-search [:quick-search/filter-idx]]
 ;:<- [::app.subs/vims-branch-uid] ;; removed to avoid circular dep.
 ;; to fix just make this a vcs.sub instead
 :<- [::app.subs/libs]
 (fn [[{:quick-search/keys [filter-idx]} branch-uid libs] _]
   (cond-> (filters [[:libs libs]] {:branch-uid branch-uid})
     filter-idx (assoc-in [filter-idx 0 :selected?] true))))

(re-frame/reg-sub
 ::selected-filter-categories
 :<- [::filters]
 (fn [filters _]
   (some (fn [[{:keys [selected?]} cmds]]
           (when selected? cmds))
         filters)))

(re-frame/reg-sub
 ::selected-filter-search-results
 :<- [::quick-search [:quick-search/query]]
 :<- [::selected-filter-categories]
 (fn [[{:quick-search/keys [query]} filter-cmds] _]
   (when filter-cmds
     (mapv (fn [filter-category]
             (update filter-category
                     1
                     (fn [cmd-map]
                       (or (util.search/search query cmd-map)
                           (vec (vals cmd-map))))))
           filter-cmds))))

(re-frame/reg-sub
 ::selected-filter-results
 :<- [::quick-search [:quick-search/filter-idx
                      :quick-search/filter-category-idx
                      :quick-search/filter-result-idx]]
 :<- [::selected-filter-search-results]
 (fn [[{:quick-search/keys [filter-idx filter-result-idx filter-category-idx]} results] _]
   (cond-> results
     (and filter-category-idx filter-result-idx)
     (assoc-in [filter-category-idx 1 filter-result-idx :selected?] true))))
