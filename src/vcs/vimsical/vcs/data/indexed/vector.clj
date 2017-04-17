(ns vimsical.vcs.data.indexed.vector
  "A wrapper type for a vector to allow for constant time index-of operations.

  NOTES

  - The index append method can be made more efficient by looking at the count
  of both indexes and only normalizing the smaller one, currently we only do the
  right one, the assumption being that we spend more time adding to the end...

  - priority maps are a good match for the indexes, but the clj impl was slower
  than maps, however the cljs one looks better, need to test that

  - Wanted to use rrb vectors for the backing vector, howver some gen testing
  for the splittable protocol revealed a bug that may just disqualify it for
  now. This needs more testing

  - `equiv` checks that indexes are `=` after normalizing them, which is always
  going to be slow, we should stick to comparing the vectors once we're
  confident the impl is correct.  after enough testing...

  - Another potential perf gain would be for splittable methods to accept normal
  vectors and index them with the right offset for the function that was
  invoked, rather than forcing the client to build an indexed vector since we'll
  offset it anyway."
  (:refer-clojure :exclude [vec vector vector?])
  (:require
   [clojure.spec :as s]
   [vimsical.vcs.data.indexed.vector-impl :as impl]
   [vimsical.vcs.data.splittable :as splittable]))


;; * Protocol

(defprotocol IIndexedVector
  (index-of [_ val]))


;; * Private

(defprotocol IIndexedInternal
  (-consistent? [_]))

;; ** Index

(deftype Index [^long offset ^clojure.lang.IPersistentMap m]

  clojure.lang.IPersistentCollection
  (empty [_]
    (Index. 0 (impl/index)))
  (equiv [_ other]
    (if (instance? Index other)
      (= (impl/-normalize m offset)
         (impl/-normalize (.m ^Index other) (.offset ^Index  other)))
      false))

  clojure.lang.ISeq
  (seq [this] (seq m))
  (first [_] (first m))
  (next [m] (next m))
  (more [this] (or (.next ^clojure.lang.ISeq m) (empty this)))

  clojure.lang.IPersistentMap
  (assoc [_ val i]
    (Index. offset (assoc m val (+ ^long i (- offset)))))
  (without [_ key]
    ;; The expectation is that dissoc is only invoked from next, meaning we have
    ;; to shift right by one
    (Index. (dec offset) (.without m key)))

  clojure.lang.Associative
  (containsKey [_ key]
    (.containsKey m key))
  (entryAt [_ key]
    (when-some [i (.entryAt ^clojure.lang.Associative m key)]
      (+ offset (int i))))

  clojure.lang.ILookup
  (valAt [_ key]
    (when-some [i (.valAt m key)]
      (+ offset ^long i)))

  splittable/Splittable
  (split [_ idx]
    (let [[l r] (splittable/split m idx)]
      [(Index. offset l) (Index. (- offset ^int idx) r)]))

  splittable/Mergeable
  (append [_ other]
    (let [other-offset (+ (count m) (- (.offset ^Index other) offset))
          m-other      (impl/-normalize (.m ^Index other) other-offset)]
      (Index. offset (merge m m-other))))

  (splice [this idx other]
    (let [[left right] (splittable/split this idx)]
      (->  left
           (splittable/append other)
           (splittable/append right)))))

