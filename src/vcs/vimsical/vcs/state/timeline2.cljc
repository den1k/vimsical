(ns vimsical.vcs.state.timeline2
  "
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
   [clojure.spec :as s]
   [clojure.data.avl :as avl]
   [vimsical.common.util.core :as util]
   [vimsical.vcs.alg.traversal :as traversal]
   [vimsical.vcs.data.indexed.vector :as indexed]
   [vimsical.vcs.data.splittable :as splittable]
   [vimsical.vcs.state.chunk :as chunk]
   [vimsical.vcs.state.branches :as state.branches]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.delta :as delta]))

;; * Spec

(s/def ::absolute-time nat-int?)
(s/def ::index nat-int?)
(s/def ::uuid-gen ifn?)


;; * Denormalizations used for lookups

(s/def ::chunks-by-branch-id
  (s/every-kv ::branch/id (s/every ::chunk/chunk :kind vector?)))

;; XXX more efficient way to get the depth
(defn add-delta-to-chunks-by-branch-id
  [chunks-by-branch-id
   branches
   uuid-gen
   {:keys [branch-id] :as delta}]
  (let [branch (util/ffilter
                (partial util/=by identity :db/id branch-id)
                branches)
        depth  (branch/depth branch)]
    (letfn [(conj-onto-last-chunk? [chunks delta]
              (and (some? chunks) (chunk/conj? (peek chunks) delta)))
            (first-chunk-in-branch? [chunks-by-branch-id {:keys [branch-id] :as delta}]
              (nil? (get chunks-by-branch-id branch-id)))
            (conj-onto-last-chunk [chunks delta]
              (let [last-index (dec (count chunks))]
                (update chunks last-index chunk/add-delta delta)))
            (update-chunks [chunks uuid-gen delta]
              (cond
                (conj-onto-last-chunk? chunks delta)
                (conj-onto-last-chunk chunks delta)
                :else
                (let [branch-off? (first-chunk-in-branch? chunks-by-branch-id delta)
                      new-chunk   (chunk/new-chunk (uuid-gen) depth [delta] branch-off?)
                      f           (fnil conj [])]
                  (f chunks new-chunk))))]
      (update chunks-by-branch-id branch-id update-chunks uuid-gen delta))))


;; * Timeline state

(s/def ::chunks-by-absolute-start-time
  (s/every-kv ::absolute-time ::chunk/chunk :kind sorted? :into (avl/sorted-map)))

(s/def ::timeline
  (s/keys :req [::chunks-by-branch-id ::chunks-by-absolute-start-time]))

(def empty-timeline
  {::chunks-by-branch-id           {}
   ::chunks-by-absolute-start-time (avl/sorted-map)})

(s/assert* ::timeline empty-timeline)


;; * Internal

(s/def ::branch/chunks (s/every ::chunk/chunk))

(s/def ::branch-chunks-by-delta-start-index (s/every-kv ::index ::chunk/chunk :kind sorted? :into (avl/sorted-map)))

(defn- chunks-by-delta-start-index
  [chunks]
  (::m
   (reduce
    (fn [{::keys [cnt m]} {::chunk/keys [count] :as chunk}]
      {::cnt (+ cnt count)
       ::m   (assoc m cnt chunk)})
    {::cnt 0 ::m (avl/sorted-map)} chunks)))

(s/fdef branch-chunks-by-delta-start-index
        :args (s/cat :branch ::branch/branch)
        :ret  ::branch-chunks-by-delta-start-index)

(defn- branch-chunks-by-delta-start-index
  [{:keys [db/id] ::branch/keys [chunks]}]
  (chunks-by-delta-start-index chunks))

(s/def ::branch-children-chunks-by-delta-branch-off-indexes (s/every-kv ::index (s/every ::chunk/chunk) :kind sorted? :into (avl/sorted-map)))

(s/fdef branch-children-chunks-by-delta-branch-off-indexes
        :args (s/cat :deltas-by-branch-id ::state.branches/deltas-by-branch-id
                     :branch ::branch/branch)
        :ret ::branch-children-chunks-by-delta-branch-off-indexes)

(defn branch-children-chunks-by-delta-branch-off-indexes
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
    (into (avl/sorted-map) (zipmap branch-off-indexes children-chunks))))

(s/fdef split-branch-chunks-by-delta-index
        :args (s/cat :branch-chunks-by-delta-start-index ::branch-chunks-by-delta-start-index
                     :index ::index)
        :ret (s/tuple ::branch-chunks-by-delta-start-index
                      (s/tuple ::index ::chunk/chunk)
                      ::branch-chunks-by-delta-start-index))

