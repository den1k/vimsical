(ns vimsical.vcs.data.splittable
  (:refer-clojure :exclude [remove])
  (:require
   [clojure.spec :as s]
   [clojure.core.rrb-vector :as rrb])
  (:import
   (clojure.core.rrb_vector.rrbt Vector)))

;; * Split

(defprotocol Splittable
  (split [_ idx] "Return a tuple of splittables, from 0 to idx exclusive, and from index to end.")
  (omit  [_ idx cnt] "Remove `cnt` values starting at `idx`."))

;; * Merge

(defprotocol Mergeable
  (splice [_ idx other] "Insert `other` at idx. NOTE that for performance reasons, implementations should inline their implementations of `append` when `idx` is `(count this)`")
  (append [_ other] "An alternative to `concat` for mergrables that doesn't require implementing lazy-seq."))

;; * Types

(extend-type String
  Splittable
  (split [s idx]
    [(subs s 0 idx) (subs s idx)])
  (omit [s idx cnt]
    (str (subs s 0 idx) (subs s (+ (long cnt) (long idx)))))

  Mergeable
  (splice [s idx other]
    (if (== (count s) idx)
      (str s other)
      (str (subs s 0 idx) other (subs s idx))))
  (append [s other]
    (str s other)))

(extend-type clojure.lang.IPersistentVector
  Splittable
  (split [v idx]
    (println {:split {:v v :idx idx}})
    [(subvec v 0 idx) (subvec v idx)])
  (omit [v idx cnt]
    (println {:omit {:v v :idx idx :cnt cnt :res (into (subvec v 0 idx) (subvec v (+ (long cnt) (long idx))))}})
    (into (subvec v 0 idx) (subvec v (+ (long cnt) (long idx)))))

  Mergeable
  (splice [v idx other]
    (if (== (count v) idx)
      (into v other)
      (into (into (subvec v 0 idx) other) (subvec v idx))))
  (append [v other]
    (assert (vector? other))
    (into v other)))

(extend-type clojure.core.rrb_vector.rrbt.Vector
  Splittable
  (split [v idx]
    (try
      [(rrb/subvec v 0 idx) (rrb/subvec v idx)]
      (catch Throwable t
        (println {:split {:v v :idx idx :e t}}))))
  (omit [v idx cnt]
    (rrb/catvec (rrb/subvec v 0 idx) (rrb/subvec v (+ (long cnt) (long idx)))))

  Mergeable
  (splice [v idx other]
    (if (== (count v) idx)
      (rrb/catvec v other)
      (rrb/catvec (rrb/subvec v 0 idx) other (rrb/subvec v idx))))
  (append [v other]
    (rrb/catvec v other)))

(s/def ::splittable (fn [x] (and x (satisfies? Splittable x))))
(s/def ::mergeable (fn [x] (and x (satisfies? Mergeable x))))

(s/def ::cnt-and-idx-in-bounds
  (fn [{:keys [this idx cnt]}]
    (< (+ cnt idx) (count this))))

(s/def ::idx-in-bounds
  (fn [{:keys [this idx]}]
    (or (zero? idx) (< idx (count this)))))

(s/def ::idx-at-bounds
  (fn [{:keys [this idx]}]
    (<= 0 idx (count this))))

(s/fdef split
        :args (s/and
               (s/cat :this ::splittable :idx nat-int?)
               ::idx-in-bounds))

(s/fdef splice
        :args (s/and
               (s/cat :this ::mergeable :idx nat-int? :other ::mergeable)
               ::idx-at-bounds))
(omit [1 2 3] 0 1)
