(ns vimsical.backend.components.delta-store.validation
  (:require
   [clojure.core.async :as a]
   [clojure.core.async.impl.protocols :as ap]
   [clojure.spec :as s]
   [net.cgrand.xforms :as x]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.delta :as delta]))

;;
;; * Spec
;;

(s/def ::delta (s/keys :req-un [::delta/uid ::delta/prev-uid ::delta/branch-uid]))
(s/def ::deltas-by-branch-uid (s/every-kv ::branch/uid ::delta))

;;
;; * Deltas validation
;;

(defn- first-delta?
  [deltas-by-branch-uid {:keys [prev-uid] :as delta}]
  (and (zero? (count deltas-by-branch-uid)) (nil? prev-uid)))

(defn- new-branch?
  [deltas-by-branch-uid {:keys [branch-uid prev-uid] :as delta}]
  (and (not-empty deltas-by-branch-uid)
       (nil? (get deltas-by-branch-uid branch-uid))
       (some? prev-uid)))

(defn- next-in-branch?
  ([deltas-by-branch-uid {:keys [branch-uid] :as delta}]
   (next-in-branch? deltas-by-branch-uid branch-uid delta))
  ([deltas-by-branch-uid branch-uid {:keys [prev-uid] :as delta}]
   (when-some [{:keys [uid] :as prev} (get deltas-by-branch-uid branch-uid)]
     (= uid prev-uid))))

(defn- next-across-branches?
  [deltas-by-branch-uid delta]
  (some
   (fn [branch-uid]
     (next-in-branch? deltas-by-branch-uid branch-uid delta))
   (keys deltas-by-branch-uid)))

(defn- validate-contiguous-xf
  [rf]
  (fn
    ([] (rf))
    ([deltas-by-branch-uid] (rf deltas-by-branch-uid))
    ([deltas-by-branch-uid {:keys [prev-uid] :as delta}]
     (if (or (next-in-branch? deltas-by-branch-uid delta)
             (new-branch? deltas-by-branch-uid delta)
             (next-across-branches? deltas-by-branch-uid delta)
             (first-delta? deltas-by-branch-uid delta))
       (rf deltas-by-branch-uid delta)
       (throw
        (ex-info "Validation error" {:last-deltas-by-branch-uid deltas-by-branch-uid :delta delta}))))))

;;
;; * API
;;

(defn group-by-branch-uid-xf
  ([] (x/by-key :branch-uid (map identity)))
  ([xform] (x/by-key :branch-uid xform)))

(defn group-by-branch-uid-rf []
  (completing (fn [m [k v]] (assoc m k v))))

(defn group-by-branch-uid
  [deltas]
  (transduce
   (group-by-branch-uid-xf)
   (group-by-branch-uid-rf)
   {} deltas))

(defn group-by-branch-uid-chan
  [buf-or-n]
  (let [in  (a/chan buf-or-n (group-by-branch-uid-xf) identity)
        out (a/reduce (group-by-branch-uid-rf) {} in)]
    (reify
      ap/ReadPort
      (take! [_ handler]
        (ap/take! out handler))
      ap/WritePort
      (put! [_ value handler]
        (ap/put! in value handler))
      ap/Channel
      (ap/close! [_]
        (a/close! in))
      (ap/closed? [_]
        (ap/closed? in)))))

(s/fdef update-deltas-by-branch-uid
        :args (s/cat :deltas-by-branch-uid ::deltas-by-branch-uid :deltas (s/every ::delta))
        :ret  ::deltas-by-branch-uid)

(defn update-deltas-by-branch-uid
  [deltas-by-branch-uid deltas]
  (transduce
   (group-by-branch-uid-xf validate-contiguous-xf)
   (group-by-branch-uid-rf)
   deltas-by-branch-uid deltas))
