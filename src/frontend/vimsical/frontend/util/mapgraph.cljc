(ns vimsical.frontend.util.mapgraph
  (:require
   [com.stuartsierra.mapgraph :as mg]
   [com.stuartsierra.subgraph :as sg]
   [re-frame.core :as re-frame]
   [re-frame.interop :as interop]))

;;
;; * Mapgraph helpers
;;

;;
;; ** Adding entities
;;

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

(defn add-join*
  [db
   target-ref-or-link-or-entity
   key
   join-ref-or-entity]
  (let [entity-ref (cond
                     (mg/ref? db target-ref-or-link-or-entity) target-ref-or-link-or-entity
                     (map? target-ref-or-link-or-entity)       (mg/ref-to db target-ref-or-link-or-entity)
                     (keyword? target-ref-or-link-or-entity)   (get db target-ref-or-link-or-entity))
        join-ref   (cond
                     (mg/ref? db join-ref-or-entity) join-ref-or-entity
                     (map? join-ref-or-entity)       (mg/ref-to db join-ref-or-entity))
        join-path  (conj entity-ref key)]
    (update-in db join-path (fnil conj []) join-ref)))

(defn add-join
  "Add `join-entity` to the `db` and conj its ref onto the `join-key` on
  `entity`. Will default to a vector if the join doesn't exist."
  [db entity join-key join-entity]
  (-> db
      (mg/add join-entity)
      (add-join* entity join-key join-entity)))

;;
;; ** Removing entities
;;

(defn- remove-ref [db ref]
  (reduce-kv
   (fn [db k v]
     (cond
       (mg/ref? db v) (remove-ref db v)
       (coll? v)      (reduce remove-ref db v)
       :else          db))
   (dissoc db ref) (get db ref)))


(defn remove-entity [db entity]
  (remove-ref db (mg/ref-to db entity)))

(defn remove-links [db & keywords]
  (reduce remove-ref db (map keywords db)))

;;
;; ** Shorthand link syntax
;;

(defn rewrite-pattern? [pattern]
  (and (= 2 (count pattern))
       (let [[?link ?pattern] pattern]
         (and (keyword? ?link)
              (vector? ?pattern)))))

(defn rewrite-pattern [[link pattern]]
  {:link    link
   :pattern [{[link '_] pattern}]})

(defn pull* [db pattern]
  (if (rewrite-pattern? pattern)
    (let [{:keys [link pattern]} (rewrite-pattern pattern)]
      (get (mg/pull db pattern) link))
    (mg/pull db pattern)))

(defn pull-sub*
  ([db pattern]
   (if (rewrite-pattern? pattern)
     (let [{:keys [link pattern]} (rewrite-pattern pattern)]
       (interop/make-reaction
        (fn []
          (get @(sg/pull db pattern) link))))
     (sg/pull db pattern)))
  ([db pattern lookup-ref]
   (sg/pull db pattern lookup-ref)))

;;
;; * Re-frame x Mapgraph
;;

(re-frame/reg-sub-raw
 :q
 (fn [db [_ pattern ?lookup-ref]]
   (if ?lookup-ref
     (pull-sub* db pattern ?lookup-ref)
     (pull-sub* db pattern))))
