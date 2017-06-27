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
   [vimsical.common.util.core :as util]
   [clojure.spec.alpha :as s]
   [vimsical.vcs.data.indexed.vector-impl :as impl]
   [vimsical.vcs.data.splittable :as splittable]
   #?(:clj [clojure.pprint :as pprint])))

;;
;; * Protocol
;;

(defprotocol IIndexedVector
  (index-of [_ val] [_ val start]))

(defn -index-of
  [v val start]
  #?(:clj
     (let [i (.indexOf v val)]
       (when-not (neg? ^long i) i))
     :cljs
     (or (let [i (.indexOf v val start)]
           (when-not (neg? i) i))
         (loop [i start]
           (when-not (neg? ^long i)
             (if (= val (nth v i))
               i
               (recur (dec ^long i))))))))

;;
;; * Private
;;

(defprotocol IIndexedInternal
  (-consistent? [_]))

;;
;; * Impl
;;

(defn ^:declared vector ([]) ([v]) ([f index v]))
(defn ^:declared vector? ([]) ([v]))


#?(:clj
   (deftype IndexedVector
       [f index v]

     clojure.lang.Seqable
     (seq [this] (when (seq v) this))

     clojure.lang.ISeq
     (first [_] (first v))
     (next [_]
       (when-some [nv (next v)]
         (let [index'      (impl/vec (next index))
               v'          (impl/vec nv)]
           (IndexedVector. f index' v'))))
     (more [this]
       (or (.next this) (empty this)))
     (cons [this val]
       (let [val-key (f val)
             index'  (conj index val-key)
             v'      (conj v val)]
         (IndexedVector. f index' v')))

     clojure.lang.IPersistentStack
     (peek [_] (.peek ^clojure.lang.IPersistentStack v))

     java.lang.Iterable
     ;; XXX This might be a bit risky in cases where someone would call remove()
     ;; on the vector's iterator, our index would go out of sync.
     (iterator [_] (.iterator ^java.lang.Iterable v))

     clojure.lang.IPersistentCollection
     (empty [_]
       (IndexedVector. f (impl/index) (impl/vector)))

     (equiv [this other]
       (or (identical? this other)
           (if (instance? IndexedVector other)
             (and (= v (.v ^IndexedVector other))
                  (= index (.index ^IndexedVector other)))
             false)))

     clojure.lang.Counted
     (count [_] (count v))

     clojure.lang.IPersistentMap
     (assoc [this i val] (.assocN this i val))
     (valAt [this i] (.nth this i))

     clojure.lang.IPersistentVector
     (assocN [_ i val]
       (let [val-key (f val)
             index'  (assoc index i val-key)
             v'      (assoc v i val)]
         (IndexedVector. f index' v')))

     clojure.lang.Indexed
     (nth [_ idx] (nth v idx))
     (nth [_ idx not-found] (nth v idx not-found))

     IIndexedVector
     (index-of [_ val] (.indexOf index val))
     (index-of [_ val start] (-index-of index val start))

     IIndexedInternal
     (-consistent? [this]
       (doseq [[i val] (map clojure.core/vector index v)]
         (try
           (when-not (= i (f val))
             (throw
              (ex-info
               "Inconsistent indexed vector state"
               {:val          val
                :fval         (f val)
                :index-vector i
                :index        index
                :v            v})))
           (catch Throwable t
             (throw (ex-info "Index impl error" {:i i :val val :fval (f val) :index index :v v})))))
       this)

     splittable/Splittable
     (split [_ idx]
       (let [[index-a index-b] (splittable/split index idx)
             [v-a v-b]         (splittable/split v idx)]
         [(IndexedVector. f index-a v-a)
          (IndexedVector. f index-b v-b)]))
     (omit [this idx cnt]
       (let [[l tmp] (splittable/split this idx)
             [_ r]   (splittable/split tmp cnt)]
         (splittable/append l r)))

     splittable/SplittablePerf
     (split-vec [_ idx]
       (splittable/split v idx))


     splittable/Mergeable
     (insert [this idx element]
       (if (== (count this) (long idx))
         (conj this element)
         (let [[left right] (splittable/split this idx)]
           (splittable/append (conj left element) right))))

     (splice [this idx other]
       (if (== (count this) (long idx))
         (splittable/append this other)
         (let [index' (splittable/splice index idx (.index ^IndexedVector other))
               v'     (splittable/splice v idx (.v ^IndexedVector other))]
           (IndexedVector. f index' v'))))

     (append [this other]
       (when other (assert (vector? other)))
       (if (nil? other)
         this
         (let [index' (splittable/append index (.index ^IndexedVector other))
               v'     (splittable/append v (.v ^IndexedVector other))]
           (IndexedVector. f index' v'))))))

#?(:cljs
   (deftype IndexedVector [f ^Index index v]
     Object
     (toString [this]
       (pr-str* v))

     ISeqable
     (-seq [this] (when (seq v) this))

     ASeq
     ISeq
     (-first [_] (first v))
     (-rest [this] (or (next this) (empty this)))

     IStack
     (-peek [_] (-peek v))

     INext
     (-next [_]
       (when-some [nv (next v)]
         (let [old-val     (first v)
               old-val-key (f old-val)
               index'      (impl/vec (next index))
               v'          (impl/vec nv)]
           (IndexedVector. f index' v'))))

     IReduce
     (-reduce [_ f] (-reduce v f))
     (-reduce [_ f start] (-reduce v f start))

     ICollection
     (-conj [this val]
       (let [val-key (f val)
             index'  (conj index val-key)
             v'      (conj v val)]
         (IndexedVector. f index' v')))

     IEmptyableCollection
     (-empty [_] (IndexedVector. f (impl/index) (impl/vector)))

     ISequential
     IEquiv
     (-equiv [this other]
       (or (identical? this other)
           (if (instance? IndexedVector other)
             (and (-equiv v (.-v other)) (-equiv index (.-index other)))
             false)))

     ICounted
     (-count [_] (count v))

     IAssociative
     (-assoc [this i val] (-assoc-n this i val))

     ILookup
     (-lookup [this i] (-nth this i))

     IVector
     (-assoc-n [_ i val]
       (let [val-key (f val)
             index'  (assoc index i val-key)
             v'      (assoc v i val)]
         (IndexedVector. f index' v')))

     IIndexed
     (-nth [_ idx] (nth v idx))
     (-nth [_ idx not-found] (nth v idx not-found))

     IIndexedVector
     (index-of [_ val] (.indexOf index val))
     (index-of [_ val start] (-index-of index val start))

     IIndexedInternal
     (-consistent? [this]
       (doseq [[i val] (map clojure.core/vector index v)]
         (when-not (= i (f val))
           (throw
            (ex-info
             "Inconsistent indexed vector state"
             {:val          val
              :fval         (f val)
              :index-vector i
              :index        index
              :v            v}))))
       this)

     splittable/Splittable
     (split [_ idx]
       (let [[index-a index-b] (splittable/split index idx)
             [v-a v-b]         (splittable/split v idx)]
         [(IndexedVector. f index-a v-a)
          (IndexedVector. f index-b v-b)]))
     (omit [this idx cnt]
       (let [[l tmp] (splittable/split this idx)
             [_ r]   (splittable/split tmp cnt)]
         (splittable/append l r)))

     splittable/SplittablePerf
     (split-vec [_ idx]
       (splittable/split v idx))

     splittable/Mergeable
     (insert [this idx element]
       (if (== (count this) (long idx))
         (conj this element)
         (let [[left right] (splittable/split this idx)]
           (splittable/append (conj left element) right))))

     (splice [this idx other]
       (if (== (count this) (long idx))
         (splittable/append this other)
         (let [index' (splittable/splice index idx (.-index other))
               v'     (splittable/splice v idx (.-v other))]
           (IndexedVector. f index' v'))))
     (append [this other]
       (when other (assert (vector? other)))
       (if (empty? other)
         this
         (let [index' (splittable/append index (.-index other))
               v'     (splittable/append v (.-v other))]
           (IndexedVector. f index' v'))))))

;;
;; ** Printing
;;

#?(:clj
   (defmethod pprint/simple-dispatch IndexedVector [^IndexedVector v]
     ((get-method pprint/simple-dispatch clojure.lang.IPersistentVector) (.v v))))

#?(:clj
   (defmethod print-method IndexedVector [^IndexedVector v w]
     (print-method (.v v) w)))

;;
;; * API
;;

(defn vector?
  ([]  (vector))
  ([x] (instance? IndexedVector x)))

(s/def ::index clojure.core/vector?)
(s/def ::vector-like (s/or :seq seq? :vec clojure.core/vector? :sequ sequential?))
(s/def ::vector (s/and vector? -consistent?))

;;
;; ** By value
;;

(s/fdef vec :args (s/cat :v ::vector-like))

(defn vec [v]
  (IndexedVector. identity (impl/index v) (impl/vec v)))

(s/fdef vector
        :args (s/* (s/cat :f ifn? :index ::index :v ::impl/vector))
        :ret ::vector)

(defn vector
  ([]
   (IndexedVector. identity (impl/index) (impl/vector)))
  ([f index v]
   (IndexedVector. f index (impl/vec v))))

;;
;; ** By key
;;

(s/fdef vec-by :args (s/cat :f ifn? :v ::vector-like))

(defn vec-by [f v]
  (vector f (impl/index f v) (impl/vec v)))

(s/fdef vector-by :args (s/cat :f ifn?))

(defn vector-by [f]
  (vector f (impl/index) (impl/vector)))

(comment
  (orchestra.spec.test/unstrument)
  (require '[criterium.core :as criterium])
  ;; Vectors: Execution time mean : 600.855707 ms
  (let [cnt 10000
        v   (vec (range cnt))
        v'  (vec (range 5))]
    (criterium/quick-bench
     (splittable/splice v (rand-int cnt) v'))))
