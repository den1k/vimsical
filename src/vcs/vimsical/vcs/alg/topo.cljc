(ns vimsical.vcs.alg.topo
  (:refer-clojure :exclude [sort sorted?])
  (:require
   [clojure.set :as set]
   [clojure.spec.alpha :as s]
   [vimsical.common.coll :as coll]
   [vimsical.common.core :refer [=by]]
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
  (let [by-uid      (coll/index-by :uid deltas)
        by-prev-uid (group-by :prev-uid deltas)]
    (letfn [(last-deltas [by-uid by-prev-uid]
              (mapv #(get by-uid %)
                    (set/difference
                     (set (keys by-uid))
                     (set (keys by-prev-uid)))))
            (next-deltas [by-prev-uid {:keys [uid]}]
              (get by-prev-uid uid))
            (prev-delta [by-uid {:keys [prev-uid]}]
              (get by-uid prev-uid))
            (dissoc!-delta [by-id! {:keys [uid] :as delta}]
              (dissoc! by-id! uid))
            (dissoc!-prev-delta [m! {:keys [prev-uid] :as delta}]
              (let [pred #(=by :uid delta %)
                    v    (get m! prev-uid)
                    v'   (remove pred v)]
                (if (seq v')
                  (assoc! m! prev-uid v')
                  (dissoc! m! prev-uid))))]
      (loop [by-id!         (transient by-uid)
             by-prev-uid!   (transient by-prev-uid)
             ordered!       (transient [])
             [delta & more] (last-deltas by-uid by-prev-uid)]
        (if (nil? delta)
          (reverse (persistent! ordered!))
          (let [prev-delta    (prev-delta by-id! delta)
                by-id!'       (dissoc!-delta by-id! delta)
                by-prev-uid!' (dissoc!-prev-delta by-prev-uid! delta)
                more'         (if (nil? (next-deltas by-prev-uid!' prev-delta))
                                (conj more prev-delta)
                                more)]
            (recur by-id!'
                   by-prev-uid!'
                   (conj! ordered! delta)
                   more')))))))



(s/fdef sorted?
        :args (s/cat :deltas (s/every ::delta/delta))
        :ref  boolean?)

(defn sorted?
  [[delta & deltas]]
  (letfn [(kfn [{:keys [uid]}] uid)
          (prev-kfn [{:keys [prev-uid]}] prev-uid)
          (seen? [ks-set delta*]
            (if-some [prev-uid (prev-kfn delta*)]
              (ks-set prev-uid)
              (= delta delta*)))]
    (boolean
     (reduce
      (fn [ks-set delta]
        (if (seen? ks-set delta)
          (conj! ks-set (kfn delta))
          (reduced false)))
      (transient #{(kfn delta)}) deltas))))
