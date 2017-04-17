(ns vimsical.vcs.data.splittable
  (:refer-clojure :exclude [remove])
  (:require
   [clojure.core.rrb-vector :as rrb])
  (:import
   (clojure.core.rrb_vector.rrbt Vector)))

;; * Split

(defprotocol Splittable
  (split [_ idx] "Return a tuple of splittables, from 0 to idx exclusive, and from index to end.")
  (omit  [_ idx cnt] "Remove `cnt` values starting at `idx`."))

;; * Merge

(defprotocol Mergeable
  (splice [_ idx other] "Insert `other` at idx.")
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
    (str (subs s 0 idx) other (subs s idx)))
  (append [s other]
    (str s other)))

(extend-type clojure.lang.IPersistentVector
  Splittable
  (split [v idx]
    [(subvec v 0 idx) (subvec v idx)])
  (omit [v idx cnt]
    (into (subvec v 0 idx) (subvec v (+ (long cnt) (long idx)))))

  Mergeable
  (splice [v idx other]
    (into (into (subvec v 0 idx) other) (subvec v idx)))
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
    (rrb/catvec (rrb/subvec v 0 idx) other (rrb/subvec v idx)))
  (append [v other]
    (rrb/catvec v other)))
