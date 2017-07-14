(ns vimsical.vcs.state.chunk
  (:require
   [clojure.data.avl :as avl]
   [clojure.spec.alpha :as s]
   [vimsical.common.util.core :as util]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.delta :as delta]
   [vimsical.vcs.file :as file]
   [vimsical.vcs.data.splittable :as splittable]))

;;
;; * Spec
;;

(s/def ::uid uuid?)
(s/def ::depth nat-int?)
(s/def ::count pos-int?)
(s/def ::relative-time nat-int?)
(s/def ::deltas-by-relative-time
  (s/every-kv ::relative-time ::delta/delta :kind sorted? :into (avl/sorted-map)))
(s/def ::duration nat-int?)
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
    (s/and
     (s/every ::delta/delta)
     same-branch?
     same-file?
     vector?)))

(s/def ::chunk
  (s/keys :req [::uid
                ::depth
                ::count
                ::deltas
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
        :args (s/cat :deltas ::deltas :offset (s/? number?))
        :ret  (s/tuple ::duration ::deltas-by-relative-time))

(defn new-deltas-by-relative-time
  "Return an avl map where each delta is assoc'd to the sum of the previous
  deltas' pad values + the delta's own pad value."
  ([deltas] (new-deltas-by-relative-time deltas 0))
  ([deltas offset]
   (reduce
    (fn [[t deltas] {:keys [pad] :as delta}]
      (let [t' (+ ^long t ^long pad)]
        [t' (assoc deltas t' delta)]))
    [offset (avl/sorted-map)] deltas)))

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
    (cond-> {::uid                     uid
             ::deltas                  (vec deltas)
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


(def ^:private assoc-deltas-by-relative-time
  (fnil assoc (avl/sorted-map)))

(s/fdef add-delta
        :args (s/and (s/cat :chunk ::chunk :delta ::delta/delta) (fn [{:keys [chunk delta]}] (conj? chunk delta)))
        :ret  ::chunk)

(defn add-delta
  "Update `chunk` by adding `delta` to `::deltas-by-relative-time`, adding the
  delta's `:pad` to the chunks' `::duration`, moving the `::delta-end-uid` and
  setting the `::delta-start-uid` if it was previously nil."
  [{::keys [duration count delta-start-uid deltas deltas-by-relative-time] :as chunk} {:keys [uid pad] :as delta}]
  (assert (pos? pad) "Can't add a single delta that's part of a paste, use add-deltas instead")
  (let [duration' (+ ^long duration ^long  pad)]
    (cond-> (assoc chunk
                   ::delta-end-uid uid
                   ::count (inc count)
                   ::duration duration'
                   ::deltas (conj deltas delta)
                   ::deltas-by-relative-time (assoc-deltas-by-relative-time deltas-by-relative-time duration' delta))
      (nil? delta-start-uid) (assoc ::delta-start-uid uid))))

(defn add-deltas
  [{::keys [duration count delta-start-uid deltas deltas-by-relative-time] :as chunk} deltas']
  (let [{first-uid :uid}                     (first deltas')
        {last-uid :uid}                      (peek deltas')
        [duration' deltas-by-relative-time'] (new-deltas-by-relative-time deltas' duration)
        coll-count                           #?(:cljs cljs.core/count :clj clojure.core/count)]
    (cond-> (assoc chunk
                   ::delta-end-uid last-uid
                   ::count (+ count (coll-count deltas'))
                   ::duration duration'
                   ::deltas (into deltas deltas')
                   ::deltas-by-relative-time (merge deltas-by-relative-time deltas-by-relative-time'))
      (nil? delta-start-uid) (assoc ::delta-start-uid first-uid))))

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
  [{::keys [count depth delta-branch-off-uid deltas branch-start? branch-end?]} uuid-fn index]
  (let [[left-deltas right-deltas] (splittable/split deltas index)
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

(s/fdef last-entry :args (s/cat :chunk ::chunk) :ret ::entry)

(defn last-entry
  [{::keys [deltas-by-relative-time]}]
  (avl/nearest
   deltas-by-relative-time
   <= #?(:clj  Integer/MAX_VALUE :cljs js/Number.MAX_SAFE_INTEGER)))

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
