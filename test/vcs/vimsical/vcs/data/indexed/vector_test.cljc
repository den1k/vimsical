(ns vimsical.vcs.data.indexed.vector-test
  (:require
   [clojure.test :refer [are deftest testing]]
   [orchestra.spec.test :as st]
   [vimsical.common.test :refer [is= isnt=]]
   [vimsical.vcs.data.indexed.vector :as sut]))

(st/instrument)

;; * Data

(defn indexed-vector-test-data
  ([] (indexed-vector-test-data (range 0 100 10)))
  ([range]
   (let [key->val (fn [k] (when k {:id k}))
         vals     (map key->val range)
         iv       (sut/indexed-vector vals)
         ivb      (sut/indexed-vector-by :id vals)]
     {:key->val key->val :vals vals :iv iv :ivb ivb})))


(defn update-test-data [{:keys [key->val] :as data}]
  (let [new-val   (key->val 100)
        new-vals  (map key->val (range 110 150 10))
        update-fn (fn [iv]  (-> iv  (conj new-val) (into new-vals)))]
    (-> data
        (update :iv  update-fn)
        (update :ivb update-fn))))


;; * Tests

(deftest indexed-vector-test
  (let [{:keys [iv]} (indexed-vector-test-data)]
    (are [val idx] (is= idx (sut/index-of iv val))
      {:id 0}  0
      {:id 50} 5
      {:id 90} 9)))

(deftest indexed-vector-by-test
  (let [{:keys [ivb]} (indexed-vector-test-data)]
    (are [k idx] (is= idx (sut/index-of ivb k))
      0  0
      50 5
      90 9)))

(deftest indexed-vector-nth-test
  (let [{:keys [iv ivb]} (indexed-vector-test-data)]
    (are [val idx] (do (is= val (nth iv idx))
                       (is= val (nth ivb idx)))
      {:id 0}  0
      {:id 50} 5
      {:id 90} 9)))

(deftest indexed-vector-split-at-test
  (let [{:keys [iv ivb]}   (indexed-vector-test-data)]
    (is= iv (apply into (sut/split-at (rand-int (count iv)) iv)))
    (is= ivb (apply into (sut/split-at (rand-int (count iv)) ivb)))))

(deftest indexed-vector-next-test
  (testing "equality"
    (is= (-> [:z :a :b :c]
             sut/indexed-vector
             next)
         (-> [:a :b]
             sut/indexed-vector
             (conj :c))
         (-> [:z :a :b]
             sut/indexed-vector
             next
             (conj :c))))
  (testing "internal state"
    (let [v  (next (sut/indexed-vector [:a :b :c]))
          v' (conj v :d)]
      (testing "next"
        (is= -1 (.offset v))
        (is= (.index v) {:b 1 :c 2})
        (is= (.v v) [:b :c])
        (is= 0 (sut/index-of v :b)))
      (testing "next + conj"
        (is= (.index v') {:b 1 :c 2 :d 3})
        (is= (.v v') [:b :c :d])
        (is= 0 (sut/index-of v' :b))
        (is= 2 (sut/index-of v' :d))))))

(deftest indexed-vector-split-test
  (let [{:keys [iv ivb]} (indexed-vector-test-data)]
    (testing "idempotency"
      (is= iv (apply into (sut/split-at 3 iv)))
      (is= ivb (apply into (sut/split-at 3 ivb))))
    (testing "doesn't support concat"
      (isnt= iv (apply concat (split-at 3 iv)))
      (isnt= ivb (apply concat (split-at 3 ivb))))))

(deftest indexed-vector-splce-test
  (let [{:keys [iv ivb vals]}      (indexed-vector-test-data)
        split-index                3
        insert-iv                  (sut/indexed-vector [{:id 1000}])
        insert-ivb                 (sut/indexed-vector-by :id [{:id 1000}])
        [expect-left expect-right] (split-at split-index vals)
        expect                     (into (into (vec expect-left) insert-iv) expect-right)]
    (is= expect (seq (sut/splice-at split-index iv insert-iv)))
    (is= expect (seq (sut/splice-at split-index ivb insert-ivb)))))

(deftest indexed-vector-concat-test
  (let [{iv1 :iv ivb1 :ivb}             (indexed-vector-test-data (range 0 10))
        {iv2 :iv ivb2 :ivb}             (indexed-vector-test-data (range 10 20))
        {iv-expect :iv ivb-expect :ivb} (indexed-vector-test-data (range 0 20))
        iv-actual                       (sut/concat iv1 iv2)
        ivb-actual                      (sut/concat ivb1 ivb2)]
    (is= iv-expect  (seq iv-actual))
    (is= ivb-expect (seq ivb-actual))
    (are [val idx] (is= idx (sut/index-of iv-actual val))
      {:id 0}  0
      {:id 10} 10
      {:id 19} 19)))

(deftest indexed-vector-index-of-test
  (let [{:keys [key->val iv ivb]} (update-test-data (indexed-vector-test-data))]
    (are [idx k] (do (is= idx (sut/index-of iv (key->val k)))
                     (is= (key->val k) (nth iv idx))
                     (is= idx (sut/index-of ivb k))
                     (is= (key->val k) (nth ivb idx)))
      0  0
      5  50
      9  90
      10 100
      14 140)))

(deftest indexed-vector-equiv-test
  (let [{:keys [iv ivb]} (indexed-vector-test-data (range 0 10))]
    (are [a b] (= a b)
      iv iv)))
