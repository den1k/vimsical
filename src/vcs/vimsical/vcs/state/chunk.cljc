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

(s/def ::uid uuid?)
(s/def ::depth nat-int?)
(s/def ::count pos-int?)
(s/def ::relative-time nat-int?)
(s/def ::deltas-by-relative-time
  (s/every-kv ::relative-time ::delta/delta :kind sorted? :into (avl/sorted-map)))
(s/def ::duration pos-int?)
(s/def ::delta-start-uid ::delta/uid)
(s/def ::delta-end-uid ::delta/uid)
(s/def ::delta-branch-off-uid ::delta/prev-uid)
(s/def ::branch-uid ::branch/uid)
(s/def ::file-uid ::file/uid)
(s/def ::branch-start? boolean?)
(s/def ::branch-end? boolean?)


(s/def ::deltas
  (letfn [(same-branch? [deltas]
            (every? (partial apply util/=by :branch-uid) (partition 2 deltas)))
          (same-file?   [deltas]
            (every? (partial apply util/=by :file-uid) (partition 2 deltas)))]
    (s/and (s/every ::delta/delta) same-branch? same-file?)))

(s/def ::chunk
  (s/keys :req [::uid
                ::depth
                ::count
                ::deltas-by-relative-time
                ::duration
                ::delta-start-uid
                ::delta-end-uid
                ::branch-uid
                ::file-uid]
          :opt [::delta-branch-off-uid]))

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
        :args (s/cat :uid ::uid :depth ::depth :deltas ::deltas :branch-off? boolean?)
        :ret ::chunk)

(defn new-chunk
  [uid depth deltas branch-off?]
  (let [{:keys
         [branch-uid file-uid]
         ;; XXX faster accessors?
         prev-uid        :prev-uid
         delta-start-uid :uid}             (first deltas)
        {delta-end-uid :uid}               (last deltas)
        [duration deltas-by-relative-time] (new-deltas-by-relative-time deltas)]
    (cond-> {::uid                      uid
             ::deltas-by-relative-time deltas-by-relative-time
             ::duration                duration
             ::count                   (count deltas)
             ::depth                   depth
             ::delta-start-uid         delta-start-uid
             ::delta-end-uid           delta-end-uid
             ::branch-uid              branch-uid
             ::file-uid                file-uid}
      branch-off? (assoc ::delta-branch-off-uid prev-uid))))

;;
;; * Adding deltas
;;

(defn conj?
  [chunk delta]
  (and
   (util/=by ::delta-end-uid :prev-uid chunk delta)
   (util/=by ::file-uid :file-uid chunk delta)
   (util/=by ::branch-uid :branch-uid chunk delta)))


(s/fdef add-delta
        :args (s/cat :chunk ::chunk :delta ::delta/delta)
        :ret  ::chunk)

(defn add-delta
  "Update `chunk` by adding `delta` to `::deltas-by-relative-time`, adding the
  delta's `:pad` to the chunks' `::duration`, moving the `::delta-end-uid` and
  setting the `::delta-start-uid` if it was previously nil."
  [{::keys [duration] :as chunk} {:keys [uid pad] :as delta}]
  (if-not (conj? chunk delta)
    (throw (ex-info "Cannot conj delta onto chunk" {:delta delta :chunk chunk}))
    (let [t (+ duration pad)]
      (-> chunk
          (assoc  ::delta-end-uid uid ::duration t)
          (update ::count inc)
          (update ::delta-start-uid (fnil identity uid))
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
  [{::keys [count depth delta-branch-off-uid deltas-by-relative-time branch-start? branch-end?]} uuid-fn index]
  {:pre [(< index count)]}
  ;; XXX We should use the left and right deltas as is and keep a time offset on
  ;; the right chunk, this would avoid rebuilding the `deltas-by-relative-time`,
  ;; it is also similar to the split operation in the indexed vector.
  (let [[left-deltas right-deltas] (mapv vals (avl/split-at index deltas-by-relative-time))
        left-chunk                 (when (seq left-deltas)
                                     (with-bounds
                                       (new-chunk (uuid-fn) depth left-deltas (some? delta-branch-off-uid))
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
