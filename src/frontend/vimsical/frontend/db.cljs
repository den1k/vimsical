(ns vimsical.frontend.db
  (:require
   [com.stuartsierra.mapgraph :as mg]
   [com.stuartsierra.subgraph :as sg]
   [re-frame.core :as re-frame]
   [vimsical.vcs.branch :as branch]
   [vimsical.common.util.core :as util]
   [vimsical.vcs.file :as file]
   [vimsical.vcs.lib :as lib]
   [vimsical.vcs.compiler :as compiler]
   [vimsical.common.test :refer [uuid]]
   [vimsical.frontend.quick-search.commands :as quick-search.commands]))

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

(defn rewrite-query? [qexpr]
  (and (= 2 (count qexpr))
       (let [[?link ?pattern] qexpr]
         (and (keyword? ?link)
              (vector? ?pattern)))))

(defn rewrite-link-query [[link pattern]]
  {:link    link
   :pattern [{[link '_] pattern}]})

(defn pull* [db qexpr]
  (if (rewrite-query? qexpr)
    (let [{:keys [link pattern]} (rewrite-link-query qexpr)]
      (get (mg/pull db pattern) link))
    (mg/pull db qexpr)))

(re-frame/reg-sub-raw :q sg/pull)
(re-frame/reg-sub-raw :q* sg/pull-link)

(defn new-vims
  ([author-ref title] (new-vims author-ref title {}))
  ([author-ref title {:keys [libs compilers] :as opts}]
   (let [libs-by-type      (group-by ::lib/sub-type libs)
         compilers-by-type (group-by ::compiler/to-sub-type compilers)
         files             [{:db/id (uuid :file-html) ::file/type :text ::file/sub-type :html ::file/compiler (:html compilers-by-type)}
                            {:db/id (uuid :file-css) ::file/type :text ::file/sub-type :css ::file/compiler (:css compilers-by-type)}
                            {:db/id (uuid :file-js) ::file/type :text ::file/sub-type :javascript ::file/compiler (:javascript compilers-by-type)}]
         branches          [{:db/id (uuid :branch-master) ::branch/name "master" ::branch/start-delta-id nil ::branch/entry-delta-id nil ::branch/created-at (util/now) ::branch/files files ::branch/libs (:javascript libs-by-type)}]]
     {:db/id         (uuid title)
      :vims/author   author-ref
      :vims/title    title
      :vims/branches branches})))

(let [js-libs   [{:db/id         (uuid :lib-js-jquery)
                  ::lib/title    "jQuery"
                  ::lib/type     :text
                  ::lib/sub-type :javascript
                  ::lib/src      "https://cdnjs.cloudflare.com/ajax/libs/jquery/3.1.1/jquery.min.js"}]
      compilers [{:db/id                 (uuid :babel-compiler)
                  ::compiler/name        "Babel"
                  ::compiler/type        :text
                  ::compiler/sub-type    :babel
                  ::compiler/to-sub-type :javascript}]
      state     {:app/user         {:db/id           (uuid :user)
                                    :user/first-name "Jane"
                                    :user/last-name  "Applecrust"
                                    :user/email      "kalavox@gmail.com"
                                    :user/vimsae     [(new-vims [:db/id (uuid :user)] "NLP Chatbot running on React Fiber")
                                                      (new-vims [:db/id (uuid :user)] "CatPhotoApp" {:libs js-libs})]}
                 :app/vims         [:db/id (uuid "CatPhotoApp")]
                 :app/quick-search {:db/id                            (uuid :quick-search)
                                    :quick-search/show?               false
                                    :quick-search/result-idx          0
                                    :quick-search/query               ""
                                    :quick-search/commands            quick-search.commands/commands
                                    :quick-search/filter-idx          nil
                                    :quick-search/filter-result-idx   nil
                                    :quick-search/filter-category-idx nil}
                 :app/libs         js-libs
                 :app/compilers    compilers
                 :app/route        :route/vcr}]

  (def default-db
    (-> (mg/new-db)
        (mg/add-id-attr :db/id)
        (add-linked-entities state))))

(re-frame/reg-event-db ::init (constantly default-db))