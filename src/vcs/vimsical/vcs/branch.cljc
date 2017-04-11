(ns vimsical.vcs.branch
  (:require
   [clojure.spec :as s]
   [clojure.set :as set]
   [vimsical.common.core :as common :refer [=by some-val]]
   [vimsical.common.coll :as coll]
   [vimsical.vcs.file :as file]))


;; * Spec

(s/def ::uuid uuid?)

;; ** Attributes

(s/def ::start-delta-id (s/nilable :delta/delta-id))
(s/def ::entry-delta-id (s/nilable :delta/delta-id))
(s/def ::current-delta-id (s/nilable :delta/delta-id))
(s/def ::latest-delta-id (s/nilable :delta/delta-id))
(s/def ::latest-edit-timestamp (s/nilable pos-int?))
(s/def ::created-at inst?)

;; ** Relations

(s/def ::parent (s/nilable ::branch))
(s/def ::files (s/coll-of ::file/file))

;; (s/def ::owner :vimsical/user)
;; (s/def ::deltas (s/coll-of :delta/delta))


;; * Pointers predicates

(defn at-end? [{::keys [latest-delta-id current-delta-id]}]
  (and latest-delta-id current-delta-id (= latest-delta-id current-delta-id)))

(defn at-beginning? [{::keys [start-delta-id current-delta-id]}]
  (and start-delta-id current-delta-id (= start-delta-id current-delta-id)))


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

(defn common-ancestor
  "Return the common ancestor of the given branches if any."
  [branch-a branch-b]
  (let [lineage-a (next (lineage branch-a))
        lineage-b (next (lineage branch-b))]
    (some-val
     (fn [ancestor-a]
       (some-val (=by :db/id ancestor-a) lineage-b))
     lineage-a)))

(defn depth
  "Return the depth of the child branch from the base branch "
  [base child]
  (letfn [(pred [branch] (=by :db/id base branch))]
    (when-not (pred child)
      (some
       (fn [[i branch]]
         (when (pred branch) i))
       ;; Vectors of [n ancestor] starting at [1 parent]
       (next (map-indexed vector (lineage child)))))))

(defn in-lineage?
  "Return true if `branch?` is part of `branch`'s lineage, either its direct
  parent or an ancestor."
  ([branch branch?]
   (in-lineage? (lineage branch) branch branch?))
  ([lineage branch branch?]
   (boolean
    (some
     (partial =by :db/id branch?)
     lineage))))


;; * Topological sort

(defn topo-sort-deltas
  "Sort `deltas` in topological order, running time is linear to input size.

  The sort guarantees that no delta will appear before its previous delta.

  `branches` should be a sequence of all branches, *fully denormalized*, and is
  used to disambiguate when there are multiple deltas for the same :prev-id
  value in the sequence. In those cases the logic is to designate as the
  previous delta the one whose branch is equal to the current delta or is the
  current delta's parent branch.

  NOTE even though the sort is deterministic for any given input, the output is
  dependent on the ordering of the input, so for example

  (let [s1 (shuffle my-delta-seq)
        s2 (shuffle my-delta-seq)
        s1-sorted (topo-sort-deltas my-branches s1)
        s2-sorted (topo-sort-deltas my-branches s2)]
    (= s1-sorted s2-sorted))

  ...could be true, or false.

  However `topo-sorted-deltas?` returns true for both."
  [branches deltas]
  ;;
  ;; Implementation notes:
  ;;
  ;; This is Kahn's algorithm, described at:
  ;; https://en.wikipedia.org/wiki/Topological_sorting
  ;;
  ;; Implementation quirks:
  ;;
  ;; CLJS transients don't implement iFn
  ;; http://dev.clojure.org/jira/browse/CLJS-1743
  ;;
  (let [branches-by-uuid     (coll/index-by :db/id branches)
        by-id                (group-by ::id deltas)
        by-prev-id           (group-by ::prev-id deltas)
        ;; Should be safe to memoize this since we'll always have a relatively
        ;; small number of branches compared to the number of deltas
        memoized-in-lineage? (memoize in-lineage?)
        delta->compound-key  (juxt ::id ::branch-uuid)]
    (letfn [(delta->branch [{::keys [branch-uuid]}]
              (get branches-by-uuid branch-uuid))
            (delta-in-lineage? [delta delta?]
              (memoized-in-lineage?
               (delta->branch delta)
               (delta->branch delta?)))
            ;; Optimization to initialize the loop: we can find the last
            ;; deltas without iterating if we consider that they're the ones
            ;; that no other deltas point to
            (last-deltas [by-id by-prev-id]
              (mapcat
               #(get by-id %)
               (set/difference
                (set (keys by-id))
                (set (keys by-prev-id)))))
            (next-deltas [by-prev-id! {::keys [id] :as delta}]
              (seq
               (filter
                (fn [next-delta]
                  (delta-in-lineage? next-delta delta))
                (get by-prev-id! id))))
            (prev-delta [by-id! {::keys [prev-id] :as delta}]
              (first
               (filter
                (fn [prev-delta]
                  (delta-in-lineage? delta prev-delta))
                (get by-id! prev-id))))
            (dissoc!-delta-by [f m! {::keys [id] :as delta}]
              (let [pred #(=by delta->compound-key delta %)
                    k    (f delta)
                    v    (get m! k)
                    v'   (remove pred v)]
                (if (seq v')
                  (assoc! m! k v')
                  (dissoc! m! k))))]
      (loop [by-id!         (transient by-id)
             by-prev-id!    (transient by-prev-id)
             ordered!       (transient [])
             [delta & more] (last-deltas by-id by-prev-id)]
        (if (nil? delta)
          (reverse (persistent! ordered!))
          (let [prev-delta   (prev-delta by-id! delta)
                by-id!'      (dissoc!-delta-by :id by-id! delta)
                by-prev-id!' (dissoc!-delta-by :prev-id by-prev-id! delta)
                more'        (if (nil? (next-deltas by-prev-id!' prev-delta))
                               (conj more prev-delta)
                               more)]
            (recur by-id!'
                   by-prev-id!'
                   (conj! ordered! delta)
                   more')))))))

(defn topo-sorted-deltas?
  [branches [delta & deltas]]
  (let [branches-by-uuid (coll/index-by :db/id branches)]
    (letfn [(delta->branch [{::keys [branch-uuid]}]
              (branches-by-uuid branch-uuid))
            (kfn [{::keys [id branch-uuid]}]
              {:id id ::branch-uuid branch-uuid})
            (prev-kfn [{::keys [prev-id branch-uuid]}]
              {:id prev-id ::branch-uuid branch-uuid})
            (pprev-kfn [{::keys [prev-id] :as delta}]
              (let [parent-branch-uuid (-> delta delta->branch ::parent :db/id)]
                {:id prev-id ::branch-uuid parent-branch-uuid}))
            (seen? [ks-set delta]
              (or (ks-set (prev-kfn delta))
                  (ks-set (pprev-kfn delta))))]
      (boolean
       (reduce
        (fn [ks-set delta]
          (if (seen? ks-set delta)
            (conj! ks-set (kfn delta))
            (reduced false)))
        (transient #{(kfn delta)}) deltas)))))

