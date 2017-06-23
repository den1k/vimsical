(ns vimsical.vcs.data.indexed.vector-impl
  (:refer-clojure :exclude [vec vector vector?])
  (:require
   [taoensso.tufte :as tufte :refer (defnp p profiled profile)]
   [clojure.spec.alpha :as s]
   [vimsical.vcs.data.splittable :as splittable]))

;;
;; * Index
;;

(defprotocol IIndex
  (-init [this f vals])
  (-normalize [this offset]))

(s/def ::index map?)

(defn index
  ([] {})
  ([f vals] (-init (index) f vals)))

;;
;; ** Map
;;

;; NOTE couldn't figure out how to remove the duplication in extending both
;; map types in cljs, `extend` didn't seem to work as expected

(extend-protocol IIndex
  #?(:clj clojure.lang.IPersistentMap :cljs PersistentArrayMap)
  (-init [m f vals]
    (p ::init-map
       (persistent!
        (reduce
         (fn [m [i val]]
           (assoc! m (f val) i))
         (transient (empty m)) (map-indexed clojure.core/vector vals)))))
  (-normalize [m offset]
    (p ::normalize-map
       (if (zero? ^long offset)
         m
         (persistent!
          (reduce-kv
           (fn [m v i]
             (assoc! m v (+ ^long offset ^long i)))
           (transient (empty m)) m))))))

#?(:cljs
   (extend-protocol IIndex
     PersistentHashMap
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
           (transient (empty m)) m))))))

(defn- split-map-at-idx
  [m idx]
  (->> (seq m)
       (sort-by second)
       (split-at idx)
       (mapv (partial into (empty m)))))

(defn- split-map-at-value
  [m idx]
  (reduce-kv
   (fn [[l r] val index]
     (if (< ^long index ^long idx)
       [(assoc l val index) r]
       [l (assoc r val index)]))
   [(empty m) (empty m)] m))

(defn- split-map-at-value-transient
  [m idx]
  (p ::split-map-transient
     (mapv persistent!
           (reduce-kv
            (fn [[l r] val index]
              (if (< ^long index ^long idx)
                [(assoc! l val index) r]
                [l (assoc! r val index)]))
            [(transient (empty m))
             (transient (empty m))]
            m))))

(extend-protocol splittable/Splittable
  #?(:clj clojure.lang.IPersistentMap :cljs PersistentArrayMap)
  (split
    ([m idx]
     (split-map-at-idx m idx))
    ([m idx offset]
     (split-map-at-value-transient m (+ ^long offset ^long idx)))))

#?(:cljs
   (extend-protocol splittable/Splittable
     PersistentHashMap
     (split
       ([m idx]
        (split-map-at-idx m idx))
       ([m idx offset]
        (split-map-at-value-transient m (+ offset idx))))))


;;
;; * Vector
;;

(s/def ::vector clojure.core/vector?)

(def vec clojure.core/vec)
(def vector clojure.core/vector)

