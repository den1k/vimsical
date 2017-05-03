(ns vimsical.vcs.state.timeline
  "
  TODO
  - move chunks seq fns to state.chunks

  Conceptual model:

  1. As deltas get added, we build chunks grouped by their branch

     The only partitioning that needs to happen in this step is a file partitioning.

     {:<branch-id>
       {:chunks [{:start-delta-id nil
                 :end-delta-id :<delta10>
                 ;; indexed vector
                 :deltas-indexed [<delta0> ... <delta10>]
                 ;; avl map
                 :deltas-by-relative-time {0 <delta0> ... 1000 <delta10>}}]}}

  2. Inlining

     Given a traversal (i.e. total ordering of deltas through their branches) we
     need to be able to take a base branch and a child branch, answering the
     following question:

     How does the first chunk in the child branch relate to the base branch?

     - If it points to the end of the base branch we can just append the chunks
     - If it points to a delta inside a chunk in the base branch, we need to
       split the base chunk and insert the child's chunks at that point

     For this operation to be fast we'll need access to a map of branch-id to
     indexed deltas in order to quickly find the insert or split point between the
     base and child branches.

  3. Read model

   We want to ask the timeline:

   a. What's the delta at time t?

     We can do that in log(n chunks) + log(m
     deltas in chunk) using an outer avl map that keeps chunks by their absolute
     start time, and an inner avl map that keeps track of deltas by their
     relative start time (within the chunk).

     When adding a delta this forces us to update the outer avl map, shifting
     all the chunks to the right of the insert, shifting their start time by the
     padding of the added delta.

   b. What's the next delta after <delta> (playback scheduling)?

     If we do not need the generality of addressing a delta only by its id, we
     can an efficient look up by introducing the concept of a chunk-delta which
     is basically a path into the timeline tracking both the delta and the chunk
     it belongs to.

     Since we have a total order for chunks and a total order for deltas within
     the chunks, we can easily retrieve the chunk, then the delta, if we have a
     next delta then we have a match, if not we get the first delta of the next
     chunk.
  "
  (:require
   [clojure.data.avl :as avl]
   [clojure.spec :as s]
   [vimsical.common.util.core :as util]
   [vimsical.vcs.alg.traversal :as traversal]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.delta :as delta]
   [vimsical.vcs.state.branches :as state.branches]
   [vimsical.vcs.state.chunk :as chunk]))


;; * Spec

(s/def ::absolute-time nat-int?)
(s/def ::index nat-int?)
(s/def ::uuid-fn ifn?)

;; TODO move chunks-seq code to state.chunks

;; * Denormalizations used for lookups

(s/def ::deltas-by-branch-id ::state.branches/deltas-by-branch-id)
(s/def ::chunks-by-branch-id (s/every-kv ::branch/id (s/every ::chunk/chunk :kind vector?)))

(defn annotate-chunk-start [chunk]
  (-> chunk
      (dissoc ::chunk/branch-end?)
      (assoc ::chunk/branch-start? true)))

(defn annotate-chunk-end [chunk]
  (-> chunk
      (dissoc ::chunk/branch-start?)
      (assoc ::chunk/branch-end? true)))

(defn annotate-single-chunk [chunk]
  (-> chunk
      (assoc ::chunk/branch-start? true ::chunk/branch-end? true)))

(defn remove-annotations [chunk]
  (dissoc chunk ::chunk/branch-start? ::chunk/branch-end?))

