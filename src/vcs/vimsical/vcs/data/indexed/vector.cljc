(ns vimsical.vcs.data.indexed.vector
  "A wrapper type for a vector to allow for constant time index-of operations.

  NOTES

  priority maps are a good match for the indexes, but the clj impl was slower
  than maps, the cljs one looks better, need to test that

  Use rrb vectors for vectors

  Normalize on equiv is bad, make sure we need to check the index at allit
  after enough testing...

  Alternatively we could define a max offset value and on assocN or next
  -normalize if we're above threshold, kinda like a rebalancing op?

  ISSUES
  
  Doesn't work with concat (no support for lazy seq protocols)"
  (:refer-clojure :exclude [split-at concat])
  (:require
   [clojure.spec :as s]
   [clojure.data.priority-map :as pm]
   [clojure.core.rrb-vector :as rrb]))


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
;; ** Index

(defn- new-index
  ([] {})
  ([vals] (new-index identity vals))
  ([f vals]
   (persistent!
    (reduce
     (fn [m [i val]]
       (assoc! m (f val) i))
     (transient (new-index)) (map-indexed vector vals)))))

(defn- normalize-index
  [index ^long offset]
  (if (zero? offset)
    index
    (persistent!
     (reduce-kv
      (fn [m v ^long i]
        (assoc! m v (+ offset i)))
      (transient (empty index)) index))))

(defn- split-index
  [n index]
  (->> (seq index)
       (sort-by second)
       (clojure.core/split-at n)
       (mapv (partial into (new-index)))))

(defn merge-indexes
  [index-offset-map]
  (reduce-kv
   (fn [m index offset]
     (merge m (normalize-index index offset)))
   (new-index) index-offset-map))

(defn- splice-index
  [n index other]
  (let [[index-a index-b] (split-index n index)]
    (merge-indexes
     {index-a 0
      other   (count index-a)
      index-b (count other)})))


;; ** Vector

(defn- new-vector
  ([] (rrb/vector))
  ([v] (rrb/vec v)))

(defn- split-vector [n v]
  [(rrb/subvec v 0 n) (rrb/subvec v n)])

(defn- splice-vector
  [n v other]
  (let [[l r] (split-vector n v)]
    (rrb/catvec l other r)))

(defn merge-vectors
  [& vs]
  (apply rrb/catvec vs ))


;; * Indexed Vector

(defn ^:declared indexed-vector [offset f index v])
(defn ^:declared indexed-vector? [v])


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
    (let [[index-a index-b] (split-index n index)
          [v-a v-b]         (split-vector n v)]
      [(indexed-vector offset f index-a v-a)
       (indexed-vector (- offset ^int n) f index-b v-b)]))
  (-splice-at [this n other]
    ;; Could coerce the arg and build the index, would be more efficient than
    ;; building the index client side to offset it here again...
    (assert (indexed-vector? other))
    (let [index' (splice-index n index (.index ^IndexedVector other))
          v'     (splice-vector n v (.v ^IndexedVector other))]
      (indexed-vector offset f index' v')))
  (-concat [this other]
    (assert (indexed-vector? other))
    (let [index' (merge-indexes
                  {index                         offset
                   (.index ^IndexedVector other) (count this)})
          v'     (merge-vectors v (.v ^IndexedVector other))]
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


;; * API

(defn indexed-vector?
  ([]  (indexed-vector))
  ([x] (instance? IndexedVector x)))

(s/def ::indexed-vector (s/and indexed-vector? -consistent?))

(s/fdef indexed-vector :ret ::indexed-vector)

(defn indexed-vector
  ([] (indexed-vector (new-vector)))
  ([v] (indexed-vector 0 identity (new-index v) v))
  ([offset f index v]
   {:pre [(number? offset) (ifn? f) (map? index) (sequential? v)]}
   (IndexedVector. (long offset) f index (new-vector v))))

(defn indexed-vector-by
  ([f] (indexed-vector 0 f (new-index) (new-vector)))
  ([f v] (indexed-vector 0 f (new-index f v) (new-vector v))))

(s/fdef split-at
        :args (s/cat :n nat-int? :v ::indexed-vector)
        :ret  (s/tuple ::indexed-vector ::indexed-vector))

(defn split-at [n ^IndexedVector v]
  (-split-at v n))

(s/fdef splice-at
        :args (s/cat :n nat-int? :v ::indexed-vector :insert sequential?)
        :ret  ::indexed-vector)

(defn splice-at [n ^IndexedVector v other]
  (if (== (count v) (long n))
    (-concat v other)
    (-splice-at v n other)))

(s/fdef concat
        :args (s/cat :v ::indexed-vector :other ::indexed-vector)
        :ret  ::indexed-vector)

(defn concat [^IndexedVector v other]
  (-concat v other))



(comment
  (orchestra.spec.test/unstrument)
  (require '[criterium.core :as criterium])
  ;; Vectors: Execution time mean : 600.855707 ms
  (let [cnt 10000
        v   (indexed-vector (range cnt))
        v'  (indexed-vector (range 5))]
    (criterium/quick-bench
     (splice-at cnt v v'))))
