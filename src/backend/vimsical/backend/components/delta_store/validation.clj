(ns vimsical.backend.components.delta-store.validation
  (:require
   [clojure.spec :as s]
   [net.cgrand.xforms :as x]
   [vimsical.backend.components.delta-store.protocol :as p]))


;; * Deltas validation

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

(defn- validate-contiguous
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

(defn update-deltas-by-branch-uid
  [deltas-by-branch-uid deltas]
  (let [xf (x/by-key :branch-uid validate-contiguous)
        f  (completing (fn [m [k v]] (assoc m k v)))]
    (transduce xf f deltas-by-branch-uid deltas)))
