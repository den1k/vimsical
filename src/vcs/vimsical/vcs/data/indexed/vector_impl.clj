(ns vimsical.vcs.data.indexed.vector-impl
  (:refer-clojure :exclude [vec vector vector?])
  (:require
   [clojure.spec :as s]
   [vimsical.vcs.data.splittable :as splittable]
   [clojure.core.rrb-vector :as rrb])
  (:import
   (clojure.lang PersistentHashMap)
   (clojure.core.rrb_vector.rrbt Vector)))

;; * Index

(defprotocol IIndex
  (-init [this f vals])
  (-normalize [this offset]))

(s/def ::index map?)

(defn index
  ([] {})
  ([f vals]
   (-> (index)
       (-init f vals))))

;; ** Map

(extend-type clojure.lang.IPersistentMap
  IIndex
  (-init [m f vals]
    (persistent!
     (reduce
      (fn [m [i val]]
        (assoc! m (f val) i))
      (transient (empty m)) (map-indexed clojure.core/vector vals))))
  (-normalize [m offset]
    (if (zero? ^long offset)
      m
      (persistent!
       (reduce-kv
        (fn [m v i]
          (assoc! m v (+ ^long offset ^long i)))
        (transient (empty m)) m))))

  splittable/Splittable
  (split [m idx]
    (->> (seq m)
         (sort-by second)
         (split-at idx)
         (mapv (partial into {})))))

;; ** AVL

;; (require '[clojure.data.avl :as avl])
;; (import '(clojure.data.avl AVLMap))

;; (defn avl-compare
;;   [a b]
;;   (println a "/" b)
;;   (compare (first a) (first b)))

;; (defn avl []
;;   (avl/sorted-map-by avl-compare))

;; (avl/split-at
;;  2
;;  (avl/sorted-map-by avl-compare
;;                     [1 {:id 1}] nil
;;                     [2 {:id 2}] nil
;;                     [3 {:id 3}] nil))

;; * Vector

(s/def ::vector clojure.core/vector?)

(comment
  ;; RRB is buggy?
  (defn- vector
    ([] (rrb/vector))
    ([v] (rrb/vec v))))

(def vec clojure.core/vec)
(def vector clojure.core/vector)


(splittable/split "abf" 0)
