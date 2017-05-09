(ns vimsical.vcs.state.chunks
  (:require
   [clojure.spec :as s]
   [vimsical.common.util.core :as util]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.state.chunk :as chunk]))

;;
;; * Spec
;;

(s/def ::chunks-by-branch-id (s/every-kv ::branch/id (s/every ::chunk/chunk :kind vector?)))

(def emtpy-chunks-by-branch-id {})

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
;; * API
;;

;; XXX more efficient way to get the depth
(defn add-delta
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
