(ns vimsical.vcs.core
  (:require
   [clojure.spec :as s]
   [vimsical.vcs.data.indexed.vector :as indexed.vector]
   [vimsical.vcs.delta :as delta]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.file :as file]))


(s/def ::deltas (s/every ::delta/delta))

;; * Deltas

(s/fdef add-deltas
        :args (s/cat :deltas ::deltas :new ::deltas)
        :ret  ::deltas)

(defn add-deltas
  [deltas new]
  (into deltas new))


;; * Indexed deltas

(s/def ::indexed-deltas indexed.vector/indexed-vector?)

(s/fdef indexed-add-deltas
        :args (s/cat :index indexed.vector/indexed-vector?
                     :deltas ::deltas)
        :ret  indexed.vector/indexed-vector?)

(defn indexed-add-deltas
  [index deltas]
  (into
   (or index (indexed.vector/indexed-vector-by :id deltas))
   deltas))


;; * Indexed deltas by branch id

(s/def ::indexed-deltas (s/and indexed.vector/indexed-vector? ::deltas))
(s/def ::indexed-deltas-by-branch-id (s/map-of :db/id ::indexed-deltas))

(s/fdef index-by-branch-add-deltas
        :args (s/cat :acc    ::indexed-deltas-by-branch-id
                     :deltas ::deltas)
        :ret ::indexed-deltas-by-branch-id)

(defn index-by-branch-add-deltas
  [acc deltas]
  (let [fconj (fnil conj (indexed.vector/indexed-vector-by :id))]
    (reduce
     (fn [acc {:keys [branch-id] :as delta}]
       (update acc branch-id fconj delta))
     acc deltas)))


;; * Branch traversal

;; * File state
