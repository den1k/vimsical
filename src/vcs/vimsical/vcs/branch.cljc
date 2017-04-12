(ns vimsical.vcs.branch
  (:require
   [clojure.spec :as s]
   [clojure.set :as set]
   [vimsical.common.core :as common :refer [=by some-val]]
   [vimsical.vcs.file :as file]
   [vimsical.vcs.delta :as delta]))


;; * Spec

(s/def ::branch
  (s/keys :req [:db/id]
          :opt [::parent ::files]))


;; ** Attributes

(s/def :db/id uuid?)
(s/def ::name (s/nilable string?))
(s/def ::start-delta-id (s/nilable ::delta/id))
(s/def ::entry-delta-id (s/nilable ::delta/id))
(s/def ::created-at inst?)

;; ** Relations

(s/def ::parent (s/nilable ::branch))
(s/def ::files (s/coll-of ::file/file))


;; * Lineage

(defn lineage
  "Return a seq of that `branch`'s lineage, starting with itself,
  then its direct parent, etc all the way to master.

  Expects branch to be *fully denormalized*, i.e. each branch contains its
  parent recursively."
  ([branch] (lineage [branch] branch))
  ([acc {::keys [parent] :as branch}]
   (if (nil? parent)
     acc
     (recur (conj acc parent) parent))))

(defn ancestors
  "Return a seq of that `branch`'s ancestors, starting with parent.

  Expects branch to be *fully denormalized*, i.e. each branch contains its
  parent recursively."
  [branch] (not-empty (lineage [] branch)))

(defn common-ancestor
  "Return the common ancestor of the given branches if any."
  [branch-a branch-b]
  (let [ancestors-a (ancestors branch-a)
        ancestors-b (ancestors branch-b)]
    (some-val
     (fn [ancestor-a]
       (some-val (=by :db/id ancestor-a) ancestors-b))
     ancestors-a)))

(defn depth
  "Return the count of ancestors "
  ^long [branch] (count (ancestors branch)))

(defn relative-depth
  "Return the count of ancestors between `base` and `child`. Returns 0 is base
  and child are equal, nil if they have no common ancestor."
  ([base child]
   (let [pred (=by :db/id base)]
     (if (pred child)
       0
       (some
        (fn [[i branch]]
          (when (pred branch) i))
        ;; Vectors of [n ancestor] starting at [1 parent]
        (next (map-indexed vector (lineage child))))))))

(defn in-lineage?
  "Return true if `other` is part of `branch`'s lineage, either its direct
  parent or an ancestor."
  ([branch other]
   (in-lineage? (lineage branch) branch other))
  ([lineage branch other]
   (boolean
    (some
     (=by :db/id other)
     lineage))))

