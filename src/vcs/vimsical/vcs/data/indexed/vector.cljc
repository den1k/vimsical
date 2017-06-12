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
   [clojure.spec.alpha :as s]
   [vimsical.vcs.data.indexed.vector-impl :as impl]
   [vimsical.vcs.data.splittable :as splittable]
   #?(:clj [clojure.pprint :as pprint])))

;;
;; * Protocol
;;

(defprotocol IIndexedVector
  (index-of [_ val]))

;;
;; * Private
;;

(defprotocol IIndexedInternal
  (-consistent? [_]))

;;
;; ** Index
;;

#?(:clj
   (deftype Index [^long offset ^clojure.lang.IPersistentMap m]

     clojure.lang.IPersistentCollection
     (empty [_] (Index. 0 (impl/index)))

     (equiv [_ other]
       (if (instance? Index other)
         (= (impl/-normalize m offset)
            (impl/-normalize (.m ^Index other) (.offset ^Index  other)))
         false))

     clojure.lang.ISeq
     (seq [_] (seq m))
     (first [_] (first m))
     (next [_] (next m))
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
       (if (== (count m) (long idx))
         (splittable/append this other)
         (let [[left right] (splittable/split this idx)]
           (->  left
                (splittable/append other)
                (splittable/append right)))))))

#?(:cljs
   (deftype Index [offset m]
     Object
     (toString [this]
       (pr-str* (impl/-normalize m offset)))

     IEmptyableCollection
     (-empty [_] (Index. 0 (impl/index)))

     IEquiv
     (-equiv [this other]
       (or (identical? this other)
           (if (instance? Index other)
             (-equiv (impl/-normalize m offset)
                     (impl/-normalize (.-m other) (.-offset other)))
             false)))

     ISeq
     (-first [_] (first m))
     (-rest [this] (or (next m) (empty this)))

     ISeqable
     (-seq [_] (seq m))

     INext
     (-next [_] (next m))

     IAssociative
     (-assoc [_ val i]
       (Index. offset (assoc m val (+ i (- offset)))))

     IMap
     (-dissoc [_ key]
       ;; The expectation is that dissoc is only invoked from next, meaning we have
       ;; to shift right by one
       (Index. (dec offset) (dissoc m key)))

     ILookup
     (-lookup [_ key]
       (when-some [i (get m key)]
         (+ offset i)))

     splittable/Splittable
     (split [_ idx]
       (let [[l r] (splittable/split m idx)]
         [(Index. offset l) (Index. (- offset idx) r)]))

     splittable/Mergeable
     (append [_ other]
       (let [other-offset (+ (count m) (- (.-offset other) offset))
             m-other      (impl/-normalize (.-m other) other-offset)]
         (Index. offset (merge m m-other))))

     (splice [this idx other]
       (if (== (count m) (long idx))
         (splittable/append this other)
         (let [[left right] (splittable/split this idx)]
           (->  left
                (splittable/append other)
                (splittable/append right)))))))

(s/def ::index #(instance? Index %))

(defn- new-index
  ([] (Index. 0 (impl/index)))
  ([vals] (Index. 0 (impl/index identity vals)))
  ([f vals] (Index. 0 (impl/index f vals))))

;;
;; ** Printing
;;

#?(:clj
   (defmethod pprint/simple-dispatch Index [^Index m]
     ((get-method pprint/simple-dispatch clojure.lang.IPersistentMap) (.m m))))

#?(:clj
   (defmethod print-method Index [^Index m w]
     (print-method (impl/-normalize (.m m) (.offset m)) w)))

;;
;; * Impl
;;

(defn ^:declared vector ([]) ([v]) ([f index v]))
(defn ^:declared vector? ([]) ([v]))


