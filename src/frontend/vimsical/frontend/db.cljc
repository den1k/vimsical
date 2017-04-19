(ns vimsical.frontend.db
  (:require
   [com.stuartsierra.mapgraph :as mg]
   [com.stuartsierra.subgraph :as sg]
   [re-frame.core :as re-frame]
   [re-frame.interop :as interop]))


(re-frame/reg-sub-raw :q sg/pull)
(re-frame/reg-sub-raw :q* sg/pull-link)

(let [user-id 1
      links   {:app/user [:db/id user-id]}
      user    {:db/id user-id :user/first-name "foo" :user/last-name "bar"}]
  (def default-db
    (-> (mg/new-db)
        (mg/add-id-attr :db/id)
        (merge links)
        (mg/add user))))

(re-frame/reg-event-db ::init (constantly default-db))
