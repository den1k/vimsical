(ns vimsical.vcs.state.timeline
  (:require
   [clojure.data.avl :as avl]
   [clojure.spec :as s]
   [vimsical.vcs.alg.traversal :as traversal]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.data.dll :as dll]
   [vimsical.vcs.data.indexed.vector :as indexed]
   [vimsical.vcs.delta :as delta]
   [vimsical.vcs.state.branches :as state.branches]
   [vimsical.vcs.state.chunks :as state.chunks]))

;; * Specs

(s/def ::time nat-int?)
(s/def ::deltas (s/every ::delta/delta))
(s/def ::branches (s/every ::branch/branch))
(s/def ::deltas-by-time (s/every-kv ::time ::delta/delta))
(s/def ::duration nat-int?)
(s/def ::chunks dll/dll?)
(s/def ::cur-chunk (s/nilable map?))
(s/def ::timeline (s/keys :req [::deltas-by-time ::duration ::chunks ::cur-chunk]))


;; * Ctor

(def empty-timeline
  {::deltas         (indexed/vector-by :id)
   ::deltas-by-time (avl/sorted-map)
   ::duration       0
   ::chunks         state.chunks/empty-chunks
   ::cur-chunk      nil})


;; * Internal

(defn- deltas-duration
  [deltas]
  (transduce (map :pad) + 0 deltas))

(s/fdef update-deltas
        :args (s/cat :timeline ::timeline :deltas ::deltas)
        :ret ::timeline)

(defn- update-chunks
  [{::keys [chunks cur-chunk] :as timeline} deltas]
  (let [chunks' (state.chunks/chunks deltas)]
    (assoc timeline ::chunks chunks')))

(s/fdef update-deltas
        :args (s/cat :timeline ::timeline :deltas-by-branch-id ::state.branches/deltas-by-branch-id :branches ::branches :deltas ::deltas)
        :ret ::timeline)

(defn- update-deltas
  [timeline deltas-by-branch-id branches deltas]
  (let [deltas-vector  (traversal/inline deltas-by-branch-id branches)
        deltas-by-time (state.chunks/deltas-by-time deltas-vector)]
    (assoc timeline
           ::deltas deltas
           ::deltas-by-time deltas-by-time)))

(defn- update-duration
  [timeline deltas]
  (update timeline ::duration + (deltas-duration deltas)))


;; * API

(s/fdef add-detlas
        :args (s/cat :timeline ::timeline
                     :deltas-by-branch-id ::state.branches/deltas-by-branch-id
                     :branches ::branches
                     :deltas ::deltas)
        :ret ::timeline)

(defn add-deltas
  [{::keys [duration chunks cur-chunk] :as timeline} deltas-by-branch-id branches deltas]
  (-> timeline
      (update-deltas deltas-by-branch-id branches deltas)
      (update-chunks deltas)
      (update-duration deltas)))

(defn delta-at-time
  [{::keys [deltas-by-time] :as timeline} time]
  ;; NOTE use chunks
  (second
   (avl/nearest deltas-by-time <= time)))