#?(:clj
   (deftype IndexedVector
       [f ^Index index v]

     clojure.lang.Seqable
     (seq [this] (when (seq v) this))

     clojure.lang.ISeq
     (first [_] (first v))
     (next [_]
       (when-some [nv (next v)]
         (let [old-val     (first v)
               old-val-key (f old-val)
               index'      (dissoc index old-val-key)
               v'          (impl/vec nv)]
           (vector f index' v'))))
     (more [this]
       (or (.next this) (empty this)))
     (cons [this val]
       (let [i       (count v)
             val-key (f val)
             index'  (update index val-key
                             (fn [prev-i?]
                               (if (some? prev-i?)
                                 (throw (ex-info "index already assigned" {:i i}))
                                 i)))
             v'      (conj v val)]
         (vector f index' v')))

     clojure.lang.IPersistentStack
     (peek [_] (.peek v))

     java.lang.Iterable
     ;; XXX This might be a bit risky in cases where someone would call remove()
     ;; on the vector's iterator, our index would go out of sync.
     (iterator [_] (.iterator v))

     clojure.lang.IPersistentCollection
     (empty [_]
       (vector f (new-index) (impl/vector)))
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
       (when (and (= identity f) (not= i (count v)))
         ;; Unsupported for now...
         (throw
          (ex-info
           "Cannot update a value that will change the key." {:f f :val val :i i :val-at-i (get v i)})))
       (let [val-key (f val)
             index'  (assoc index val-key i)
             v'      (assoc v i val)]
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
         [(vector f index-a v-a)
          (vector f index-b v-b)]))
     (omit [this idx cnt]
       (let [[l tmp] (splittable/split this idx)
             [_ r]   (splittable/split tmp cnt)]
         (splittable/append l r)))

     splittable/Mergeable
     (splice [this idx other]
       (if (== (count this) (long idx))
         (splittable/append this other)
         (let [index' (splittable/splice index idx (.index ^IndexedVector other))
               v'     (splittable/splice v idx (.v ^IndexedVector other))]
           (vector f index' v'))))
     (append [this other]
       (when other (assert (vector? other)))
       (if (nil? other)
         this
         (let [index' (splittable/append index (.index ^IndexedVector other))
               v'     (splittable/append v (.v ^IndexedVector other))]
           (vector f index' v'))))))

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
               index'      (dissoc index old-val-key)
               v'          (impl/vec nv)]
           (vector f index' v'))))

     IReduce
     (-reduce [_ f] (-reduce v f))
     (-reduce [_ f start] (-reduce v f start))

     ICollection
     (-conj [this val]
       (let [i       (count v)
             val-key (f val)
             index'  (update index val-key
                             (fn [prev-i?]
                               (if (some? prev-i?)
                                 (throw (ex-info "index already assigned" {:i i}))
                                 i)))
             v'      (conj v val)]
         (vector f index' v')))

     IEmptyableCollection
     (-empty [_] (vector f (new-index) (impl/vector)))

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
       (when (and (= identity f) (not= i (count v)))
         ;; Unsupported for now...
         (throw
          (ex-info
           "Cannot update a value that will change the key." {:f f :val val :i i :val-at-i (get v i)})))
       (let [val-key (f val)
             index'  (assoc index val-key i)
             v'      (assoc v i val)]
         (vector f index' v')))

     IIndexed
     (-nth [_ idx] (nth v idx))
     (-nth [_ idx not-found] (nth v idx not-found))

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
         [(vector f index-a v-a)
          (vector f index-b v-b)]))
     (omit [this idx cnt]
       (let [[l tmp] (splittable/split this idx)
             [_ r]   (splittable/split tmp cnt)]
         (splittable/append l r)))

     splittable/Mergeable
     (splice [this idx other]
       (if (== (count this) (long idx))
         (splittable/append this other)
         (let [index' (splittable/splice index idx (.-index other))
               v'     (splittable/splice v idx (.-v other))]
           (vector f index' v'))))
     (append [this other]
       (when other (assert (vector? other)))
       (if (empty? other)
         this
         (let [index' (splittable/append index (.-index other))
               v'     (splittable/append v (.-v other))]
           (vector f index' v'))))))

;;
;; ** Printing
;;

#?(:clj
   (defmethod pprint/simple-dispatch IndexedVector [^IndexedVector v]
     ((get-method pprint/simple-dispatch clojure.lang.IPersistentVector) (.v v))))

#?(:clj
   (defmethod print-method IndexedVector [^IndexedVector v w]
     (print-method (.v v) w)))

#?(:clj
   (prefer-method pprint/simple-dispatch Index clojure.lang.IPersistentMap ))

;;
;; * API
;;

(defn vector?
  ([]  (vector))
  ([x] (instance? IndexedVector x)))

(s/def ::vector-like (s/or :seq seq? :vec clojure.core/vector? :sequ sequential?))
(s/def ::vector (s/and vector? -consistent?))

;;
;; ** By value
;;

(s/fdef vec :args (s/cat :v ::vector-like))

(defn vec [v]
  (IndexedVector. identity (new-index v) (impl/vec v)))

(s/fdef vector
        :args (s/* (s/cat :f ifn? :index ::index :v ::impl/vector))
        :ret ::vector)

(defn vector
  ([]
   (IndexedVector. identity (new-index) (impl/vector)))
  ([f index v]
   (IndexedVector. f index (impl/vec v))))

;;
;; ** By key
;;

(s/fdef vec-by :args (s/cat :f ifn? :v ::vector-like))

(defn vec-by [f v]
  (vector f (new-index f v) (impl/vec v)))

(s/fdef vector-by :args (s/cat :f ifn?))

(defn vector-by [f]
  (vector f (new-index) (impl/vector)))

(comment
  (orchestra.spec.test/unstrument)
  (require '[criterium.core :as criterium])
  ;; Vectors: Execution time mean : 600.855707 ms
  (let [cnt 10000
        v   (vec (range cnt))
        v'  (vec (range 5))]
    (criterium/quick-bench
     (splittable/splice v (rand-int cnt) v'))))