(s/def ::index #(instance? Index %))

(defn- index
  ([] (Index. 0 (impl/index)))
  ([vals] (index identity vals))
  ([f vals] (Index. 0 (impl/index f vals))))


;; ** Printing

(defmethod clojure.pprint/simple-dispatch Index [^Index m]
  ((get-method clojure.pprint/simple-dispatch clojure.lang.IPersistentMap) (.m m)))

(defmethod print-method Index [^Index m w]
  (print-method (impl/-normalize (.m m) (.offset m)) w))



;; * Impl

(defn ^:declared vector ([]) ([v]) ([f index v]))
(defn ^:declared vector? ([]) ([v]))


(deftype IndexedVector
    [f ^Index index v]

  clojure.lang.Seqable
  (seq [this] (when (seq v) this))

  clojure.lang.ISeq
  (first [_] (first v))
  (next [this]
    (when (seq v)
      (let [val     (first v)
            val-key (f val)
            index'  (dissoc index val-key)
            v'      (clojure.core/vec (next v))]
        (vector f index' v'))))
  (more [this]
    (or (.next this) (empty this)))
  (cons [this val]
    (.assocN this (count v) val))

  clojure.lang.IPersistentCollection
  (empty [_]
    (vector f (empty index) (empty v)))
  (equiv [_ other]
    (if (instance? IndexedVector other)
      (and (= v (.v ^IndexedVector other))
           (= index (.index ^IndexedVector other)))
      false))

  clojure.lang.Counted
  (count [_] (count v))

  clojure.lang.IPersistentVector
  (assocN [_ i val]
    (let [val-key (f val)
          index'  (assoc index val-key i)
          v'      (conj v val)]
      (vector f index' v')))

  clojure.lang.Indexed
  (nth [_ idx] (nth v idx))
  (nth [_ idx not-found] (nth v idx not-found))

  IIndexedVector
  (index-of [_ val]
    ;; The entire purpose of this whole file!
    (get index val))

  IIndexedInternal
  (-consistent? [this]
    (doseq [[i val] (map-indexed clojure.core/vector v)]
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
    this)

  splittable/Splittable
  (split [_ idx]
    (let [[index-a index-b] (splittable/split index idx)
          [v-a v-b]         (splittable/split v idx)]
      [(vector f index-a v-a) (vector f index-b v-b)]))
  (omit [this idx cnt]
    (let [[l tmp] (splittable/split this idx)
          [_ r]   (splittable/split tmp cnt)]
      (splittable/append l r)))


  splittable/Mergeable
  (splice [this idx other]
    (let [index' (splittable/splice index idx (.index ^IndexedVector other))
          v'     (splittable/splice v idx (.v ^IndexedVector other))]
      (vector f index' v')))
  (append [this other]
    (when other (assert (vector? other)))
    (if (nil? other)
      this
      (let [index' (splittable/append index (.index ^IndexedVector other))
            v'     (splittable/append v (.v ^IndexedVector other))]
        (vector f index' v')))))


;; ** Printing

(defmethod clojure.pprint/simple-dispatch IndexedVector [^IndexedVector v]
  ((get-method clojure.pprint/simple-dispatch clojure.lang.IPersistentVector) (.v v)))

(defmethod print-method IndexedVector [^IndexedVector v w]
  (print-method (.v v) w))

(prefer-method clojure.pprint/simple-dispatch Index clojure.lang.IPersistentMap )


;; * API

(defn vector?
  ([]  (vector))
  ([x] (instance? IndexedVector x)))

(s/def ::vector-like (s/or :seq seq? :vec clojure.core/vector? :sequ sequential?))
(s/def ::vector (s/and vector? -consistent?))

(s/explain ::vector (vector))

;; ** By value

(s/fdef vec :args (s/cat :v ::vector-like))

(defn vec [v]
  (IndexedVector. identity (index v) (impl/vec v)))

(s/fdef vector
        :args (s/* (s/cat :f ifn? :index ::index :v ::impl/vector))
        :ret ::vector)

(defn vector
  ([] (vector identity (index) (impl/vector)))
  ([f index v]
   (IndexedVector. f index (impl/vec v))))


;; ** By key

(s/fdef vec-by :args (s/cat :f ifn? :v ::vector-like))

(defn vec-by [f v]
  (vector f (index f v) (impl/vec v)))

(s/fdef vector-by :args (s/cat :f ifn?))

(defn vector-by [f]
  (vector f (index) (impl/vector)))

(comment
  (orchestra.spec.test/unstrument)
  (require '[criterium.core :as criterium])
  ;; Vectors: Execution time mean : 600.855707 ms
  (let [cnt 10000
        v   (vec (range cnt))
        v'  (vec (range 5))]
    (criterium/quick-bench
     (splittable/splice v (rand-int cnt) v'))))



(require '[clojure.data.avl :as avl])
