(ns vimsical.vcs.data.splittable-test
  "TODO
  Check that we don't generate garbage from no-ops, like splicing an empty coll
  or omitting 0: test for identical? return values."
  #?@(:clj
      [(:require
        [clojure.spec :as s]
        [clojure.test :as t :refer [testing]]
        [clojure.test.check.clojure-test :as tc :refer [defspec]]
        [clojure.test.check.generators :as gen]
        [clojure.test.check.properties :as prop :refer [for-all]]
        [orchestra.spec.test :as st]
        [vimsical.vcs.data.indexed.vector :as indexed]
        [vimsical.vcs.data.splittable :as sut])]
      :cljs
      [(:require
        [clojure.spec :as s]
        [clojure.spec.test :as st]
        [cljs.test :as t :refer-macros [deftest is testing]]
        [clojure.test.check.clojure-test :as tc :refer-macros [defspec]]
        [clojure.test.check.generators :as gen]
        [clojure.test.check.properties :as prop :refer-macros [for-all]]
        [vimsical.vcs.data.indexed.vector :as indexed]
        [vimsical.vcs.data.splittable :as sut])]))

(st/instrument)


;; * Generators

(defn gen-with-index-in-bounds
  "Return a generator that will return tuples of: value from coll-gen, index in value."
  [coll-gen]
  (gen/bind
   coll-gen
   (fn [coll]
     (gen/tuple
      (gen/return coll)
      (gen/choose 0 (max 0 (dec (count coll))))))))

(defn gen-with-index-at-bounds
  "Return a generator that will return tuples of: value from coll-gen, index in value or count of value."
  [coll-gen]
  (gen/bind
   coll-gen
   (fn [coll]
     (gen/tuple
      (gen/return coll)
      (gen/choose 0 (max 0 (dec (count coll))))))))

(defn gen-with-index-and-count
  "Return a generator that will return tuples of:
    [<value from coll-gen>
    [<valid index in coll>
    <count within bounds of index to end of coll>]."
  [coll-gen]
  (gen/let [coll          coll-gen
            index         (gen/choose 0 (max 0 (dec (count coll))))
            bounded-count (gen/choose 0 (max 0 (- (count coll) index)))]
    [coll
     [index bounded-count]]))

(defn gen-with-index-and-counts
  "Return a generator that will return tuples of:
    [<value from coll-gen>
    [<valid index in coll>
    <count within bounds of index to end of coll>
    <count within bounds previous count>]."
  [coll-gen]
  (gen/let [coll           coll-gen
            index          (gen/choose 0 (max 0 (dec (count coll))))
            bounded-count  (gen/choose 0 (max 0 (- (dec (count coll)) index)))
            bounded-count2 (gen/choose 0 (max 0 (dec bounded-count)))]
    [coll
     [index bounded-count bounded-count2]]))

(comment
  (gen/sample (gen-with-index-at-bounds gen/string) 10)
  (gen/sample (gen-with-index-and-count gen/string) 10))

(s/def ::uuid uuid?)
(s/def ::uuid-map (s/keys :req [::uuid]))
(def gen-uuid-map  (s/gen ::uuid-map))
(def gen-uuid-map-vec (gen/not-empty (gen/vector-distinct gen-uuid-map)))

;; ** Splittable types

(defn gen-indexed [coll-gen]
  (gen/bind
   coll-gen
   (fn [coll] (gen/return (indexed/vec coll)))))

(defn gen-indexed-by [key coll-gen]
  (gen/bind
   coll-gen
   (fn [coll] (gen/return (indexed/vec-by key coll)))))


;; * Properties

(extend-protocol indexed/IIndexedInternal
  #?(:clj Object :cljs default)
  (-consistent? [x] x))


(def split! (fn [x idx] (mapv indexed/-consistent? (sut/split x idx))))
(def splice! (comp indexed/-consistent? sut/splice))
(def append! (comp indexed/-consistent? sut/append))
(def omit! (comp indexed/-consistent? sut/omit))

(defn split-and-append-prop
  [gen]
  (testing "Splitting then merging should equal the original"
    (for-all
     [[coll idx] (gen-with-index-in-bounds gen)]
     (let [[l r]  (split! coll idx)
           merged (append! l r)]
       (= coll merged)))))

(defn splice-length-prop
  [gen]
  (testing "The count after splicing should equal the sum of the input counts"
    (for-all
     [[coll idx] (gen-with-index-at-bounds gen)
      other      gen]
     (let [spliced  (splice! coll idx other)]
       (= (+ (count coll) (count other))
          (count spliced))))))

(defn omit-length-prop
  [gen]
  (testing "The count after omiting should equal the subtraction of the input counts"
    (for-all
     [[coll [idx cnt]] (gen-with-index-and-count gen)]
     (let [omitted (omit! coll idx cnt)]
       (= (- (count coll) cnt) (count omitted))))))

(defn idempotency-prop
  [gen]
  (testing "The count after omiting should equal the subtraction of the input counts"
    (for-all
     [[coll [idx cnt cnt2]] (gen-with-index-and-counts gen)]
     (let [[a b]   (split! coll idx)
           [b1 b2] (split! b cnt)
           [b11 b12] (split! b1 cnt2)]
       (= coll
          (append! a b)
          (append! a (append! b1 b2))
          (append! a (append! (append! b11 b12) b2))
          (append! (append! a (append! b11 b12)) b2)
          (append! (append! a (append! b11 b12)) b2)
          (splice! (append! a b2) idx b1)
          (splice! (append! a b2) idx (append! b11 b12))
          (splice! (omit! coll idx cnt) idx b1)
          (splice! (omit! coll idx cnt) idx (append! b11 b12)))))))

;; * Tests

(def num-tests 200)

(defspec split-merge-string num-tests (split-and-append-prop gen/string))
(defspec split-merge-vec num-tests (split-and-append-prop (gen/vector gen/int)))
(defspec split-merge-indexed num-tests (split-and-append-prop (gen-indexed gen-uuid-map-vec)))
(defspec split-merge-indexed-by num-tests (split-and-append-prop (gen-indexed-by ::uuid gen-uuid-map-vec)))

(defspec splice-length-string num-tests (splice-length-prop gen/string))
(defspec splice-length-vec num-tests (splice-length-prop (gen/vector gen/int)))
(defspec splice-length-indexed num-tests (splice-length-prop (gen-indexed gen-uuid-map-vec)))
(defspec splice-length-indexed-by num-tests (splice-length-prop (gen-indexed-by ::uuid gen-uuid-map-vec)))

(defspec omit-length-string num-tests (omit-length-prop gen/string))
(defspec omit-length-vec num-tests (omit-length-prop (gen/vector gen/string)))
(defspec omit-length-indexed num-tests (omit-length-prop (gen-indexed gen-uuid-map-vec)))
(defspec omit-length-indexed-by num-tests (omit-length-prop (gen-indexed-by ::uuid gen-uuid-map-vec)))

(defspec idempotency-string num-tests (idempotency-prop gen/string))
(defspec idempotency-vec num-tests (idempotency-prop (gen/vector gen/int)))
(defspec idempotency-indexed num-tests (idempotency-prop (gen-indexed gen-uuid-map-vec)))
(defspec idempotency-indexed-by num-tests (idempotency-prop (gen-indexed-by ::uuid gen-uuid-map-vec)))
