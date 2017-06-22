(ns vimsical.vcs.data.splittable
  (:refer-clojure :exclude [interleave])
  (:require
   [clojure.spec.alpha :as s]))

;;
;; * Split
;;

(defprotocol Splittable
  (split [_ idx] [_ idx offset] "Return a tuple of splittables, from 0 to idx exclusive, and from index to end.")
  (omit  [_ idx cnt] "Remove `cnt` values starting at `idx`."))

(defprotocol SplittablePerf
  (split-vec [_ idx]))

;;
;; * Merge
;;

(defprotocol Mergeable
  (splice [_ idx other] "Insert `other` at idx. NOTE that for performance reasons, implementations should inline their implementations of `append` when `idx` is `(count this)`")
  (append [_ other] "An alternative to `concat` for mergrables that doesn't require implementing lazy-seq."))

;;
;; * Default implementations
;;

(extend-protocol Splittable
  #?(:clj String :cljs string)
  (split [s idx]
    [(subs s 0 idx) (subs s idx)])
  (omit [s ^long idx ^long cnt]
    (str (subs s 0 idx) (subs s (+ cnt idx))))

  #?(:clj clojure.lang.IPersistentVector :cljs PersistentVector)
  (split [v ^long idx]
    [(subvec v 0 idx) (subvec v idx)])
  (omit [v ^long idx ^long cnt]
    (into (subvec v 0 idx) (subvec v (+ cnt idx)))))


#?(:cljs
   (extend-protocol Splittable
     Subvec
     (split [v ^long idx]
       [(subvec v 0 idx) (subvec v idx)])
     (omit [v ^long idx ^long cnt]
       (into (subvec v 0 idx) (subvec v (+ (long cnt) (long idx)))))))


(extend-protocol Mergeable
  #?(:clj String :cljs string)
  (splice [s ^long idx other]
    (if (== (count s) idx)
      (str s other)
      (str (subs s 0 idx) other (subs s idx))))
  (append [s other]
    (str s other))

  #?(:clj clojure.lang.IPersistentVector :cljs PersistentVector)
  (splice [v ^long idx other]
    (if (== (count v) idx)
      (into v other)
      (into (into (subvec v 0 idx) other) (subvec v idx))))
  (append [v other]
    (assert (vector? other))
    (into v other)))

#?(:cljs
   (extend-protocol Mergeable
     Subvec
     (splice [v ^long idx other]
       (if (== (count v) idx)
         (into v other)
         (into (into (subvec v 0 idx) other) (subvec v idx))))
     (append [v other]
       (assert (vector? other))
       (into v other))))


(s/def ::idx nat-int?)
(s/def ::offset integer?)
(s/def ::splittable (fn [x] (and x (satisfies? Splittable x))))
(s/def ::mergeable (fn [x] (and x (satisfies? Mergeable x))))

(s/def ::cnt-and-idx-in-bounds
  (fn [{:keys [this ^long idx ^long cnt]}]
    (and (pos-int? cnt) (< (+ cnt idx) (count this)))))

(s/def ::idx-in-bounds
  (fn [{:keys [this ^long idx]}]
    (and (nat-int? idx) (or (zero? idx) (< idx (count this))))))

(s/def ::idx-at-bounds
  (fn [{:keys [this ^long idx]}]
    (and (nat-int? idx) (<= 0 idx (count this)))))

(s/def ::idx-offset-at-bounds
  (fn [{:keys [this ^long idx ^long offset]}]
    (and (nat-int? idx) (int? offset))))

(s/fdef split
        :args (s/or :idx    (s/and (s/cat :this ::splittable :idx ::idx) ::idx-at-bounds)
                    :offset (s/and (s/cat :this ::splittable :idx ::idx :offset ::offset) ::idx-offset-at-bounds)))

(s/fdef splice
        :args (s/and (s/cat :this ::mergeable :idx ::idx :other ::mergeable) ::idx-at-bounds))

(defn splits
  ([splittable indexes] (splits splittable [] indexes))
  ([splittable acc [^long index & indexes]]
   (if (nil? index)
     (cond-> acc (some? splittable) (conj splittable))
     (let [[l r] (split splittable index)]
       (recur r (conj acc l) (map #(- ^long % index) indexes))))))

(defn interleave
  "Like `clojure.core/interleave` but non-lazy and doesn't stop at the shortest
  seq."
  [& colls]
  (loop [colls colls
         acc   (transient [])]
    (if (not-any? seq colls)
      (persistent! acc)
      (recur
       (map next colls)
       (reduce
        (fn [acc coll]
          (cond-> acc
            (seq coll) (conj! (first coll))))
        acc colls)))))