;; (defn split-branch-chunks-by-delta-index
;;   [branch-chunks-by-delta-start-index index]
;;   (let [[left idx+chunk right :as split] (avl/split-key index branch-chunks-by-delta-start-index)]
;;     (if idx+chunk
;;       split
;;       (let [[idx chunk] (last left)
;;             left'       (dissoc left idx)]
;;         [left' [idx chunk] right]))))


(s/fdef slice-branch-chunks-by-delta-index
        :args (s/cat :branch-chunks-by-delta-start-index ::branch-chunks-by-delta-start-index
                     :indexes (s/every ::index)
                     :uuid-gen ::uuid-gen)
        :ret  ::branch-chunks-by-delta-start-index)

(defn slice-branch-chunks-by-delta-index
  [branch-chunks-by-delta-start-index indexes uuid-gen]
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
               relative-split-index     (- index chunk-index)
               [chunk-left chunk-right] (chunk/split-at-delta-index chunk uuid-gen relative-split-index)]
           (assoc branch-chunks-by-delta-start-index
                  chunk-index chunk-left
                  index chunk-right)))))
   branch-chunks-by-delta-start-index indexes))

(s/fdef inlined-chunks-seq
        :args (s/cat :branch-chunks-by-delta-start-index ::branch-chunks-by-delta-start-index
                     :branch-children-chunks-by-delta-branch-off-indexes ::branch-children-chunks-by-delta-branch-off-indexes
                     :uuid-gen ::uuid-gen)
        :ret (s/every ::chunk/chunk))

(defn inlined-chunks-seq
  [branch-chunks-by-delta-start-index
   branch-children-chunks-by-delta-branch-off-indexes
   uuid-gen]
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
         (slice-branch-chunks-by-delta-index branch-chunks-by-delta-start-index slice-indexes uuid-gen)
         branch-children-chunks-by-delta-branch-off-indexes))))))

#_
(defn inlined-chunks-seq
  [branch-chunks-by-delta-start-index
   branch-children-chunks-by-delta-branch-off-indexes
   uuid-gen]
  (letfn [(offset-indexed-map [m offset]
            (reduce-kv
             (fn [m n v]
               (assoc m (+ offset n) v))
             (empty m) m))
          (child-chunks-last-delta-index [chunks]
            (max 0 (dec (reduce + (map ::chunk/count chunks)))))
          (branch-split-last-delta-index [chunks]
            (max 0 (dec (reduce-kv (fn [cnt n _] (+ cnt n)) 0 chunks))))]
    (vals
     (reduce-kv
      (fn [branch-chunks-by-delta-start-index
           delta-branch-off-index
           child-chunks]
        (s/assert* ::branch-chunks-by-delta-start-index branch-chunks-by-delta-start-index)
        (let [[left-chunks
               [chunk-index chunk]
               right-chunks]       (split-branch-chunks-by-delta-index branch-chunks-by-delta-start-index delta-branch-off-index)
              relative-split-index (- delta-branch-off-index chunk-index)]
          (cond
            ;; Nothing to the left, push child then right-chunks
            (nil? chunk)
            (merge
             left-chunks
             (chunks-by-delta-start-index child-chunks)
             (offset-indexed-map right-chunks (child-chunks-last-delta-index child-chunks)))

            ;; The child-chunks follow the found chunk, no need to split
            (<= (::chunk/count chunk) relative-split-index)
            (let [child-chunks-map (some-> child-chunks
                                           (chunks-by-delta-start-index)
                                           (offset-indexed-map (branch-split-last-delta-index left-chunks)))
                  right-chunks     (some-> right-chunks
                                           not-empty
                                           (offset-indexed-map (branch-split-last-delta-index child-chunks-map)))]
              (merge
               left-chunks
               child-chunks-map
               right-chunks))

            :else
            (let [[left-chunk right-chunk] (chunk/split-at-delta-index chunk uuid-gen relative-split-index)
                  left-chunk-map           (when left-chunk
                                             (offset-indexed-map {delta-branch-off-index left-chunk} (branch-split-last-delta-index left-chunks)))
                  child-chunks-map         (some-> child-chunks
                                                   not-empty
                                                   (chunks-by-delta-start-index)
                                                   (offset-indexed-map (branch-split-last-delta-index (or left-chunk-map left-chunks))))
                  right-chunk-map          (when right-chunk {(branch-split-last-delta-index child-chunks-map) right-chunk})
                  right-chunks-map         (offset-indexed-map right-chunks (branch-split-last-delta-index (or right-chunk-map child-chunks-map)))]
              (merge
               left-chunks
               left-chunk-map
               child-chunks-map
               right-chunk-map
               right-chunks-map)
              ))))
      branch-chunks-by-delta-start-index branch-children-chunks-by-delta-branch-off-indexes))))

