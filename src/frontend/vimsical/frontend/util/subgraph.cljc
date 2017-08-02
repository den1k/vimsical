(ns vimsical.frontend.util.subgraph
  (:refer-clojure :exclude [remove])
  (:require
   [vimsical.subgraph :as sg]
   [vimsical.subgraph.re-frame :as sg.re-frame]
   [re-frame.core :as re-frame]
   [re-frame.interop :as interop]))

;;
;; * Helpers
;;

(defn- entities? [db coll]
  (boolean
   (when (coll? coll)
     (every? (partial sg/entity? db) coll))))

(defn- ref-or-refs [db x]
  (cond
    (sg/entity? db x) (sg/ref-to db x)
    (entities? db x) (mapv (partial sg/ref-to db) x)
    :else nil))

(defn ref->entity [[k id]] {k id})

(defn ent->id-attr [db entity]
  (some ::sg/id-attrs (keys entity)))

(defn ->entity
  ([db x] (->entity db :db/uid x))
  ([db id-attr x]
   {:post [(sg/ref-to db %)]}
   (cond
     (map? x)       (select-keys x [id-attr])
     (sg/ref? db x) (apply hash-map x)
     (uuid? x)      {id-attr x})))

(defn ->ref
  ([db x] (->ref db :db/uid x))
  ([db id-attr x]
   {:post [(sg/ref? db %)]}
   (cond
     (sg/ref? db x) x
     (map? x)       (sg/ref-to db x)
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
     (sg/ref? db x) (second x)
     (map? x) (get x id-attr))))
;;
;; * Add
;;

(defmulti add
  (fn [db entity-or-entities]
    (cond
      (sg/entity? db entity-or-entities) :entity
      (entities? db entity-or-entities) :entities)))

(defmethod add :entity
  ([db entity]
   (sg/add db entity)))

(defmethod add :entities
  ([db entities]
   (apply sg/add db entities)))

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
      (sg/add join-entity)
      (add-join* entity join-key join-entity)))

(defn add-ref
  [db key entity]
  (assoc db key (sg/ref-to db entity)))

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
    (update-in db path (partial #'sg/keept pred))))

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
      (get (sg/pull db pattern) link))
    (sg/pull db pattern)))

(defn pull-sub*
  ([db pattern]
   (if (rewrite-pattern? pattern)
     (let [{:keys [link pattern]} (rewrite-pattern pattern)]
       (interop/make-reaction
        (fn []
          (get @(sg.re-frame/pull db pattern) link))))
     (sg.re-frame/pull db pattern)))
  ([db pattern lookup-ref]
   (sg.re-frame/pull db pattern lookup-ref)))

;;
;; * Re-frame x Mapgraph
;;

(re-frame/reg-sub-raw
 :q
 (fn [db [_ pattern ?lookup-ref]]
   (if ?lookup-ref
     (pull-sub* db pattern ?lookup-ref)
     (pull-sub* db pattern))))
