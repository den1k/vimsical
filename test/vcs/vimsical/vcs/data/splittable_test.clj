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

(defn gen-with-index
  "Return a generator that will return tuples of: value from coll-gen, index in value."
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
            bounded-count (gen/choose 0 (max 0 (- (dec (count coll)) index)))]
    [coll
     [index bounded-count]]))

(comment
  (gen/sample (gen-with-index gen/string) 10)
  (gen/sample (gen-with-index (gen-rrb (gen/vector gen/int))) 10)
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

(defn split-and-append-prop
  [gen]
  (t/testing "Splitting then merging should equal the original"
    (prop/for-all
     [[coll idx] (gen-with-index gen)]
     (let [[l r]  (sut/split coll idx)
           merged (sut/append l r)]
       (= coll merged)))))

(defn splice-length-prop
  [gen]
  (t/testing "The count after splicing should equal the sum of the input counts"
    (prop/for-all
     [[coll idx] (gen-with-index gen)
      other      gen]
     (let [spliced  (sut/splice coll idx other)]
       (= (+ (count coll) (count other))
          (count spliced))))))

(defn omit-length-prop
  [gen]
  (t/testing "The count after omiting should equal the subtraction of the input counts"
    (prop/for-all
     [[coll [idx cnt]] (gen-with-index-and-count gen)]
     (let [omitted (sut/omit coll idx cnt)]
       (= (- (count coll) cnt) (count omitted))))))

(extend-protocol indexed/IIndexedInternal
  Object
  (-consistent? [x] x))

(defn idempotency-prop
  [gen]
  (t/testing "The count after omiting should equal the subtraction of the input counts"
    (prop/for-all
     [[coll [idx cnt]] (gen-with-index-and-count gen)]
     (let [[a b]   (sut/split coll idx)
           [b1 b2] (sut/split b cnt)]
       (= coll
          (indexed/-consistent? (sut/append a b))
          (indexed/-consistent? (sut/append a (sut/append b1 b2)))
          (indexed/-consistent? (sut/append (sut/append a b1) b2))
          (indexed/-consistent? (sut/splice (sut/append a b2) idx b1))
          (indexed/-consistent? (sut/splice (sut/omit coll idx cnt) idx b1)))))))

;; * Tests

(def num-tests 200)

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
(time
 (tc/defspec idempotency-indexed num-tests (idempotency-prop (gen-indexed gen-uuid-map-vec))))
(time
 (tc/defspec idempotency-indexed-by num-tests (idempotency-prop (gen-indexed-by ::uuid gen-uuid-map-vec))))
