(ns vimsical.vcs.state.chunk
  (:require
   [clojure.spec :as s]
   [clojure.data.avl :as avl]
   [vimsical.vcs.data.indexed.vector :as indexed]
   [vimsical.vcs.delta :as delta]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.file :as file]
   [vimsical.common.util.core :as util]))


;; * Spec

(s/def ::id uuid?)

(s/def ::branch-id ::branch/id)
(s/def ::file-id ::file/id)

(s/def ::depth nat-int?)
(s/def ::count pos-int?)
(s/def ::delta-start-id ::delta/id)
(s/def ::delta-end-id ::delta/id)
(s/def ::delta-branch-off-id ::delta/prev-id)

;; (s/def ::deltas-indexed (s/every ::delta/delta :kind ::indexed/vector))
(s/def ::deltas-by-relative-time (s/every-kv nat-int? ::delta/delta :kind sorted? :into (avl/sorted-map)))

(defn- deltas-same-branch? [deltas] (every? (partial apply util/=by :branch-id) (partition 2 deltas)))
(defn- deltas-same-file?   [deltas] (every? (partial apply util/=by :file-id) (partition 2 deltas)))

(s/def ::chunk-deltas
  (s/and
   (s/every ::delta/delta)
   deltas-same-branch?
   deltas-same-file?))

(s/fdef new-deltas-by-relative-time
        :args (s/cat :deltas ::chunk-deltas)
        :ret  (s/tuple ::duration ::deltas-by-relative-time))

(defn- new-deltas-by-relative-time
  [deltas]
  (reduce
   (fn [[t deltas] {:keys [pad] :as delta}]
     (let [t' (+ ^long t ^long pad)]
       [t' (assoc deltas t' delta)]))
   [0 (avl/sorted-map)] deltas))

(s/def ::duration pos-int?)

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


;; * API

(s/fdef new-chunk
        :args (s/cat :id ::id :depth ::depth :deltas ::chunk-deltas :branch-off? boolean?)
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

(s/fdef split-at-delta-index
        :args (s/cat :chunk ::chunk :uuid-gen ifn? :index nat-int?)
        :ret  (s/tuple (s/nilable ::chunk)
                       (s/nilable ::chunk)))

(defn split-at-delta-index
  [{::keys [count depth delta-branch-off-id deltas-by-relative-time]} uuid-gen index]
  {:pre [(< index count)]}
  (println "Split chunks" {:index index :count count :real-count (clojure.core/count deltas-by-relative-time)})
  ;; XXX This is easy and helps with correctness but it would be much more
  ;; efficient to split the underlying deltas and update the chunk
  (let [[left-deltas right-deltas] (mapv vals (avl/split-at index deltas-by-relative-time))
        left-chunk                 (when (seq left-deltas) (new-chunk (uuid-gen) depth left-deltas (some? delta-branch-off-id)))
        right-chunk                (when (seq right-deltas) (new-chunk (uuid-gen) depth right-deltas false))]
    [left-chunk right-chunk]))


(s/fdef delta-at-relative-time
        :args (s/cat :chunk ::chunk :t number?)
        :ret ::chunk)

(defn delta-at-relative-time
  [{::keys [duration deltas-by-relative-time]} t]
  {:pre [(<= t duration)]}
  (second
   (avl/nearest deltas-by-relative-time <= t)))
