(ns vimsical.vcs.alg.traversal
  (:require
   [clojure.spec :as s]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.data.indexed.vector :as indexed]
   [vimsical.vcs.data.splittable :as splittable]
   [vimsical.vcs.state.branches :as state.branches]))

;;
;; * Tree traversals
;;

(defn walk-tree
  "Perform a depth-first walk of `tree`.
  `rec` is a fn of `tree` that should return the recursive part of the tree or
  nil to stop the recursion and start bracktracking.

  `node` is a fn that takes a tree node, the recursive part of the tree and
  returns a new node`

  `pre` and `post` are fns of a tree node applied before and after traversing
  that node."
  [rec node pre post tree]
  (letfn [(rec' [rec-tree]
            (mapv (partial walk-tree rec node pre post) rec-tree))]
    (if-some [rec-tree (rec tree)]
      (-> (pre tree)
          (node (rec' rec-tree))
          (post))
      (post (pre tree)))))


(defn reduce-tree
  "Depth-first reduction of a tree.

  `rec` is a fn of `tree` that should return the recursive part of the tree or
  nil to stop the recursion and start bracktracking.

  `rf` is a reducing fn of `acc` and a `tree` node"
  [rec rf acc tree]
  (letfn [(reduce-tree' [acc tree]
            (reduce-tree rec rf acc tree))
          (rec' [acc rec-tree]
            (reduce reduce-tree' acc rec-tree))]
    (if-some [rec-tree (rec tree)]
      (rf (rec' acc rec-tree) tree)
      (rf acc tree))))

;;
;; * Branch comparator
;;

(def asc -1)
(def desc 1)


(s/fdef new-comparator
        :args (s/cat :state ::comparator-state)
        :ret  (s/fspec :args (s/cat :a ::comparable :b ::comparable)
                       :ret  #{-1 0 1}))

(s/fdef new-branch-comparator
        :args (s/cat :deltas-by-branch-uid ::state.branches/deltas-by-branch-uid)
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
  [deltas-by-branch-uid]
  (letfn [(compare-relative-depth [common-ancestor a b]
            (let [depth-a          (branch/relative-depth common-ancestor a)
                  depth-b          (branch/relative-depth common-ancestor b)
                  depth-comparison (compare depth-a depth-b)]
              (when-not (zero? depth-comparison)
                depth-comparison)))
          (branch-entry-index [deltas-by-branch-uid {::branch/keys [branch-off-delta-uid] :keys [db/uid]}]
            (state.branches/index-of-delta deltas-by-branch-uid uid branch-off-delta-uid))
          (compare-entry-deltas [common-ancestor a b]
            (compare
             (branch-entry-index deltas-by-branch-uid a)
             (branch-entry-index deltas-by-branch-uid b)))]
    (fn branch-comparator
      [a b]
      (try
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
              {:branch-a a :branch-b b}))))
        (catch #?(:clj Throwable :cljs :default) t
            (throw (ex-info "Branch comparison error" {:t t :a a :b b})))))))

;;
;; * Branch inlining
;;

(defn child-has-branch-off-delta-uid?
  [{::branch/keys [parent branch-off-delta-uid]}]
  (or (nil? parent) (some? branch-off-delta-uid)))

(s/def ::branch (s/and ::branch/branch child-has-branch-off-delta-uid?))
(s/def ::branches (s/every ::branch))

(s/fdef inline
        :args (s/cat :deltas-by-branch-uid ::state.branches/deltas-by-branch-uid
                     :branches ::branches)
        :ret  ::indexed/vector)

;; NOTE do a post-traversal of the branch-tree, inlining children in their
;; relative desc order (so inserting an earlier branch doesn't require keeping
;; offsets of offsets processed so far

(defn inline
  "Perform a depth-first walk over the branch tree and splices the children's
  deltas into its parent's deltas."
  [deltas-by-branch-uid branches]
  (let [cpr  (new-branch-comparator deltas-by-branch-uid)
        tree (branch/branch-tree branches)
        rec  (fn [{::branch/keys [children]}]
               (->> children not-empty (sort cpr)))
        node (fn [branch children]
               (assoc branch ::branch/children children))]
    (::branch/deltas
     (walk-tree
      rec node
      (fn pre [branch]
        (assoc branch ::branch/deltas
               (state.branches/get-deltas deltas-by-branch-uid branch)))
      (fn post [{:keys [db/uid] ::branch/keys [deltas children] :as branch}]
        (let [children-deltas    (mapv ::branch/deltas children)
              branch-off-ids     (mapv ::branch/branch-off-delta-uid children)
              branch-off-indexes (mapv
                                  (fn [delta-uid]
                                    ;; We want to insert _after_ the entry delta
                                    (inc (state.branches/index-of-delta deltas-by-branch-uid uid delta-uid)))
                                  branch-off-ids)
              splits             (splittable/splits deltas branch-off-indexes)
              deltas+splits      (splittable/interleave splits children-deltas)
              deltas'            (reduce splittable/append deltas+splits)]
          (assoc branch ::branch/deltas deltas')))
      tree))))
