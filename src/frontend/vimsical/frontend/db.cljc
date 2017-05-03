(ns vimsical.frontend.db
  (:require
   [com.stuartsierra.mapgraph :as mg]
   [com.stuartsierra.subgraph :as sg]
   [re-frame.core :as re-frame]
   [vimsical.vcs.branch :as branch]
   [vimsical.common.util.core :as util]
   [vimsical.vcs.file :as file]
   [vimsical.vcs.lib :as lib]
   [vimsical.common.test :refer [uuid]]))

(defn- entities? [db coll]
  (boolean
   (when (coll? coll)
     (every? (partial mg/entity? db) coll))))

(defn- ref-or-refs [db x]
  (cond
    (mg/entity? db x) (mg/ref-to db x)
    (entities? db x) (mapv (partial mg/ref-to db) x)
    :else nil))

(defmulti add
  (fn [db entity-or-entities]
    (cond
      (mg/entity? db entity-or-entities) :entity
      (entities? db entity-or-entities) :entities)))

(defmethod add :entity
  ([db entity]
   (mg/add db entity)))

(defmethod add :entities
  ([db entities]
   (apply mg/add db entities)))

(defn add-to [db k v]
  (if-let [rors (ref-or-refs db v)]
    (-> db
        (add v)
        (assoc k rors))
    (assoc db k v)))

(defn add-linked-entities
  "Takes a map of link-keys->entities, normalizes entities and creates links for
  them."
  [db state]
  (reduce-kv add-to db state))

(re-frame/reg-sub-raw :q sg/pull)
(re-frame/reg-sub-raw :q* sg/pull-link)

(defn new-vims
  ([author-ref title] (new-vims author-ref title {}))
  ([author-ref title {:keys [libs] :as opts}]
   (let [libs-by-type (group-by ::lib/sub-type libs)
         files        [{:db/id (uuid :file-html) ::file/type :text ::file/sub-type :html}
                       {:db/id (uuid :file-css) ::file/type :text ::file/sub-type :css}
                       {:db/id (uuid :file-js) ::file/type :text ::file/sub-type :javascript ::file/compiler :babel}]
         branches     [{:db/id (uuid :branch-master) ::branch/name "master" ::branch/start-delta-id nil ::branch/entry-delta-id nil ::branch/created-at (util/now) ::branch/files files ::branch/libs (:javascript libs-by-type)}]]
     {:db/id         (uuid title)
      :vims/author   author-ref
      :vims/title    title
      :vims/branches branches})))

(let [js-libs [{:db/id         (uuid :lib-js-jquery)
                ::lib/title    "jQuery"
                ::lib/type     :text
                ::lib/sub-type :javascript
                ::lib/src      "https://cdnjs.cloudflare.com/ajax/libs/jquery/3.1.1/jquery.min.js"}]
      state   {:app/user         {:db/id           (uuid :user)
                                  :user/first-name "Jane"
                                  :user/last-name  "Applecrust"
                                  :user/email      "kalavox@gmail.com"
                                  :user/vimsae     [(new-vims [:db/id (uuid :user)] "NLP Chatbot running on React Fiber")
                                                    (new-vims [:db/id (uuid :user)] "CatPhotoApp" {:libs js-libs})]}
               :app/vims         [:db/id (uuid "CatPhotoApp")]
               :app/quick-search {:db/id              (uuid :quick-search)
                                  :quick-search/show? false}
               :app/libs         js-libs
               :app/route        :route/vcr}]

  (def default-db
    (-> (mg/new-db)
        (mg/add-id-attr :db/id)
        (add-linked-entities state))))

(re-frame/reg-event-db ::init (constantly default-db))