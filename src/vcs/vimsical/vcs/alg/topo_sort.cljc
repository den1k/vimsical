(ns vimsical.vcs.alg.topo-sort
  (:require
   [clojure.set :as set]
   [clojure.spec :as s]
   [vimsical.common.coll :as coll]
   [vimsical.common.core :refer [=by]]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.delta :as delta]))

;; * Specs

(defn ^:declared topo-sorted? [branches deltas])

(s/def ::deltas (s/every ::delta/delta))
(s/def ::sorted-deltas (s/and ::deltas topo-sorted?))

(s/fdef topo-sort
        ;; NOTE Can't spec args without a valid delta seq gen
        ;; :args (s/cat :branches ::branch/branch :deltas ::deltas)
        :ret  ::sorted-deltas)


;; * Topo sort

(defn topo-sort
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
        s1-sorted (topo-sort my-branches s1)
        s2-sorted (topo-sort my-branches s2)]
    (= s1-sorted s2-sorted))

  ...could be true, or false.

  However `topo-sorted?` returns true for both."
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
  (let [branches-by-id       (coll/index-by :db/id branches)
        by-id                (group-by :id deltas)
        by-prev-id           (group-by :prev-id deltas)
        ;; Should be safe to memoize this since we'll always have a relatively
        ;; small number of branches compared to the number of deltas
        memoized-in-lineage? (memoize branch/in-lineage?)
        delta->compound-key  (juxt :id :branch-id)]
    (letfn [(delta->branch [{:keys [branch-id]}]
              (get branches-by-id branch-id))
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
            (next-deltas [by-prev-id! {:keys [id] :as delta}]
              (seq
               (filter
                (fn [next-delta]
                  (delta-in-lineage? next-delta delta))
                (get by-prev-id! id))))
            (prev-delta [by-id! {:keys [prev-id] :as delta}]
              (first
               (filter
                (fn [prev-delta]
                  (delta-in-lineage? delta prev-delta))
                (get by-id! prev-id))))
            (dissoc!-delta-by [f m! {:keys [id] :as delta}]
              (let [pred (=by delta->compound-key delta)
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

(s/fdef topo-sort
        :args (s/cat :branches (s/every ::branch/branch) :deltas ::deltas)
        :ref  boolean?)

(defn topo-sorted?
  [branches [delta & deltas]]
  (let [branches-by-id (coll/index-by :db/id branches)]
    (letfn [(delta->branch [{:keys [branch-id]}]
              (branches-by-id branch-id))
            (kfn [{:keys [id branch-id]}]
              {:id id :branch-id branch-id})
            (prev-kfn [{:keys [prev-id branch-id]}]
              {:id prev-id :branch-id branch-id})
            (pprev-kfn [{:keys [prev-id] :as delta}]
              (let [parent-branch-id (-> delta delta->branch ::branch/parent :db/id)]
                {:id prev-id :branch-id parent-branch-id}))
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
