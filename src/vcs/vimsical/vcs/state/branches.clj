(ns vimsical.vcs.state.branches
  (:require
   [clojure.spec :as s]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.data.indexed.vector :as indexed]
   [vimsical.vcs.delta :as delta]))

;; * Spec

(defn index-sorted?
  [index]
  (boolean
   (reduce
    (fn [{:keys [id] :as left}
         {:keys [prev-id] :as right}]
      (if (= id prev-id) right (reduced false)))
    (seq index))))

(s/def ::deltas (s/every ::delta/delta))
(s/def ::index (s/and ::indexed/vector ::deltas index-sorted?))
(s/def ::delta-index (s/map-of :db/id ::index))


;; * Internal

(defn- new-index
  ([] (indexed/vector-by :id))
  ([deltas] (indexed/vec-by :id deltas)))

(defn- update-index
  [index delta]
  (conj (or index (new-index)) delta))

(defn- update-delta-index
  [delta-index {:keys [branch-id] :as delta}]
  {:pre [branch-id]}
  (update delta-index branch-id update-index delta))


;; * API

(defn ^:declared add-deltas ([]) ([deltas]))

(s/fdef new-delta-index :ret ::delta-index)

(defn new-delta-index
  ([] {})
  ([deltas] (-> (new-delta-index) (add-deltas deltas))))

(s/fdef add-deltas
        :args (s/cat :delta-index ::delta-index :deltas ::deltas)
        :ret ::delta-index)

(defn add-deltas
  [delta-index deltas]
  (reduce update-delta-index delta-index deltas))

(s/fdef get-deltas
        :args (s/cat :delta-index ::delta-index
                     :branch (s/or :branch ::branch/branch :uuid uuid?))
        :ret  ::index)

(defn get-deltas
  [delta-index branch]
  (get delta-index (if (map? branch) (:db/id branch) branch)))

(s/fdef index-of
        :args
        (s/or :delta  (s/cat :delta-index ::delta-index :delta ::delta/delta)
              :params (s/cat :delta-index ::delta-index :branch-id ::branch/id :delta-id ::delta/id))
        :ret  (s/nilable number?))

(defn index-of
  ([delta-index {:keys [branch-id id] :as delta}]
   (index-of delta-index branch-id id))
  ([delta-index branch-id delta-id]
   (some-> delta-index
           (get-deltas branch-id)
           (indexed/index-of delta-id))))
