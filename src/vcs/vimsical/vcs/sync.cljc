(ns vimsical.vcs.sync
  (:require
   [vimsical.vcs.alg.topo :as topo]
   [vimsical.vcs.delta :as delta]
   [vimsical.vcs.branch :as branch]
   [clojure.spec.alpha :as s]
   [vimsical.vcs.core :as vcs]
   [vimsical.vcs.data.indexed.vector :as indexed]
   [vimsical.vcs.data.splittable :as splittable]
   [vimsical.vcs.state.timeline :as state.timeline]
   [vimsical.vcs.validation :as validation]))

;;
;; * Spec
;;

;; NOTE This data structure is similar to vimsical.vcs.state.branches, however
;; we use plain vectors to store the deltas since we don't intend to "edit" the
;; diff later. This allows use to use a faster split method on the indexed
;; vector that returns plain vectors

(s/def ::deltas (s/and (s/every ::delta/delta) topo/sorted?))
(s/def ::deltas-by-branch-uid (s/every-kv ::branch/uid ::deltas))

;;
;; * Diffing local state and remote delta-by-branch-uid
;;

(defn- deltas-after-index
  [deltas index]
  (not-empty (second (splittable/split-vec deltas (inc (long index))))))

(defn- diff-timeline
  [{timeline-deltas-by-branch-uid ::state.timeline/deltas-by-branch-uid} delta-by-branch-uid]
  (reduce-kv
   (fn [m branch-uid deltas]
     (if-some [{:keys [uid]} (get delta-by-branch-uid branch-uid)]
       (if-some [split-index (indexed/index-of deltas uid)]
         (if-some [new-deltas (deltas-after-index deltas split-index)]
           (assoc m branch-uid new-deltas)
           m)
         m)
       (assoc m branch-uid deltas)))
   nil timeline-deltas-by-branch-uid))

(s/fdef diff
        :args (s/cat :vcs ::vcs/vcs :delta-by-branch-uid (s/nilable ::validation/delta-by-branch-uid))
        :ret  (s/nilable ::deltas-by-branch-uid))

(defn diff
  "Return a map of <branch-uid> to [deltas] of the deltas that appear in `vcs`
  _after_ the ones in `deltas-by-branch-uid`. Note that this is both exhaustive
  and fast, per branch we do a constant time lookup and a logarithmic time
  split."
  [{::vcs/keys [timeline] :as vcs} delta-by-branch-uid]
  (diff-timeline timeline delta-by-branch-uid))

(defn diff-deltas
  [vcs delta-by-branch-uid]
  (reduce (fnil into []) nil (vals (diff vcs delta-by-branch-uid))))
