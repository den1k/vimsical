(ns vimsical.vcs.data.indexed.vector-impl
  (:refer-clojure :exclude [vec vector vector?])
  (:require
   [clojure.spec :as s]
   [vimsical.vcs.data.splittable :as splittable]))

;; * Index

(defprotocol IIndex
  (-init [this f vals])
  (-normalize [this offset]))

(s/def ::index map?)

(defn index
  ([] {})
  ([f vals] (-init (index) f vals)))

;; ** Map

;; NOTE couldn't figure out how to remove the duplication in extending both
;; map types in cljs, `extend` didn't seem to work as expected

(extend-protocol IIndex
  #?@(:clj  [clojure.lang.IPersistentMap]
      :cljs [cljs.core/PersistentHashMap
             cljs.core/PersistentArrayMap])
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
        (transient (empty m)) m)))))


(extend-protocol splittable/Splittable
  #?@(:clj  [clojure.lang.IPersistentMap]
      :cljs [cljs.core/PersistentArrayMap
             cljs.core/PersistentHashMap])
  (split [m idx]
    (->> (seq m)
         (sort-by second)
         (split-at idx)
         (mapv (partial into {})))))

;; * Vector

(s/def ::vector clojure.core/vector?)

(def vec clojure.core/vec)
(def vector clojure.core/vector)
