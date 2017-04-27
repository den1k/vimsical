(ns vimsical.vcs.state.chunks
  (:refer-clojure :exclude [chunk])
  (:require
   [clojure.data.avl :as avl]
   [vimsical.common.util.core :as util]
   [vimsical.common.uuid :refer [uuid]]
   [vimsical.vcs.data.dll :as dll]))

;; * Partitioning

(def ^:private partition-delta (juxt :file-id :branch-id))
(def ^:private partition-chunk (juxt :chunk/file-id :chunk/branch-id))
(defn- same-branch? [chunk-a chunk-b] (util/=by :chunk/branch-id chunk-a chunk-b))


;; * Sorting by time (pad sum)

(defn- deltas-duration [deltas]
  (transduce (map :pad) + 0 deltas))

(defn- add-deltas-by-time
  [deltas-by-time deltas]
  (let [dur (or (-> deltas-by-time last first) 0)]
    (first
     (reduce
      (fn [[deltas-by-time dur prev-pad] {:keys [pad] :as delta}]
        (let [time  (+ (long dur) (long pad))
              ;; HACK: in multiple events (i.e. paste or autocomplete)
              ;; only the first event is padded. Therefore later
              ;; events with a pad of 0 overwrite all previous ones.
              ;; this breaks, because the last event ends up being zero
              ;; making the duration longer than the sum of pads.
              ;; We avoid this by looking for an event keyed with the
              ;; same time as a current one and, if there is one,
              ;; use its padding.
              pad'  (if (zero? (long pad)) prev-pad pad)
              delta (assoc delta :pad pad')]
          [(assoc deltas-by-time time delta)
           time
           pad']))
      [deltas-by-time dur 0]
      deltas))))

(defn deltas-by-time
  ([] (avl/sorted-map))
  ([deltas] (add-deltas-by-time (deltas-by-time) deltas)))

(defn- last-delta-by-time [deltas-by-time]
  (-> deltas-by-time last second))

(defn- split-deltas-by-time
  [deltas-by-time time]
  (let [[left k right] (avl/split-key time deltas-by-time)]
    [(cond-> left (some? k) (merge k)) right]))

;; XXX This is inefficient but we don't want to deal with time during updates,
;; let's find a better way...

(defn- split-deltas-by-delta
  [deltas-by-time {:keys [prev-id] :as delta}]
  (letfn [(split-index [deltas-by-time id]
            (::found
             (reduce-kv
              (fn [{::keys [sum]} _ {:keys [id] :as delta}]
                (if (= prev-id id)
                  (reduced {::found sum})
                  {::sum (inc sum)}))
              {::sum 0} deltas-by-time)))]
    (avl/split-at
     (split-index deltas-by-time prev-id)
     deltas-by-time)))


;; * Ctor

(defn new-chunk [deltas]
  {:pre [(vector? deltas)]}
  (let [{:as   first-delta
         :keys [branch-id file-id]} (first deltas)
        {:as last-delta}            (peek deltas)]
    {:db/id                (uuid)
     :chunk/deltas-by-time (deltas-by-time deltas)
     :chunk/time-offset    0
     :chunk/start          (:id first-delta)
     :chunk/end            (:id last-delta)
     :chunk/file-id        file-id
     :chunk/branch-id      branch-id
     :chunk/dur            (deltas-duration deltas)}))

(def empty-chunks (dll/doubly-linked-list :db/id))

(defn chunks
  ([] (chunks []))
  ([deltas]
   (let [xf (comp
             (partition-by partition-delta)
             (map new-chunk))]
     (into empty-chunks xf deltas))))

;;; Chunk Utils

(defn- chunk-at-time
  [chunks offset]
  (let [ret (reduce (fn [dur chunk]
                      (let [chunk-dur (:chunk/dur chunk)
                            tdur      (+ dur chunk-dur)]
                        (if (>= tdur offset)
                          (reduced [chunk (- tdur offset)])
                          tdur)))
                    0
                    chunks)]
    (if (number? ret) nil ret)))

(defn time-at-chunk
  ([chunks halt-pred] (time-at-chunk chunks halt-pred nil))
  ([chunks halt-pred offset]
   (transduce
    (comp
     (util/halt-when
      halt-pred
      (fn [res in] (+ res (or offset (:chunk/dur in)))))
     (map :chunk/dur))
    + chunks)))

(defn delta-at-time
  [chunks {:chunk/keys [deltas-by-time time-offset] :as chunk} time]
  (letfn [(delta-at-time* [deltas-by-time time]
            (some-> deltas-by-time (avl/nearest <= time) second))]
    (let [time+offset (+ time-offset time)]
      (or (delta-at-time* deltas-by-time time+offset)
          (when-some [prev (dll/get-prev chunks chunk)]
            (delta-at-time chunks prev time))))))

(defn- take-chunks
  [chunks from-chunk dir]
  {:pre [(get #{:left :right} dir)]}
  (let [next-key (get {:left :prev :right :next} dir)]
    (dll/seq-from chunks from-chunk next-key)))

(defn- out-of-bounds? [chunk time]
  (not (<= 0 time (:chunk/dur chunk))))

(defn- move*
  [chunks start-chunk inner-offset offset]
  {:pre [(number? inner-offset) (number? offset)]}
  (letfn [(abs [x] (max x (- x)))]
    (let [chunk      (get chunks start-chunk)
          chunk-dur  (:chunk/dur chunk)
          sum-offset (+ inner-offset offset)]
      (if-not (out-of-bounds? chunk sum-offset)
        {:cur-chunk chunk :offset sum-offset}
        (let [forward?    (pos? sum-offset)
              move-offset (if forward?
                            (- sum-offset chunk-dur)
                            (abs sum-offset))
              chunks-seq  (rest (take-chunks chunks chunk (if forward? :right :left)))]
          (loop [[{:chunk/keys [dur] :as chunk} :as chunks'] chunks-seq offset move-offset]
            (if (> dur offset)
              {:cur-chunk chunk :offset (if forward? offset (- dur offset))}
              (if-some [more (next chunks')]
                (recur more (- offset dur))
                (if forward?
                  (let [last-chunk (peek chunks)]
                    {:cur-chunk last-chunk :offset (:chunk/dur last-chunk)})
                  (let [first-chunk (first chunks)]
                    {:cur-chunk first-chunk :offset 0}))))))))))

(defn move-to [chunks start-chunk time] (move* chunks start-chunk time 0))

;; * Chunk updates

(defn- append-deltas?
  [chunk [delta]]
  {:pre [chunk delta]}
  (and
   (= (:chunk/end chunk) (:prev-id delta))
   (= (partition-chunk chunk) (partition-delta delta))))

(defn- insert-chunk?
  [cur-chunk [delta]]
  {:pre [cur-chunk delta]}
  (and
   (= (:chunk/end cur-chunk) (:prev-id delta))
   (not= (partition-chunk cur-chunk) (partition-delta delta))))

(defn- append-deltas
  [chunks chunk deltas]
  (let [new-chunk  (-> chunk
                       (update :chunk/deltas-by-time add-deltas-by-time deltas)
                       (update :chunk/dur + (deltas-duration deltas))
                       (assoc :chunk/end (-> deltas peek :id)))
        new-chunks (dll/replace chunks chunk new-chunk)]
    {:chunks new-chunks :cur-chunk new-chunk}))

(defn- split-chunk
  [{:chunk/keys [deltas-by-time time-offset dur] :as chunk} delta]
  (let [[prev-tl af-tl]            (split-deltas-by-delta deltas-by-time delta)
        [prev-time prev-end-delta] (last prev-tl)
        prev-dur                   (- prev-time time-offset)
        prev-chunk                 (assoc chunk
                                          :db/id (uuid)
                                          :chunk/deltas-by-time prev-tl
                                          :chunk/time-offset 0
                                          :chunk/dur prev-dur
                                          :chunk/end (:id prev-end-delta))
        af-start-delta             (-> af-tl first second)
        af-dur                     (- dur prev-dur)
        af-chunk                   (assoc chunk
                                          :db/id (uuid)
                                          :chunk/deltas-by-time af-tl
                                          :chunk/time-offset prev-time
                                          :chunk/dur af-dur
                                          :chunk/start (:id af-start-delta))]
    [prev-chunk af-chunk]))

(defn- split-and-insert
  [chunks chunk [delta :as deltas]]
  (let [chunk             (get chunks chunk)
        [chunk-a chunk-b] (split-chunk chunk delta)
        new-chunk         (new-chunk deltas)
        new-chunks        (-> chunks
                              (dll/replace chunk chunk-a)
                              (dll/add-after chunk-a new-chunk)
                              (dll/add-after new-chunk chunk-b))]
    {:chunks new-chunks :cur-chunk new-chunk}))

(defn- add-first-chunk
  "Takes chunks and the very first deltas of a vims. Makes a new chunk from the
  deltas and annotates it with attributes characteristic of master branch, and
  conjes it onto chunks."
  [chunks deltas]
  (let [new-chunk (new-chunk deltas)]
    {:chunks (conj chunks new-chunk) :cur-chunk new-chunk}))

(defn- insert-chunk [chunks chunk deltas]
  (let [new-chunk (new-chunk deltas)]
    {:chunks (dll/add-after chunks chunk new-chunk) :cur-chunk new-chunk}))


;; * API

(defn add-deltas
  [chunks cur-chunk deltas]
  (cond
    (empty? chunks)                   (add-first-chunk chunks deltas)
    (append-deltas? cur-chunk deltas) (append-deltas chunks cur-chunk deltas)
    (insert-chunk? cur-chunk deltas)  (insert-chunk chunks cur-chunk deltas)
    :else->split                      (split-and-insert chunks cur-chunk deltas)))