;; XXX more efficient way to get the depth
(defn add-delta-to-chunks-by-branch-id
  [chunks-by-branch-id
   branches
   uuid-fn
   {:keys [branch-id] :as delta}]
  (let [branch (util/ffilter
                (partial util/=by  identity :db/id branch-id)
                branches)
        depth  (branch/depth branch)]
    (letfn [(conj-onto-last-chunk? [chunks delta]
              (and (some? chunks) (chunk/conj? (peek chunks) delta)))
            (first-chunk-in-branch? [chunks-by-branch-id {:keys [branch-id] :as delta}]
              (nil? (get chunks-by-branch-id branch-id)))
            (conj-onto-last-chunk [chunks delta]
              (let [last-index (dec (count chunks))]
                (update chunks last-index chunk/add-delta delta)))
            (update-chunks [chunks uuid-fn delta]
              (cond
                (conj-onto-last-chunk? chunks delta) (conj-onto-last-chunk chunks delta)
                :else
                (let [branch-start? (first-chunk-in-branch? chunks-by-branch-id delta)
                      chunk'        (chunk/new-chunk (uuid-fn) depth [delta] branch-start?)
                      f             (fnil conj [])]
                  (f chunks chunk'))))
            (annotate-branch-start-and-end [chunks]
              (case (count chunks)
                0 chunks
                1 (update chunks 0 annotate-single-chunk)
                (let [first-index       0
                      last-index        (max 0 (dec (count chunks)))
                      before-last-index (max 0 (dec last-index))]
                  (cond-> chunks
                    true (update first-index annotate-chunk-start)

                    (< first-index before-last-index last-index)
                    (update before-last-index remove-annotations)

                    true (update last-index  annotate-chunk-end)))))]
      (-> chunks-by-branch-id
          (update branch-id update-chunks uuid-fn delta)
          (update branch-id annotate-branch-start-and-end)))))


;; * Timeline state

(s/def ::chunks-by-absolute-start-time
  (s/every-kv ::absolute-time ::chunk/chunk :kind sorted? :into (avl/sorted-map)))

(s/def ::duration nat-int?)

(s/def ::timeline
  (s/keys :req [::deltas-by-branch-id
                ::chunks-by-branch-id
                ::chunks-by-absolute-start-time
                ::duration]))

(def empty-timeline
  {::deltas-by-branch-id           state.branches/empty-deltas-by-branch-id
   ::chunks-by-branch-id           {}
   ::chunks-by-absolute-start-time (avl/sorted-map)
   ::duration                      0})

(s/assert* ::timeline empty-timeline)


;; * Internal

(s/def ::branch/chunks (s/every ::chunk/chunk))

(s/def ::branch-chunks-by-delta-start-index
  (s/every-kv ::index ::chunk/chunk :kind sorted? :into (avl/sorted-map)))

(defn- new-chunks-by-delta-start-index
  "Return an avl map where each chunk is assoc'd with its delta-start index."
  [chunks]
  (::m
   (reduce
    (fn [{::keys [cnt m]} {::chunk/keys [count] :as chunk}]
      {::cnt (+ cnt count) ::m (assoc m cnt chunk)})
    {::cnt 0 ::m (avl/sorted-map)} chunks)))

(s/fdef new-branch-chunks-by-delta-start-index
        :args (s/cat :branch ::branch/branch)
        :ret  ::branch-chunks-by-delta-start-index)

(defn- new-branch-chunks-by-delta-start-index
  [{:keys [db/id] ::branch/keys [chunks]}]
  (new-chunks-by-delta-start-index chunks))

(s/def ::branch-children-chunks-by-delta-branch-off-indexes
  (s/every-kv ::index (s/every ::chunk/chunk) :kind sorted? :into (avl/sorted-map)))

(s/fdef new-branch-children-chunks-by-delta-branch-off-indexes
        :args (s/cat :deltas-by-branch-id  ::deltas-by-branch-id
                     :branch ::branch/branch)
        :ret ::branch-children-chunks-by-delta-branch-off-indexes)

(defn new-branch-children-chunks-by-delta-branch-off-indexes
  "Return an avl map where each child's chunks are assoc'd in a single sequence
  with the branch-off index relative to the base branch."
  [deltas-by-branch-id {:keys [db/id] ::branch/keys [children] :as branch}]
  (let [children-chunks    (mapv ::branch/chunks children)
        branch-off-ids     (keep (comp ::chunk/delta-branch-off-id first) children-chunks)
        branch-off-indexes (mapv (fn [delta-id]
                                   ;; We want to insert _after_ the entry delta
                                   (or (some->> delta-id
                                                (state.branches/index-of-delta deltas-by-branch-id id)
                                                (inc))
                                       (throw
                                        (ex-info
                                         "branch off index not found"
                                         {:id delta-id :branch branch}))))
                                 branch-off-ids)]
    (into
     (avl/sorted-map)
     (zipmap
      branch-off-indexes
      children-chunks))))

(s/fdef slice-branch-chunks-by-delta-index
        :args (s/cat :branch-chunks-by-delta-start-index ::branch-chunks-by-delta-start-index
                     :indexes (s/every ::index)
                     :uuid-fn ::uuid-fn)
        :ret  ::branch-chunks-by-delta-start-index)

(defn- slice-branch-chunks-by-delta-index
  "Split the chunks in `branch-chunks-by-delta-start-index` according to
  `indexes`. Will not create new chunks for an index that points directly to the
  delta-start of an existing chunk in `branch-chunks-by-delta-start-index`."
  [branch-chunks-by-delta-start-index indexes uuid-fn]
  (reduce
   (fn [branch-chunks-by-delta-start-index index]
     (let [[left idx+chunk] (avl/split-key index branch-chunks-by-delta-start-index)]
       ;; If a chunk was found at our index we don't need to slice since
       ;; we'll be able to insert the child chunks in between the
       ;; branch's chunks
       (if idx+chunk
         branch-chunks-by-delta-start-index
         ;; Else our split point is inside the last chunk on the left, so
         ;; we split it and update the map
         (let [[chunk-index chunk]      (last left)
               relative-split-index     (- (long index) (long chunk-index))
               [chunk-left chunk-right] (chunk/split-at-delta-index chunk uuid-fn relative-split-index)]
           (assoc branch-chunks-by-delta-start-index
                  chunk-index chunk-left
                  index chunk-right)))))
   branch-chunks-by-delta-start-index indexes))

(s/fdef new-inlined-chunks-seq
        :args (s/cat :branch-chunks-by-delta-start-index ::branch-chunks-by-delta-start-index
                     :branch-children-chunks-by-delta-branch-off-indexes ::branch-children-chunks-by-delta-branch-off-indexes
                     :uuid-fn ::uuid-fn)
        :ret (s/every ::chunk/chunk))

(defn- new-inlined-chunks-seq
  "Return a sequence of chunks representing the in-lining of the branch's
  children chunks within its own. The implementation currently hard-codes the
  ordering to be: branch chunks left of branch-off, children chunks, branch
  chunks right of branch-off."
  [branch-chunks-by-delta-start-index
   branch-children-chunks-by-delta-branch-off-indexes
   uuid-fn]
  (let [slice-indexes (keys branch-children-chunks-by-delta-branch-off-indexes)]
    (if-not (seq slice-indexes)
      (vals branch-chunks-by-delta-start-index)
      (flatten
       (vals
        (reduce-kv
         (fn [branch-chunks-by-delta-start-index branch-off-index child-chunks]
           (let [[left [idx chunk-or-chunks] right] (avl/split-key branch-off-index branch-chunks-by-delta-start-index)
                 value                              (if (vector? chunk-or-chunks)
                                                      (into chunk-or-chunks child-chunks)
                                                      (conj child-chunks chunk-or-chunks))]
             (merge
              left
              {idx value}
              right)))
         (slice-branch-chunks-by-delta-index branch-chunks-by-delta-start-index slice-indexes uuid-fn)
         branch-children-chunks-by-delta-branch-off-indexes))))))

(s/def new-chunks-by-absolute-start-time (s/every-kv ::absolute-time ::chunk/chunk))

(s/fdef new-chunks-by-absolute-start-time
        :args (s/cat :chunks (s/every ::chunk/chunk))
        :ret  ::chunks-by-absolute-start-time)

(defn- new-chunks-by-absolute-start-time
  "Return an avl map where each chunk is assoc'd with it's absolute start time
  within `chunks`."
  [chunks]
  (::m
   (reduce
    (fn [{::keys [m t]} {::chunk/keys [duration] :as chunk}]
      (assert t)
      (assert duration)
      {::m (assoc m t chunk)
       ::t (+ (long t) (long duration))})
    {::m (avl/sorted-map) ::t 0} chunks)))

(s/fdef inline-chunks
        :args (s/cat :deltas-by-branch-id ::deltas-by-branch-id
                     :chunks-by-branch-id ::chunks-by-branch-id
                     :branches (s/every ::branch/branch)
                     :uuid-fn ::uuid-fn)
        :ret  (s/every ::chunk/chunk))

(defn inline-chunks
  "Perform a depth-first walk over the branch tree and splices the children's
  deltas into its parent's deltas. The order of the final sequence is currently
  determined by both `traversal/new-branch-comparator` and
  `new-inlined-chunks-seq`."
  [deltas-by-branch-id chunks-by-branch-id branches uuid-fn]
  (letfn [(with-chunks
            [{:as branch :keys [db/id] ::branch/keys [chunks]}]
            (when-some [chunks (get chunks-by-branch-id id)]
              (assoc branch ::branch/chunks chunks)))]
    (let [cpr  (traversal/new-branch-comparator deltas-by-branch-id)
          tree (branch/branch-tree branches)
          rec  (fn [{::branch/keys [children]}]
                 (some->> children (keep with-chunks) (sort cpr) (not-empty)))
          node (fn [branch children]
                 (cond-> branch
                   (seq children) (assoc ::branch/children children)))]
      (::branch/chunks
       (traversal/walk-tree
        rec node
        with-chunks
        (fn post [{:keys [db/id] ::branch/keys [chunks children] :as branch}]
          (if-not (seq children)
            (assoc branch ::branch/chunks chunks)
            ;; XXX see what we can cache here instead
            (let [branch-chunks-by-delta-start-index                 (new-branch-chunks-by-delta-start-index branch)
                  branch-children-chunks-by-delta-branch-off-indexes (new-branch-children-chunks-by-delta-branch-off-indexes deltas-by-branch-id branch)
                  inlined-chunks-seq                                 (new-inlined-chunks-seq branch-chunks-by-delta-start-index branch-children-chunks-by-delta-branch-off-indexes uuid-fn)]
              (assoc branch ::branch/chunks inlined-chunks-seq))))
        tree)))))


;; * API

;; ** Updates

(s/fdef add-delta
        :args (s/cat :timeline ::timeline
                     :branches (s/every ::branch/branch)
                     :uuid-fn ::uuid-fn
                     :delta ::delta/delta)
        :ret ::timeline)

(defn add-delta
  [{:as timeline ::keys [deltas-by-branch-id chunks-by-branch-id duration]} branches uuid-fn
   {:keys [pad] :as delta}]
  (let [deltas-by-branch-id'           (state.branches/add-delta deltas-by-branch-id delta)
        chunks-by-branch-id'           (add-delta-to-chunks-by-branch-id chunks-by-branch-id branches uuid-fn delta)
        inlined-chunks                 (inline-chunks deltas-by-branch-id' chunks-by-branch-id' branches uuid-fn)
        chunks-by-absolute-start-time' (new-chunks-by-absolute-start-time inlined-chunks)]
    (assoc timeline
           ::deltas-by-branch-id deltas-by-branch-id'
           ::chunks-by-branch-id chunks-by-branch-id'
           ::chunks-by-absolute-start-time chunks-by-absolute-start-time'
           ::duration (+ (long duration) (long pad)))))

(s/fdef add-deltas
        :args (s/cat :timeline ::timeline
                     :branches (s/every ::branch/branch)
                     :uuid-fn ::uuid-fn
                     :deltas (s/every ::delta/delta))
        :ret ::timeline)

(defn add-deltas
  [timeline branches uuid-fn deltas]
  (reduce
   (fn [timeline delta]
     (add-delta timeline branches uuid-fn delta))
   timeline deltas))


;; ** Queries

(def duration ::duration)

(defn delta-at-absolute-time
  [{::keys [chunks-by-absolute-start-time]} t]
  (let [[abs-time chunk] (avl/nearest chunks-by-absolute-start-time < t)
        rel-time         (- (long t) (long abs-time))]
    (chunk/delta-at-relative-time chunk rel-time)))

(s/def ::delta-path (s/tuple ::chunk/id ::delta/id))

(def chunks-by-absolute-start-time ::chunks-by-absolute-start-time)

(s/fdef next-delta-path
        :args (s/cat :timeline ::timeline :delta-path ::delta-path)
        :ret ::delta-path)
