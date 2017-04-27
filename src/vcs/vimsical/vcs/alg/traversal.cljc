(ns vimsical.vcs.alg.traversal
  (:require
   [clojure.spec :as s]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.data.indexed.vector :as indexed]
   [vimsical.vcs.data.splittable :as splittable]
   [vimsical.vcs.state.branches :as state.branches]))

;; * Internal

;; Can never remember those!
(def asc -1)
(def desc 1)

(defn reduce-tree
  "Depth-first reduction of a tree.

  `rec` is a fn of `tree` that should return the recursive part of the tree or
  nil to stop the recursion and start bracktracking.

  `f` is a reducing fn of `acc` and a `tree` node"
  [rec f acc tree]
  (letfn [(reduce-tree' [acc tree]
            (reduce-tree rec f acc tree))
          (rec' [acc rec-tree]
            (reduce reduce-tree' acc rec-tree))]
    (if-some [rec-tree (rec tree)]
      (f (rec' acc rec-tree) tree)
      (f acc tree))))

(comment
  (assert
   (= [0 1 2 3 4 5]
      (reduce-tree
       :children
       (fn f [acc {:keys [id]}]
         (conj acc id))
       [] {:id        5
           :children [{:id 2 :children [{:id 1 :children [{:id 0}]}]}
                      {:id 4 :children [{:id 3}]}]}))))


(defn update-tree
  [rec rf f tree]
  (if-some [rec-tree (rec tree)]
    (f (rf tree (mapv (partial update-tree rec rf f) rec-tree)))
    (f tree)))

(comment
  (assert
   (= {:id        6
       :children [{:id 3 :children [{:id 2 :children [{:id 1}]}]}
                  {:id 5 :children [{:id 4}]}]}
      (update-tree
       :children
       (fn [node children] (assoc node :children children))
       (fn [node] (update node :id inc))
       {:id        5
        :children [{:id 2 :children [{:id 1 :children [{:id 0}]}]}
                   {:id 4 :children [{:id 3}]}]}) )))

;; * Branch comparator

(s/fdef new-comparator
        :args (s/cat :state ::comparator-state)
        :ret  (s/fspec :args (s/cat :a ::comparable :b ::comparable)
                       :ret  #{-1 0 1}))

(s/fdef new-branch-comparator
        :args (s/cat :deltas-by-branch-id ::state.branches/deltas-by-branch-id)
        ;;
        ;; NOTE this doesn't work because fspec will exercise the :args spec and
        ;; invoke the comparator with it, so the comparators args spec needs to
        ;; ensure that branches :a and :b have a valid lineage relationship.
        ;;
        ;; The way we'd solve this is by making a generator that'd output a
        ;; lineage, and make a comparable branch spec whose generator would bind
        ;; to that and pluck some values from the lineage generator
        ;;
        ;; :ret  (s/fspec :args (s/cat :a ::branch/branch :b ::branch/branch)
        ;;                :ret  #{-1 0 1})
        )

(defn new-branch-comparator
  [deltas-by-branch-id]
  (letfn [(compare-relative-depth [common-ancestor a b]
            (let [depth-a          (branch/relative-depth common-ancestor a)
                  depth-b          (branch/relative-depth common-ancestor b)
                  depth-comparison (compare depth-a depth-b)]
              (when-not (zero? depth-comparison)
                depth-comparison)))
          (branch-entry-index [deltas-by-branch-id {::branch/keys [entry-delta-id] :keys [db/id]}]
            (state.branches/index-of-delta deltas-by-branch-id id entry-delta-id))
          (compare-entry-deltas [common-ancestor a b]
            (compare
             (branch-entry-index deltas-by-branch-id a)
             (branch-entry-index deltas-by-branch-id b)))]
    (fn branch-comparator
      [a b]
      (let [common-ancestor (branch/common-ancestor a b)]
        (cond
          (branch/parent? a b) asc

          (branch/parent? b a) desc

          (some? common-ancestor)
          (or (compare-relative-depth common-ancestor a b)
              (compare-entry-deltas common-ancestor a b)
              (throw (ex-info "Entry deltas not found???" {})))

          (and (zero? (branch/depth a))
               (pos?  (branch/depth b))) asc

          (and (pos?  (branch/depth a))
               (zero? (branch/depth b))) desc

          :else
          (throw
           (ex-info
            "Can't compare branches with no master or no common ancestor, are
            the branches fully denormalized (include their ancestors
            recursively through ::branch/parent)?"
            {:branch-a a :branch-b b})))))))


;; * Branch inlining

(s/fdef inline
        :args (s/cat :deltas-by-branch-id ::state.branches/deltas-by-branch-id
                     :branches (s/every ::branch/branch))
        :ret  ::indexed/vector)

;; NOTE do a post-traversal of the branch-tree, inlining children in their
;; relative desc order (so inserting an earlier branch doesn't require keeping
;; offsets of offsets processed so far

(defn inline
  "Does a reduce-tree traversal of the branch tree, reducing over children
  branches and inlining their deltas in a single indexed vector."
  [deltas-by-branch-id branches]
  (let [cpr  (new-branch-comparator deltas-by-branch-id)
        tree (branch/branch-tree branches)
        rec  (fn [{::branch/keys [children]}]
               (sort cpr children))]
    (::insert-deltas
     (reduce-tree
      rec
      (fn [{:as    acc
            ::keys [insert-id insert-deltas]}
           {:as           branch
            :keys         [db/id]
            ::branch/keys [entry-delta-id]}]
        (let [branch-deltas (state.branches/get-deltas deltas-by-branch-id branch)]
          (if (nil? acc)
            {::insert-id     entry-delta-id
             ::insert-deltas branch-deltas}
            (let [insert-index (inc (state.branches/index-of-delta deltas-by-branch-id id insert-id))]
              {::insert-id     entry-delta-id
               ::insert-deltas (splittable/splice branch-deltas insert-index insert-deltas)}))))
      nil tree))))
