(ns vimsical.vcs.state.chunk
  (:require
   [clojure.data.avl :as avl]
   [clojure.spec :as s]
   [vimsical.common.util.core :as util]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.delta :as delta]
   [vimsical.vcs.file :as file]))

;;
;; * Spec
;;

(s/def ::id uuid?)
(s/def ::depth nat-int?)
(s/def ::count pos-int?)
(s/def ::relative-time nat-int?)
(s/def ::deltas-by-relative-time
  (s/every-kv ::relative-time ::delta/delta :kind sorted? :into (avl/sorted-map)))
(s/def ::duration pos-int?)
(s/def ::delta-start-id ::delta/id)
(s/def ::delta-end-id ::delta/id)
(s/def ::delta-branch-off-id ::delta/prev-id)
(s/def ::branch-id ::branch/id)
(s/def ::file-id ::file/id)
(s/def ::branch-start? boolean?)
(s/def ::branch-end? boolean?)


(s/def ::deltas
  (letfn [(same-branch? [deltas]
            (every? (partial apply util/=by :branch-id) (partition 2 deltas)))
          (same-file?   [deltas]
            (every? (partial apply util/=by :file-id) (partition 2 deltas)))]
    (s/and (s/every ::delta/delta) same-branch? same-file?)))

(s/def ::chunk
  (s/keys :req [::id
                ::depth
                ::count
                ::deltas-by-relative-time
                ::duration
                ::delta-start-id
                ::delta-end-id
                ::branch-id
                ::file-id]
          :opt [::delta-branch-off-id]))

(s/def ::timeline-annotations
  (s/keys :opt [::branch-start? ::branch-end?]))

(s/def ::annotated-chunk
  (s/merge ::chunk ::timeline-annotations))

;;
;; * Internal
;;

(s/fdef new-deltas-by-relative-time
        :args (s/cat :deltas ::deltas)
        :ret  (s/tuple ::duration ::deltas-by-relative-time))

(defn- new-deltas-by-relative-time
  "Return an avl map where each delta is assoc'd to the sum of the previous
  deltas' pad values + the delta's own pad value."
  [deltas]
  (reduce
   (fn [[t deltas] {:keys [pad] :as delta}]
     (let [t' (+ ^long t ^long pad)]
       [t' (assoc deltas t' delta)]))
   [0 (avl/sorted-map)] deltas))

;;
;; * Ctor
;;

(s/fdef new-chunk
        :args (s/cat :id ::id :depth ::depth :deltas ::deltas :branch-off? boolean?)
        :ret ::chunk)

(defn new-chunk
  [id depth deltas branch-off?]
  (let [{:keys
         [branch-id file-id]
         ;; XXX faster accessors?
         delta-start-id :id :as first-delta} (first deltas)
        {delta-end-id :id}                   (last deltas)
        [duration deltas-by-relative-time]   (new-deltas-by-relative-time deltas)]
    (cond-> {::id                      id
             ::deltas-by-relative-time deltas-by-relative-time
             ::duration                duration
             ::count                  (count deltas)
             ::depth                   depth
             ::delta-start-id          delta-start-id
             ::delta-end-id            delta-end-id
             ::branch-id               branch-id
             ::file-id                 file-id}
      branch-off? (assoc ::delta-branch-off-id (delta/op-id first-delta)) )))

;;
;; * Adding deltas
;;

(defn conj?
  [chunk delta]
  (and
   (util/=by ::delta-end-id :prev-id chunk delta)
   (util/=by ::file-id :file-id chunk delta)
   (util/=by ::branch-id :branch-id chunk delta)))


(s/fdef add-delta
        :args (s/cat :chunk ::chunk :delta ::delta/delta)
        :ret  ::chunk)

(defn add-delta
  "Update `chunk` by adding `delta` to `::deltas-by-relative-time`, adding the
  delta's `:pad` to the chunks' `::duration`, moving the `::delta-end-id` and
  setting the `::delta-start-id` if it was previously nil."
  [{::keys [duration] :as chunk} {:keys [id pad] :as delta}]
  (if-not (conj? chunk delta)
    (throw (ex-info "Cannot conj delta onto chunk" {:delta delta :chunk chunk}))
    (let [t (+ duration pad)]
      (-> chunk
          (assoc  ::delta-end-id id ::duration t)
          (update ::count inc)
          (update ::delta-start-id (fnil identity id))
          (update ::deltas-by-relative-time (fnil assoc (avl/sorted-map)) t delta)))))

(defn with-bounds [chunk start? end?]
  (when chunk
    (cond-> chunk
      (true? start?) (assoc ::branch-start? start?)
      (true? end?)   (assoc ::branch-end? end?))))

;;
;; * Splitting
;;

(s/fdef split-at-delta-index
        :args (s/cat :chunk ::chunk :uuid-fn ifn? :index nat-int?)
        :ret  (s/tuple (s/nilable ::chunk)
                       (s/nilable ::chunk)))

(defn split-at-delta-index
  [{::keys [count depth delta-branch-off-id deltas-by-relative-time branch-start? branch-end?]} uuid-fn index]
  {:pre [(< index count)]}
  ;; XXX This is easy and helps with correctness but it would be much more
  ;; efficient to split the underlying deltas and update the chunk
  (let [[left-deltas right-deltas] (mapv vals (avl/split-at index deltas-by-relative-time))
        left-chunk                 (when (seq left-deltas)
                                     (with-bounds
                                       (new-chunk (uuid-fn) depth left-deltas (some? delta-branch-off-id))
                                       branch-start? false))
        right-chunk                (when (seq right-deltas)
                                     (with-bounds
                                       (new-chunk (uuid-fn) depth right-deltas false)
                                       false branch-end?))]
    [left-chunk right-chunk]))


;;
;; * Entries lookups
;;

(s/def ::entry (s/tuple ::relative-time ::delta/delta))

(s/fdef first-entry :args (s/cat :chunk ::chunk) :ret ::entry)

(defn first-entry
  [{::keys [deltas-by-relative-time]}]
  (first deltas-by-relative-time))

(s/fdef entry-at-relative-time
        :args (s/cat :chunk ::chunk :test ifn? :t ::relative-time)
        :ret  (s/nilable ::entry))

(defn entry-at-relative-time
  [{::keys [deltas-by-relative-time]} test t]
  (some-> deltas-by-relative-time (avl/nearest test t)))

(s/fdef delta-at-relative-time
        :args (s/cat :chunk ::chunk :t ::relative-time)
        :ret ::delta/delta)

(defn delta-at-relative-time
  [timeline t]
  (second (entry-at-relative-time timeline <= t)))
