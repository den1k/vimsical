(ns vimsical.vcs.validation
  (:require
   [clojure.spec :as s]
   [net.cgrand.xforms :as x]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.delta :as delta]))

;;
;; * Spec
;;

(s/def ::delta (s/keys :req-un [::delta/uid ::delta/prev-uid ::delta/branch-uid]))
(s/def ::delta-by-branch-uid (s/every-kv ::branch/uid ::delta))

;;
;; * Predicate
;;

(defn- first-delta?
  [delta-by-branch-uid {:keys [prev-uid] :as delta}]
  (and (zero? (count delta-by-branch-uid)) (nil? prev-uid)))

(defn- new-branch?
  [delta-by-branch-uid {:keys [branch-uid prev-uid] :as delta}]
  (and (not-empty delta-by-branch-uid)
       (nil? (get delta-by-branch-uid branch-uid))
       (some? prev-uid)))

(defn- next-in-branch?
  ([delta-by-branch-uid {:keys [branch-uid] :as delta}]
   (next-in-branch? delta-by-branch-uid branch-uid delta))
  ([delta-by-branch-uid branch-uid {:keys [prev-uid] :as _delta}]
   (when-some [{:keys [uid] :as _prev} (get delta-by-branch-uid branch-uid)]
     (= uid prev-uid))))

(defn- next-across-branches?
  [delta-by-branch-uid delta]
  (some
   (fn [branch-uid]
     (next-in-branch? delta-by-branch-uid branch-uid delta))
   (keys delta-by-branch-uid)))

(defn- validate-contiguous-xf
  [rf]
  (fn
    ([] (rf))
    ([delta-by-branch-uid] (rf delta-by-branch-uid))
    ([delta-by-branch-uid {:keys [prev-uid] :as delta}]
     (if (or (next-in-branch? delta-by-branch-uid delta)
             (new-branch? delta-by-branch-uid delta)
             (next-across-branches? delta-by-branch-uid delta)
             (first-delta? delta-by-branch-uid delta))
       (rf delta-by-branch-uid delta)
       (throw
        (ex-info "Validation error" {:last-deltas-by-branch-uid delta-by-branch-uid :delta delta}))))))

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

(s/fdef update-delta-by-branch-uid
        :args (s/cat :deltas-by-branch-uid (s/nilable ::delta-by-branch-uid)
                     :deltas (s/nilable (s/every ::delta)))
        :ret  (s/nilable ::delta-by-branch-uid))

(defn update-delta-by-branch-uid
  [delta-by-branch-uid deltas]
  (try
    (transduce
     (group-by-branch-uid-xf validate-contiguous-xf)
     (group-by-branch-uid-rf)
     delta-by-branch-uid deltas)
    (catch #?(:clj Throwable :cljs :default) t
        (throw
         (ex-info "update-delta-by-branch-uid"
                  {:delta-by-branch-uid delta-by-branch-uid
                   :detlas              deltas
                   :ex                  t})))))