(s/def chunks-by-absolute-start-time (s/every-kv ::absolute-time ::chunk/chunk))

(s/fdef chunks-by-absolute-start-time
        :args (s/cat :chunks (s/every ::chunk/chunk))
        :ret  ::chunks-by-absolute-start-time)

(defn- chunks-by-absolute-start-time
  [chunks]
  (::m
   (reduce
    (fn [{::keys [m t]} {::chunk/keys [duration] :as chunk}]
      (assert t)
      (assert duration)
      {::m (assoc m t chunk)
       ::t (+ t duration)})
    {::m (avl/sorted-map) ::t 0} chunks)))

(s/fdef inline-chunks
        :args (s/cat :deltas-by-branch-id ::state.branches/deltas-by-branch-id
                     :chunks-by-branch-id ::chunks-by-branch-id
                     :branches (s/every ::branch/branch)
                     :uuid-gen ::uuid-gen)
        :ret  ::chunks-by-absolute-start-time)

(defn- inline-chunks
  "Perform a depth-first walk over the branch tree and splices the children's
  deltas into its parent's deltas."
  [deltas-by-branch-id chunks-by-branch-id branches uuid-gen]
  (letfn [(with-chunks
            [{:as branch :keys [db/id] ::branch/keys [chunks]}]
            (when-some [chunks (get chunks-by-branch-id id)]
              (assoc branch ::branch/chunks chunks)))]
    (let [cpr  (traversal/new-branch-comparator deltas-by-branch-id)
          tree (branch/branch-tree branches)
          rec  (fn [{::branch/keys [children] :as branch}]
                 (some->> children (keep with-chunks) (sort cpr) (not-empty)))
          node (fn [branch children]
                 (cond-> branch
                   (seq children) (assoc ::branch/children children)))]
      (chunks-by-absolute-start-time
       (::branch/chunks
        (traversal/walk-tree
         rec node
         with-chunks
         (fn post [{:keys [db/id] ::branch/keys [chunks children] :as branch}]
           (if-not (seq children)
             (assoc branch ::branch/chunks chunks)
             (let [branch-chunks-by-delta-start-index                 (branch-chunks-by-delta-start-index branch)
                   branch-children-chunks-by-delta-branch-off-indexes (branch-children-chunks-by-delta-branch-off-indexes deltas-by-branch-id branch)
                   inlined-chunks-seq                                 (inlined-chunks-seq branch-chunks-by-delta-start-index branch-children-chunks-by-delta-branch-off-indexes uuid-gen)]
               (assoc branch ::branch/chunks inlined-chunks-seq))))
         tree))))))

;; * API

(s/fdef add-delta
        :args (s/cat :timeline ::timeline
                     :deltas-by-branch-id ::state.branches/deltas-by-branch-id
                     :branches (s/every ::branch/branch)
                     :uuid-gen ::uuid-gen
                     :delta ::delta/delta)
        :ret ::timeline)

(defn add-delta
  [{:as timeline ::keys [chunks-by-branch-id]}
   deltas-by-branch-id
   branches
   uuid-gen
   delta]
  (let [chunks-by-branch-id'           (add-delta-to-chunks-by-branch-id chunks-by-branch-id branches uuid-gen delta)
        chunks-by-absolute-start-time' (inline-chunks deltas-by-branch-id chunks-by-branch-id' branches uuid-gen)]
    (assoc timeline
           ::chunks-by-branch-id chunks-by-branch-id'
           ::chunks-by-absolute-start-time chunks-by-absolute-start-time')))


(defn delta-at-absolute-time
  [{::keys [chunks-by-absolute-start-time]} t]
  (let [[abs-time chunk] (avl/nearest chunks-by-absolute-start-time <= t)
        rel-time         (- t abs-time)]
    (chunk/delta-at-relative-time chunk rel-time)))

(s/def ::delta-path
  (s/keys :req [::chunk/id ::delta/id]))

(s/fdef next-delta-path
        :args (s/cat :timeline ::timeline :delta-path ::delta-path)
        :ret ::delta-path)

(defn next-delta-path
  [timeline delta-path]

  )
