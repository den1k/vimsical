(ns vimsical.frontend.db
  (:require
   [com.stuartsierra.mapgraph :as mg]
   [com.stuartsierra.subgraph :as sg]
   [re-frame.core :as re-frame]
   [re-frame.interop :as interop]))

(defn add-to [db k entity]
  (-> db
      (mg/add entity)
      (assoc k (mg/ref-to db entity))))

(defn add-linked-entities
  "Takes a map of link-keys->entities, normalizes entities and creates links for
  them."
  [db state]
  (reduce-kv add-to db state))

(re-frame/reg-sub-raw :q sg/pull)
(re-frame/reg-sub-raw :q* sg/pull-link)

(let [state {:app/user
                               {:db/id           1
                                :user/first-name "Jane"
                                :user/last-name  "Applecrust"
                                :user/email      "kalavox@gmail.com"
                                :user/vimsae     [{:db/id               10
                                                   :vims/author         [:db/id 1]
                                                   :vims/title          "NLP Chatbot running on React Fiber"
                                                   :vims/current-branch {:db/id        100
                                                                         :branch/title "Master"}}
                                                  {:db/id               20
                                                   :vims/author         [:db/id 1]
                                                   :vims/title          "CatPhotoApp"
                                                   :vims/current-branch {:db/id        200
                                                                         :branch/title "Master"}}]}
             :app/quick-search {:db/id              5
                                :quick-search/show? true}}]
  (def default-db
    (-> (mg/new-db)
        (mg/add-id-attr :db/id)
        (add-linked-entities state))))

(re-frame/reg-event-db ::init (constantly default-db))
