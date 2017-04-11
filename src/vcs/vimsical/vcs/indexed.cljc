(ns vimsical.vcs.indexed
  "A wrapper type for a vector to allow for constant time key-of operations."
  (:refer-clojure :exclude [split-at])
  (:require [clojure.spec :as s]))

;; NOTE

;; Normalize on equiv is bad, make sure we need to check the index at allit
;; after enough testing...

;; Alternatively we could define a max offset value and on assocN or next
;; -normalize if we're above threshold, kinda like a rebalancing op?

;; ISSUES

;; Doesn't work with concat (no support for lazy seq protocols)



;; * Protocol

(defprotocol IIndexed
  (key-of [_ val] [_ val not-found]))

(defprotocol IIndexedInternal
  (-split-at [_ n])
  (-splice-at [_ n v])
  (-normalize [_]))

;; * Private

(defn- normalize-index
  [index ^long offset]
  (persistent!
   (reduce-kv
    (fn [m! v ^long i]
      (assoc! m! v (+ offset i)))
    (transient (empty index)) index)))

;; * Vector


(defn ^:declared indexed-vector [offset f index v])

(deftype IndexedVector
    [^long offset
     f
     ^clojure.lang.PersistentHashMap index
     ^clojure.lang.PersistentVector v]

  clojure.lang.Seqable
  (seq [this] (when (seq v) this))

  clojure.lang.ISeq
  (first [_]
    (println "first" (first v))
    (first v))
  (next [this]
    (when (seq v)
      (let [val     (first v)
            val-key (f val)
            offset' (dec offset)
            index'  (dissoc index val-key)
            v'      (vec (next v))]
        (indexed-vector offset' f index' v'))))
  (more [this]
    (or (.next this) (empty this)))

  clojure.lang.IPersistentCollection
  (cons [this val]
    (.assocN this (count v) val))
  (empty [_]
    (indexed-vector 0 f (empty index) (empty v)))
  (equiv [_ val]
    (if (instance? IndexedVector val)
      (and (= v (.v ^IndexedVector val))
           ;; This is bad for performance and could be unnecessary?
           (= (normalize-index index offset)
              (normalize-index (.index ^IndexedVector val) (.offset ^IndexedVector val))))
      false))

  clojure.lang.Counted
  (count [_] (count v))

  clojure.lang.IPersistentVector
  (assocN [_ i val]
    (println val)
    (let [val-index (+ (- offset) (int i))
          val-key   (f val)
          index'    (assoc index val-key val-index)
          v'        (conj v val)]
      (indexed-vector offset f index' v')))

  clojure.lang.Indexed
  (nth [_ n] (nth v n))
  (nth [_ n not-found] (nth v n not-found))

  IIndexed
  (key-of [_ val]
    (+ offset ^long (get index val)))
  (key-of [_ val not-found]
    (if-some [i ^long (get index val not-found)]
      (+ offset i)
      not-found))

  IIndexedInternal
  (-split-at [_ n]
    (letfn [(split-index [n index]
              (->> (seq index)
                   (sort-by second)
                   (clojure.core/split-at n)
                   (mapv (partial into {}))))
            (split-v [n v]
              (->> v
                   (clojure.core/split-at n)
                   (mapv vec)))]
      (let [[index-a index-b] (split-index n index)
            [v-a v-b]         (split-v n v)]
        (println "va-b" v-a v-b)
        [(indexed-vector offset f index-a v-a)
         (indexed-vector (- offset ^int n) f index-b v-b)])))
  (-splice-at [this n other]
    (let [[l r] (-split-at this n)]
      (println {:l l
                :r r
                :other other
                :linto (into l other)
                :rinto (into (into l other) r)
                :vec (vec (into (into l other) r))})
      (into (into l other) r)))
  (-normalize [_]
    (indexed-vector f 0 (normalize-index index offset) v)))

(defmethod clojure.pprint/simple-dispatch IndexedVector [^IndexedVector v]
  ((get-method clojure.pprint/simple-dispatch clojure.lang.IPersistentVector) (.v v)))

(defmethod print-method IndexedVector [^IndexedVector v w]
  (print-method (.v v) w))

(defn indexed-vector
  ([] (indexed-vector nil))
  ([v]
   (-> (indexed-vector 0 identity {} [])
       (into v)))
  ([offset f index v]
   {:pre [(number? offset) (ifn? f) (map? index) (vector? v)]}
   (IndexedVector. (long offset) f index v)))

(defn indexed-vector-by
  ([f] (indexed-vector-by f nil))
  ([f v]
   (-> (IndexedVector. 0 f {} [])
       (into v))))

(defn indexed-vector?
  ([] (indexed-vector))
  ([x] (instance? IndexedVector x)))

(defn split-at [n ^IndexedVector v]
  (-split-at v n))

(defn splice-at [n ^IndexedVector v insert-vector]
  (-splice-at v n insert-vector))


;; * Map

;; TODO clojure.lang.IKVReduce

(deftype IndexedMap
    [^clojure.lang.IPersistentMap index
     ^clojure.lang.IPersistentMap m]

  clojure.lang.IPersistentCollection
  (seq [this] (if (seq m) this nil))
  (cons [_ [key val]]
    (IndexedMap. (assoc index val key) (assoc m key val)))
  (empty [_]
    (IndexedMap. (empty index) (empty m)))
  (equiv [_ val]
    (if (instance? IndexedMap val)
      (and (= m (.m ^IndexedMap val))
           (= index (.index ^IndexedMap val)))
      false))

  clojure.lang.Counted
  (count [_] (count m))

  clojure.lang.ISeq
  (first [_] (first m))
  (next [_] (next m))
  (more [_] (rest m))

  clojure.lang.Associative
  (containsKey [_ key] (.containsKey m key))
  (entryAt [_ key] (.entryAt m key))
  (assoc [_ key val]
    (IndexedMap.
     (assoc index val key)
     (assoc m key val)))

  clojure.lang.ILookup
  (valAt [_ key] (.valAt m key))
  (valAt [_ key not-found] (.valAt m key not-found))

  IIndexed
  (key-of [_ val] (get index val))
  (key-of [_ val not-found] (get index val not-found)))

(defn indexed-map
  ([] (indexed-map nil))
  ([m] (into (IndexedMap. {} {}) m)))

(defn indexed-vector-spec
  [vector-spec]
  (fn [^IndexedVector iv]
    (s/valid? vector-spec (.v iv))))

(comment
  ;; Spec interop
  (s/explain
   (s/every number? :kind vector?)
   (indexed-vector [1 2])))
