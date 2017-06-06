(ns vimsical.vcs.state.branch-pointers
  "Keep track of the start and end deltas for each branch."
  (:require
   [clojure.spec.alpha :as s]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.delta :as delta]))

;;
;; * Spec
;;

(s/def ::start ::delta/prev-uid)
(s/def ::end ::delta/prev-uid)
(s/def ::branch-pointers (s/keys :req [::start ::end]))
(s/def ::branch-pointers-by-branch-uid (s/map-of ::branch/uid ::branch-pointers))

;;
;; * State
;;

(def empty-branch-pointers-by-branch-uid {})

(defn- update-pointers
  [{::keys [start end] :as branch-pointers} start-uid end-uid]
  (cond-> branch-pointers
    (nil? start) (assoc ::start start-uid)
    true         (assoc ::end end-uid)))

;;
;; * API
;;

(s/fdef add-delta
        :args (s/cat :bpbb ::branch-pointers-by-branch-uid ::deltas ::delta/delta)
        :ret ::branch-pointers-by-branch-uid)

(defn add-delta
  [branch-pointers-by-branch-uid {:keys [uid branch-uid] :as delta}]
  (update branch-pointers-by-branch-uid branch-uid update-pointers uid uid))

(s/fdef add-deltas
        :args (s/cat :bpbb ::branch-pointers-by-branch-uid ::deltas (s/every ::delta/delta))
        :ret ::branch-pointers-by-branch-uid)

(defn add-deltas
  [branch-pointers-by-branch-uid deltas]
  (reduce-kv
   (fn [branch-pointers-by-branch-uid branch-uid deltas]
     (let [start (-> deltas first :uid)
           end   (-> deltas last :uid)]
       (update branch-pointers-by-branch-uid branch-uid update-pointers start end )))
   branch-pointers-by-branch-uid (group-by :branch-uid deltas)))

(defn start?
  [branch-pointers-by-branch-uid {:keys [branch-uid uid] :as delta}]
  (= uid (get-in branch-pointers-by-branch-uid [branch-uid ::start])))

(defn end?
  [branch-pointers-by-branch-uid {:keys [branch-uid uid] :as delta}]
  (= uid (get-in branch-pointers-by-branch-uid [branch-uid ::end])))
