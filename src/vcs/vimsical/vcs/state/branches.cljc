(ns vimsical.vcs.state.branches
  "Keep track of the deltas for a branch"
  (:require
   [clojure.spec :as s]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.alg.topo :as topo]
   [vimsical.vcs.data.indexed.vector :as indexed]
   [vimsical.vcs.data.splittable :as splittable]
   [vimsical.vcs.delta :as delta]))


;; * Spec

(s/def ::deltas (s/and ::indexed/vector (s/every ::delta/delta) topo/sorted?))
(s/def ::deltas-by-branch-id (s/every-kv ::branch/id ::deltas))

(def empty-deltas-by-branch-id {})


;; * Internal

(defn- new-vector
  ([] (indexed/vector-by :id))
  ([deltas] (indexed/vec-by :id deltas)))

(defn- update-deltas
  [deltas delta]
  (splittable/append deltas (new-vector [delta])))

(defn- update-deltas-by-branch-id
  [deltas-by-branch-id {:keys [branch-id] :as delta}]
  {:pre [branch-id]}
  (update deltas-by-branch-id branch-id (fnil update-deltas (new-vector)) delta))


;; * API

(s/fdef add-deltas
        :args (s/cat :deltas-by-branch-id ::deltas-by-branch-id :deltas (s/every ::delta/delta))
        :ret ::deltas-by-branch-id)

(defn add-deltas
  [deltas-by-branch-id deltas]
  (reduce update-deltas-by-branch-id deltas-by-branch-id deltas))

(s/fdef get-deltas
        :args (s/cat :deltas-by-branch-id ::deltas-by-branch-id
                     :branch (s/or :branch ::branch/branch :uuid uuid?))
        :ret  ::deltas)

(defn get-deltas
  [deltas-by-branch-id branch-or-branch-id]
  (cond->> branch-or-branch-id
    (map? branch-or-branch-id) (:db/id)
    true                       (get deltas-by-branch-id)))

(s/fdef index-of-delta
        :args
        (s/or :delta  (s/cat :deltas-by-branch-id ::deltas-by-branch-id :delta ::delta/delta)
              :params (s/cat :deltas-by-branch-id ::deltas-by-branch-id :branch-id ::branch/id :delta-id ::delta/prev-id))
        :ret  (s/nilable number?))

(defn index-of-delta
  ([deltas-by-branch-id {:keys [branch-id id] :as delta}]
   (index-of-delta deltas-by-branch-id branch-id id))
  ([deltas-by-branch-id branch-id delta-id]
   (some-> deltas-by-branch-id
           (get-deltas branch-id)
           (indexed/index-of delta-id))))
