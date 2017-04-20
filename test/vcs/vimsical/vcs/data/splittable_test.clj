(ns vimsical.vcs.data.splittable-test
  "TODO
  Check that we don't generate garbage from no-ops, like splicing an empty coll
  or omitting 0: test for identical? return values."
  (:require
   [clojure.spec :as s]
   [clojure.test :as t]
   [clojure.test.check :as check]
   [clojure.test.check.clojure-test :as tc]
   [clojure.test.check.generators :as gen]
   [clojure.test.check.properties :as prop]
   [orchestra.spec.test :as st]
   [vimsical.vcs.data.splittable :as sut]
   [clojure.core.rrb-vector :as rrb]
   [vimsical.vcs.data.indexed.vector :as indexed]))

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
  (gen/bind
   coll-gen
   (fn [coll]
     (gen/tuple
      (gen/return coll)
      (gen/bind
       (gen/choose 0 (max 0 (dec (count coll))))
       (fn [index]
         (gen/tuple
          (gen/return index)
          (gen/choose 0 (max 0 (- (dec (count coll)) index))))))))))

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
  (gen/sample (gen-with-index-at-bounds (gen-rrb (gen/vector gen/int))) 10)
  (gen/sample (gen-with-index-and-count gen/string) 10))

(s/def ::uuid uuid?)
(s/def ::uuid-map (s/keys :req [::uuid]))
(def gen-uuid-map  (s/gen ::uuid-map))
(def gen-uuid-map-vec (gen/not-empty (gen/vector-distinct gen-uuid-map)))

;; ** Splittable types

(defn gen-rrb [coll-gen]
  (gen/bind
   coll-gen
   (fn [coll] (gen/return (rrb/vec coll)))))

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
  Object
  (indexed/-consistent? [x] x))


(def split! (fn [x idx] (mapv indexed/-consistent? (sut/split x idx))))
(def splice! (comp indexed/-consistent? sut/splice))
(def append! (comp indexed/-consistent? sut/append))
(def omit! (comp indexed/-consistent? sut/omit))

(defn split-and-append-prop
  [gen]
  (t/testing "Splitting then merging should equal the original"
    (prop/for-all
     [[coll idx] (gen-with-index-in-bounds gen)]
     (let [[l r]  (split! coll idx)
           merged (append! l r)]
       (= coll merged)))))

(defn splice-length-prop
  [gen]
  (t/testing "The count after splicing should equal the sum of the input counts"
    (prop/for-all
     [[coll idx] (gen-with-index-at-bounds gen)
      other      gen]
     (let [spliced  (splice! coll idx other)]
       (= (+ (count coll) (count other))
          (count spliced))))))

(defn omit-length-prop
  [gen]
  (t/testing "The count after omiting should equal the subtraction of the input counts"
    (prop/for-all
     [[coll [idx cnt]] (gen-with-index-and-count gen)]
     (let [omitted (omit! coll idx cnt)]
       (= (- (count coll) cnt) (count omitted))))))

(defn idempotency-prop
  [gen]
  (t/testing "The count after omiting should equal the subtraction of the input counts"
    (prop/for-all
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

(def num-tests 2000)

(tc/defspec split-merge-string num-tests (split-and-append-prop gen/string))
(tc/defspec split-merge-vec num-tests (split-and-append-prop (gen/vector gen/int)))
(tc/defspec split-merge-rrb num-tests (split-and-append-prop (gen-rrb (gen/vector gen/int))))
(tc/defspec split-merge-indexed num-tests (split-and-append-prop (gen-indexed gen-uuid-map-vec)))
(tc/defspec split-merge-indexed-by num-tests (split-and-append-prop (gen-indexed-by ::uuid gen-uuid-map-vec)))

(tc/defspec splice-length-string num-tests (splice-length-prop gen/string))
(tc/defspec splice-length-vec num-tests (splice-length-prop (gen/vector gen/int)))
(tc/defspec splice-length-rrb num-tests (splice-length-prop (gen-rrb (gen/vector gen/int))))
(tc/defspec splice-length-indexed num-tests (splice-length-prop (gen-indexed gen-uuid-map-vec)))
(tc/defspec splice-length-indexed-by num-tests (splice-length-prop (gen-indexed-by ::uuid gen-uuid-map-vec)))

(tc/defspec omit-length-string num-tests (omit-length-prop gen/string))
(tc/defspec omit-length-vec num-tests (omit-length-prop (gen/vector gen/string)))
(tc/defspec omit-length-rrb num-tests (omit-length-prop (gen-rrb (gen/vector gen/int))))
(tc/defspec omit-length-indexed num-tests (omit-length-prop (gen-indexed gen-uuid-map-vec)))
(tc/defspec omit-length-indexed-by num-tests (omit-length-prop (gen-indexed-by ::uuid gen-uuid-map-vec)))

(tc/defspec idempotency-string num-tests (idempotency-prop gen/string))
(tc/defspec idempotency-vec num-tests (idempotency-prop (gen/vector gen/int)))
;; Showstopper bug?
;; http://dev.clojure.org/jira/browse/CRRBV-14
;; (tc/defspec idempotency-rrb num-tests (idempotency-prop (gen-rrb (gen/vector gen/int))))
(tc/defspec idempotency-indexed num-tests (idempotency-prop (gen-indexed gen-uuid-map-vec)))
(tc/defspec idempotency-indexed-by num-tests (idempotency-prop (gen-indexed-by ::uuid gen-uuid-map-vec)))
