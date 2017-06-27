(ns vimsical.vcs.state.chunks
  (:require
   [clojure.spec.alpha :as s]
   [vimsical.common.util.core :as util]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.state.chunk :as chunk]
   [vimsical.vcs.delta :as delta]))

;;
;; * Spec
;;

(s/def ::chunks-by-branch-uid (s/every-kv ::branch/uid (s/every ::chunk/chunk :kind vector?)))
(s/def ::branches (s/every ::branch/branch))

(def emtpy-chunks-by-branch-uid {})

;;
;; * Branch annotations
;;

(defn- annotate-chunk-start [chunk]
  (-> chunk
      (dissoc ::chunk/branch-end?)
      (assoc ::chunk/branch-start? true)))

(defn- annotate-chunk-end [chunk]
  (-> chunk
      (dissoc ::chunk/branch-start?)
      (assoc ::chunk/branch-end? true)))

(defn- annotate-single-chunk [chunk]
  (-> chunk
      (assoc ::chunk/branch-start? true ::chunk/branch-end? true)))

(defn- remove-annotations [chunk]
  (dissoc chunk ::chunk/branch-start? ::chunk/branch-end?))

;;
;; * Helpers
;;
(def ^:private conj-chunk (fnil conj []))

(def chunks-branch-start? empty?)

(defn- conj-onto-last-chunk?
  [chunks delta]
  (and (seq chunks) (chunk/conj? (peek chunks) delta)))

(defn- conj-onto-last-chunk
  [chunks delta]
  (let [last-index (dec (count chunks))]
    (update chunks last-index chunk/add-delta delta)))

(defn- into-last-chunk
  [chunks deltas]
  (let [last-index (dec (count chunks))]
    (update chunks last-index chunk/add-deltas deltas)))

(defn- update-chunks-delta
  [chunks depth uuid-fn delta]
  (if (conj-onto-last-chunk? chunks delta)
    (conj-onto-last-chunk chunks delta)
    (let [branch-start? (chunks-branch-start? chunks)
          chunk'        (chunk/new-chunk (uuid-fn) depth [delta] branch-start?)]
      (conj-chunk chunks chunk'))))

(defn- annotate-branch-start-and-end
  [chunks]
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

        true (update last-index  annotate-chunk-end)))))

(defn branch-uid-branch
  [branches branch-uid]
  (util/ffilter
   (partial util/=by  identity :db/uid branch-uid)
   branches))

(defn branch-uid-depth
  [branches branch-uid]
  (branch/depth (branch-uid-branch branches branch-uid)))

(defn delta-branch
  [branches {:keys [branch-uid] :as delta}]
  (branch-uid-branch branches branch-uid))

(defn delta-depth
  [branches delta]
  (branch/depth (delta-branch branches delta)))

;;
;; * API
;;

(s/fdef add-delta
        :args (s/cat :chunks-by-branch-uid ::chunks-by-branch-uid
                     :branches (s/every ::branch/branch)
                     :uuid-fn ifn?
                     :delta ::delta/delta)
        :ret ::chunks-by-branch-uid)

(defn add-delta
  [chunks-by-branch-uid branches uuid-fn {:keys [branch-uid] :as delta}]
  (let [depth  (delta-depth branches delta)
        chunks (get chunks-by-branch-uid branch-uid)]
    (assoc chunks-by-branch-uid branch-uid
           (-> chunks
               (update-chunks-delta depth uuid-fn delta)
               (annotate-branch-start-and-end)))))

;; DUMB method

(s/fdef add-deltas
        :args (s/cat :chunks-by-branch-uid ::chunks-by-branch-uid
                     :branches (s/every ::branch/branch)
                     :uuid-fn ifn?
                     :deltas (s/every ::delta/delta))
        :ret ::chunks-by-branch-uid)

(defn add-deltas
  [chunks-by-branch-uid branches uuid-fn deltas]
  (reduce
   (fn [chunks-by-branch-uid delta]
     (add-delta chunks-by-branch-uid branches uuid-fn delta))
   chunks-by-branch-uid deltas))

;; Perf

(defn deltas-parition-xf
  []
  (let [vprev-uid (volatile! nil)
        vval      (volatile! true)]
    (partition-by
     (fn [{:keys [file-uid]}]
       (let [file-uid' @vprev-uid]
         (vreset! vprev-uid file-uid)
         (cond
           ;; init
           (nil? file-uid') @vval
           ;; same
           (= file-uid' file-uid) @vval
           ;; new
           :else (vswap! vval not)))))))

(s/fdef add-deltas-by-branch-uid
        :args (s/cat :chunks-by-branch-uid ::chunks-by-branch-uid
                     :branches (s/every ::branch/branch)
                     :uuid-fn ifn?
                     :deltas-by-branch-uid (s/every-kv ::branch/uid (s/every ::delta/delta)))
        :ret ::chunks-by-branch-uid)

(defn add-deltas-by-branch-uid
  [chunks-by-branch-uid branches uuid-fn deltas-by-branch-uid]
  (letfn [(new-chunks [depth branch-start? deltas-partition]
            (map
             (fn [branch-start? deltas]
               (chunk/new-chunk (uuid-fn) depth deltas branch-start?))
             (cons branch-start? (repeat false))
             deltas-partition))]
    (reduce-kv
     (fn [chunks-by-branch-uid branch-uid [delta :as deltas]]
       (let [depth              (branch-uid-depth branches branch-uid)
             deltas-partition   (into [] (deltas-parition-xf) deltas)]
         (update chunks-by-branch-uid branch-uid
                 (fn [chunks]
                   (if (conj-onto-last-chunk? chunks delta)
                     ;; add the first partition to the last chunk and make new chunks for
                     ;; the rest
                     (-> chunks
                         (into-last-chunk (first deltas-partition))
                         (into (new-chunks depth false (next deltas-partition)))
                         (annotate-branch-start-and-end))
                     (-> (or chunks [])
                         (into (new-chunks depth (chunks-branch-start? chunks) deltas-partition))
                         (annotate-branch-start-and-end)))))))
     chunks-by-branch-uid
     deltas-by-branch-uid)))

#_(defn add-deltas
    [chunks-by-branch-uid branches uuid-fn [delta :as deltas]]
    (-> chunks-by-branch-uid
        (update branch-uid update-chunks-delta uuid-fn delta)
        (update branch-uid annotate-branch-start-and-end)))
