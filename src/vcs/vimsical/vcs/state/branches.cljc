(ns vimsical.vcs.state.branches
  "Keep track of the deltas for a branch"
  (:require
   [clojure.spec.alpha :as s]
   [vimsical.vcs.alg.topo :as topo]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.data.indexed.vector :as indexed]
   [vimsical.vcs.delta :as delta]
   [vimsical.vcs.data.splittable :as splittable]))

;;
;; * Spec
;;

(s/def ::deltas (s/and ::indexed/vector (s/every ::delta/delta) topo/sorted?))

(s/def ::deltas-by-branch-uid (s/every-kv ::branch/uid ::deltas))

(def empty-deltas-by-branch-uid {})

;;
;; * Internal
;;

(defn- new-vector
  ([] (indexed/vector-by :uid))
  ([deltas] (indexed/vec-by :uid deltas)))

;;
;; * API
;;

(def ^:private conj-deltas (fnil conj (new-vector)))

(s/fdef add-delta
        :args (s/cat :deltas-by-branch-uid ::deltas-by-branch-uid :deltas ::delta/delta)
        :ret ::deltas-by-branch-uid)

(defn add-delta
  [deltas-by-branch-uid {:keys [branch-uid] :as delta}]
  {:pre [branch-uid]}
  (update deltas-by-branch-uid branch-uid conj-deltas delta))

(s/fdef add-deltas
        :args (s/cat :deltas-by-branch-uid ::deltas-by-branch-uid :deltas (s/nilable (s/every ::delta/delta)))
        :ret ::deltas-by-branch-uid)

(defn add-deltas
  [deltas-by-branch-uid deltas]
  (reduce-kv
   (fn [acc branch-uid deltas]
     (let [deltas' (new-vector deltas)]
       (assoc acc branch-uid
              (if-some [prev-deltas (get acc branch-uid)]
                (splittable/append prev-deltas deltas')
                deltas'))))
   deltas-by-branch-uid
   (group-by :branch-uid deltas)))

(s/fdef get-deltas
        :args (s/cat :deltas-by-branch-uid ::deltas-by-branch-uid
                     :branch (s/or :branch ::branch/branch :uuid uuid?))
        :ret  ::deltas)

(defn get-deltas
  [deltas-by-branch-uid branch-or-branch-uid]
  (cond->> branch-or-branch-uid
    (map? branch-or-branch-uid) (:db/uid)
    true                        (get deltas-by-branch-uid)))

(s/fdef index-of-delta
        :args
        (s/or :delta  (s/cat :deltas-by-branch-uid ::deltas-by-branch-uid :delta ::delta/delta)
              :params (s/cat :deltas-by-branch-uid ::deltas-by-branch-uid :branch-uid ::branch/uid :delta-uid ::delta/prev-uid))
        :ret  (s/nilable number?))

(defn index-of-delta
  ([deltas-by-branch-uid {:keys [branch-uid uid] :as delta}]
   (index-of-delta deltas-by-branch-uid branch-uid uid))
  ([deltas-by-branch-uid branch-uid delta-uid]
   (some-> deltas-by-branch-uid
           (get-deltas branch-uid)
           (indexed/index-of delta-uid))))
