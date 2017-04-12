(ns vimsical.vcs.data.indexed.vector
  "A wrapper type for a vector to allow for constant time index-of operations.

  NOTES

  Normalize on equiv is bad, make sure we need to check the index at allit
  after enough testing...

  Alternatively we could define a max offset value and on assocN or next
  -normalize if we're above threshold, kinda like a rebalancing op?

  ISSUES

  Doesn't work with concat (no support for lazy seq protocols)"
  (:refer-clojure :exclude [split-at concat])
  (:require [clojure.spec :as s]))


;; * Protocol

(defprotocol IIndexedVector
  (index-of [_ val]))

(defprotocol IIndexedInternal
  (-split-at [_ n])
  (-splice-at [_ n v])
  (-concat [_ v])
  (-normalize [_])
  (-consistent? [_]))


;; * Private

(defn- new-index
  ([vals] (new-index identity vals))
  ([f vals]
   (persistent!
    (reduce
     (fn [m! [i val]]
       (assoc! m! (f val) i))
     (transient {})
     (map-indexed vector vals)))))

(defn- normalize-index
  [index ^long offset]
  (persistent!
   (reduce-kv
    (fn [m! v ^long i]
      (assoc! m! v (+ offset i)))
    (transient (empty index)) index)))


;; * Indexed Vector

(defn ^:declared indexed-vector [offset f index v])


;; ** Type

(deftype IndexedVector
    [^long offset
     f
     ^clojure.lang.PersistentHashMap index
     ^clojure.lang.PersistentVector v]

  clojure.lang.Seqable
  (seq [this] (when (seq v) this))

  clojure.lang.ISeq
  (first [_] (first v))
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
    (let [val-index (+ (- offset) (int i))
          val-key   (f val)
          index'    (assoc index val-key val-index)
          v'        (conj v val)]
      (indexed-vector offset f index' v')))

  clojure.lang.Indexed
  (nth [_ n] (nth v n))
  (nth [_ n not-found] (nth v n not-found))

  IIndexedVector
  (index-of [_ val]
    (when-some [i (get index val)]
      (+ offset ^long i)))

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
        [(indexed-vector offset f index-a v-a)
         (indexed-vector (- offset ^int n) f index-b v-b)])))
  (-splice-at [this n other]
    (let [[l r] (-split-at this n)]
      (into (into l other) r)))
  (-concat [this other]
    (assert (= f (.f ^IndexedVector other)) "Merging vectors indexed by different keys is not supported")
    (let [index' (merge (normalize-index index offset)
                        (normalize-index (.index ^IndexedVector other) (count this)))
          v'     (into v (.v ^IndexedVector other))]
      (indexed-vector 0 f index' v')))
  (-normalize [_]
    (indexed-vector 0 f (normalize-index index offset) v))
  (-consistent? [this]
    (doseq [[i val] (map-indexed vector v)]
      (when-not (= i (index-of this (f val)))
        (throw
         (ex-info
          "Inconsistent indexed vector state"
          {:val          val
           :fval         (f val)
           :index-pos    (index-of this (f val))
           :index-vector i
           :index        index
           :v            v}))))
    true))


;; ** Printing

(defmethod clojure.pprint/simple-dispatch IndexedVector [^IndexedVector v]
  ((get-method clojure.pprint/simple-dispatch clojure.lang.IPersistentVector) (.v v)))

(defmethod print-method IndexedVector [^IndexedVector v w]
  (print-method (.v v) w))


;; * Constructors

(defn indexed-vector?
  ([]  (indexed-vector))
  ([x] (instance? IndexedVector x)))

(s/def ::indexed-vector (s/and indexed-vector? -consistent?))

(s/fdef indexed-vector :ret ::indexed-vector)

(defn indexed-vector
  ([] (indexed-vector []))
  ([v] (indexed-vector 0 identity (new-index v) v))
  ([offset f index v]
   {:pre [(number? offset) (ifn? f) (map? index) (sequential? v)]}
   (IndexedVector. (long offset) f index (vec v))))

(defn indexed-vector-by
  ([f] (indexed-vector 0 f {} []))
  ([f v] (indexed-vector 0 f (new-index f v) v)))


;; * API

(s/fdef split-at
        :args (s/cat :n pos-int? :v ::indexed-vector)
        :ret  (s/tuple ::indexed-vector ::indexed-vector))

(defn split-at [n ^IndexedVector v]
  (-split-at v n))

(s/fdef splice-at
        :args (s/cat :n pos-int? :v ::indexed-vector :insert sequential?)
        :ret  ::indexed-vector)

(defn splice-at [n ^IndexedVector v insert-vector]
  (-splice-at v n insert-vector))

(s/fdef concat
        :args (s/cat :v ::indexed-vector :other ::indexed-vector)
        :ret  ::indexed-vector)

(defn concat [^IndexedVector v ^IndexedVector other]
  (-concat v other))
