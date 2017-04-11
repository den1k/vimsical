(ns vimsical.vcs.core
  (:require
   [clojure.spec :as s]
   [vimsical.vcs.indexed :as indexed]
   [vimsical.vcs.delta :as delta]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.file :as file]))


(defmulti add-deltas
  (fn [spec coll deltas] spec))

;; * Deltas

(s/def ::deltas
  (s/every ::delta/delta :kind vector?))

(defmethod add-deltas ::deltas
  [_ deltas new-deltas]
  (into (or deltas []) new-deltas))

;; * Indexed deltas

(s/def ::indexed-deltas indexed/indexed-vector?)

(defn indexed-deltas
  "Return deltas indexed by their :id"
  ([] (indexed/indexed-vector-by :id))
  ([deltas] (indexed/indexed-vector-by :id deltas)))

(defmethod add-deltas ::indexed-deltas
  [_ deltas new-deltas]
  (into (or deltas (indexed-deltas)) new-deltas))

;; * Indexed deltas by branch uuid

;; TODO how to get a better spec for it?
(s/def ::indexed-vector indexed/indexed-vector?)

(s/def ::indexed-deltas-by-branch-uuid
  (s/map-of :db/id ::indexed))

(defmethod add-deltas ::indexed-deltas-by-branch-uuid
  [_ indexed-deltas-by-branch-uuid new-deltas]
  (let [fconj (fnil conj (indexed-deltas))]
    (reduce
     (fn [acc {:keys [branch-uuid] :as delta}]
       (update acc branch-uuid fconj delta))
     indexed-deltas-by-branch-uuid new-deltas)))


;; * Branch topology


;; Find the latest deltas for all files at a given point
;;
;; * Deltas at time?
;; VS
;; * Deltas at delta?
