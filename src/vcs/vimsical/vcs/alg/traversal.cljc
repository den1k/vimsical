(ns vimsical.vcs.alg.traversal
  (:require
   [clojure.spec :as s]
   [vimsical.vcs.data.indexed.vector :as indexed.vector]
   [vimsical.vcs.state.vims.branches.delta-index :as delta-index]
   [vimsical.vcs.branch :as branch]))


;; * Spec

(s/def ::comparison-result #{-1 0 1})

(s/def ::branch-comparator
  (s/fspec :args (s/cat :a ::branch/branch :b ::branch/branch) :ret  ::comparison-result))


;; * Internal

;; Can never remember those...
(def ^:private asc -1)
(def ^:private desc 1)


;; * Branch comparator

(s/fdef new-branch-comparator
        :args ::delta-index/delta-index
        :ret  ::branch-comparator)

(defn new-branch-comparator
  [delta-index]
  (letfn [(compare-relative-depth [common-ancestor a b]
            (let [depth-a          (branch/relative-depth common-ancestor a)
                  depth-b          (branch/relative-depth common-ancestor b)
                  ;; Deeper branches should come after shallow ones
                  depth-comparison (compare depth-b depth-a)]
              (when-not (== 0 depth-comparison)
                depth-comparison)))
          (branch-entry-index [delta-index {::branch/keys [entry-delta-id] :keys [db/id]}]
            (delta-index/index-of delta-index id entry-delta-id))
          (compare-entry-deltas [common-ancestor a b]
            (compare
             (branch-entry-index delta-index a)
             (branch-entry-index delta-index b)))]
    (fn branch-comparator
      [a b]
      (let [common-ancestor (branch/common-ancestor a b)]
        (cond
          (some? common-ancestor)
          (or (compare-relative-depth common-ancestor a b)
              (compare-entry-deltas common-ancestor a b)
              (throw (ex-info "Entry deltas not found???")))

          ;; a is master
          (and (zero? (branch/depth a))
               (pos?  (branch/depth b)))
          asc

          ;; b is master
          (and (pos?  (branch/depth a))
               (zero? (branch/depth b)))
          desc

          :else
          (throw
           (ex-info
            "Can't compare branches if no master and no common ancestor, are the
            branches fully denormalized (included their ancestors recursively
            through ::branch/parent?"
            {:branch-a a :branch-b b})))))))


;; * Branch inlining

(s/fdef inline
        :args (s/cat :delta-index ::delta-index/delta-index :branches (s/every ::branch/branch))
        :ret  ::indexed.vector/indexed-vector)

(defn inline
  [delta-index branches]
  (let [cpr (new-branch-comparator delta-index)]
    (reduce
     (fn [indexed-vector {:as           branch
                          :keys         [db/id]
                          ::branch/keys [entry-delta-id]}]
       (let [indexed-deltas (delta-index/get-deltas delta-index branch)
             index          (when entry-delta-id (delta-index/index-of delta-index id entry-delta-id))]
         (if (some? entry-delta-id)
           (indexed.vector/splice-at indexed-vector index indexed-deltas)
           (indexed.vector/concat indexed-vector indexed-deltas))))
     (indexed.vector/indexed-vector-by :id)
     (sort cpr branches))))
