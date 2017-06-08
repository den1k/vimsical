(ns vimsical.vcs.sync
  (:require
   [clojure.spec.alpha :as s]
   [vimsical.vcs.core :as vcs]
   [vimsical.vcs.data.indexed.vector :as indexed]
   [vimsical.vcs.data.splittable :as splittable]
   [vimsical.vcs.state.timeline :as state.timeline]
   [vimsical.vcs.validation :as validation]))

;;
;; * Diffing local state and remote delta-by-branch-uid
;;

(defn- deltas-after-index
  [deltas index]
  (not-empty (second (splittable/split deltas (inc index)))))

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
        :ret  (s/nilable ::state.timeline/deltas-by-branch-uid))

(defn diff
  "Return a map of <branch-uid> to [deltas] of the deltas that appear in `vcs`
  _after_ the ones in `deltas-by-branch-uid`. Note that this is both exhaustive
  and fast, per branch we do a constant time lookup and a logarithmic time
  split."
  [{::vcs/keys [timeline] :as vcs} delta-by-branch-uid]
  (diff-timeline timeline delta-by-branch-uid))


(defn diff-deltas
  [vcs delta-by-branch-uid]
  (not-empty
   (apply concat (vals (diff vcs delta-by-branch-uid)))))