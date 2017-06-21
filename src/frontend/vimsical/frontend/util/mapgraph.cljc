(ns vimsical.frontend.util.mapgraph
  (:refer-clojure :exclude [remove])
  (:require
   [com.stuartsierra.mapgraph :as mg]
   [com.stuartsierra.subgraph :as sg]
   [re-frame.core :as re-frame]
   [re-frame.interop :as interop]))

;;
;; * Helpers
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

(defn ref->entity [[k id]] {k id})

(defn ent->id-attr [db entity]
  (some ::mg/id-attrs (keys entity)))

(defn ->entity
  ([db x] (->entity db :db/uid x))
  ([db id-attr x]
   {:post [(mg/ref-to db %)]}
   (cond
     (map? x)       (select-keys x [id-attr])
     (mg/ref? db x) (apply hash-map x)
     (uuid? x)      {id-attr x})))

(defn ->ref
  ([db x] (->ref db :db/uid x))
  ([db id-attr x]
   {:post [(mg/ref? db %)]}
   (cond
     (mg/ref? db x) x
     (map? x)       (mg/ref-to db x)
     (uuid? x)      [id-attr x]
     (keyword? x)   (->ref db id-attr (get db x)))))

(defn ->ref-maybe
  ([db x] (->ref-maybe db :db/uid x))
  ([db id-attr x]
   (try (->ref db id-attr x) (catch #?(:clj Throwable :cljs :default) _))))

(defn ->uid
  ([db x] (->uid db :db/uid x))
  ([db id-attr x]
   {:post [(uuid? %)]}
   (cond
     (uuid? x) x
     (mg/ref? db x) (second x)
     (map? x) (get x id-attr))))
;;
;; * Add
;;

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
  [db entity key join]
  (let [path     (if (keyword? entity)
                   [(get db entity) key]
                   [(->ref db entity) key])
        join-ref (->ref db join)]
    (update-in db path (fnil conj []) join-ref)))

(defn add-join
  "Add `join-entity` to the `db` and conj its ref onto the `join-key` on
  `entity`. Will default to a vector if the join doesn't exist."
  [db entity join-key join-entity]
  (-> db
      (mg/add join-entity)
      (add-join* entity join-key join-entity)))

(defn add-ref
  [db key entity]
  (assoc db key (mg/ref-to db entity)))

;;
;; * Remove
;;

(defn remove
  [db x]
  (dissoc db (->ref db x)))

(defn remove-join*
  [db entity key join]
  (let [path     [(->ref db entity) key]
        join-ref (->ref db join)
        pred     (fn [ref] (when-not (= ref join-ref) ref))]
    (update-in db path (partial #'mg/keept pred))))

(defn remove-join
  "Remove `join` to the from the `join-key` on `entity`. Expects a vector of
  refs at join-key.  Does not remove join-entity from `db`"
  [db entity join-key join]
  (-> db (remove-join* entity join-key join)))

;;
;; * Shorthand link syntax
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
