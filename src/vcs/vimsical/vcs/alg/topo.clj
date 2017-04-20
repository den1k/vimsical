(ns vimsical.vcs.alg.topo
  (:refer-clojure :exclude [sort sorted?])
  (:require
   [clojure.set :as set]
   [clojure.spec :as s]
   [vimsical.common.coll :as coll]
   [vimsical.common.core :refer [=by]]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.delta :as delta]))


(defn sort
  "Sort `deltas` in topological order, running time is linear to input size.
  The sort guarantees that no delta will appear before its previous delta.
  NOTE even though the sort is deterministic for any given input, the output is
  dependent on the ordering of the input, so for example
  (let [s1 (shuffle my-delta-seq)
        s2 (shuffle my-delta-seq)
        s1-sorted (topo-sort s1)
        s2-sorted (topo-sort s2)]
    (= s1-sorted s2-sorted))
  ...could be true, or false.
  However `topo-sorted?` returns true for both."
  [deltas]
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
  (let [by-id      (coll/index-by :id deltas)
        by-prev-id (group-by :prev-id deltas)]
    (letfn [(last-deltas [by-id by-prev-id]
              (mapv #(get by-id %)
                    (set/difference
                     (set (keys by-id))
                     (set (keys by-prev-id)))))
            (next-deltas [by-prev-id {:keys [id]}]
              (get by-prev-id id))
            (prev-delta [by-id {:keys [prev-id]}]
              (get by-id prev-id))
            (dissoc!-delta [by-id! {:keys [id] :as delta}]
              (dissoc! by-id! id))
            (dissoc!-prev-delta [m! {:keys [prev-id] :as delta}]
              (let [pred #(=by :id delta %)
                    v    (get m! prev-id)
                    v'   (remove pred v)]
                (if (seq v')
                  (assoc! m! prev-id v')
                  (dissoc! m! prev-id))))]
      (loop [by-id!         (transient by-id)
             by-prev-id!    (transient by-prev-id)
             ordered!       (transient [])
             [delta & more] (last-deltas by-id by-prev-id)]
        (if (nil? delta)
          (reverse (persistent! ordered!))
          (let [prev-delta   (prev-delta by-id! delta)
                by-id!'      (dissoc!-delta by-id! delta)
                by-prev-id!' (dissoc!-prev-delta by-prev-id! delta)
                more'        (if (nil? (next-deltas by-prev-id!' prev-delta))
                               (conj more prev-delta)
                               more)]
            (recur by-id!'
                   by-prev-id!'
                   (conj! ordered! delta)
                   more')))))))



(s/fdef sorted?
        :args (s/cat :deltas (s/every ::delta/delta))
        :ref  boolean?)

(defn sorted?
  [[delta & deltas]]
  (letfn [(kfn [{:keys [id]}] id)
          (prev-kfn [{:keys [prev-id]}] prev-id)
          (seen? [ks-set delta]
            (ks-set (prev-kfn delta)))]
    (boolean
     (reduce
      (fn [ks-set delta]
        (if (seen? ks-set delta)
          (conj! ks-set (kfn delta))
          (reduced false)))
      (transient #{(kfn delta)}) deltas))))
