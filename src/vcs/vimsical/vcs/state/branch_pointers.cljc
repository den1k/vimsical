(ns vimsical.vcs.state.branch-pointers
  "Keep track of the start and end deltas for each branch."
  (:require
   [clojure.spec :as s]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.delta :as delta]))


;; * Spec

(s/def ::start ::delta/prev-id)
(s/def ::end ::delta/prev-id)
(s/def ::branch-pointers (s/keys :req [::start ::end]))
(s/def ::branch-pointers-by-branch-id (s/map-of ::branch/id ::branch-pointers))


;; * State

(def empty-branch-pointers-by-branch-id {})


;; * API

(s/fdef add-deltas
        :args (s/cat :bpbb ::branch-pointers-by-branch-id ::deltas (s/every ::delta/delta))
        :ret ::branch-pointers-by-branch-id)

(defn add-deltas
  [branch-pointers-by-branch-id deltas]
  (reduce-kv
   (fn [branch-pointers-by-branch-id branch-id deltas]
     (let [start' (-> deltas first :id)
           end'   (-> deltas last :id)]
       (update branch-pointers-by-branch-id branch-id
               (fn [{::keys [start end] :as branch-pointers}]
                 (cond-> branch-pointers
                   (nil? start) (assoc ::start start')
                   true         (assoc ::end end'))))))
   branch-pointers-by-branch-id (group-by :branch-id deltas)))

(defn start?
  [branch-pointers-by-branch-id {:keys [branch-id id] :as delta}]
  (= id (get-in branch-pointers-by-branch-id [branch-id ::start])))

(defn end?
  [branch-pointers-by-branch-id {:keys [branch-id id] :as delta}]
  (= id (get-in branch-pointers-by-branch-id [branch-id ::end])))
