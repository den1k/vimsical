(ns vimsical.vcs.data.indexed.vector-test
  (:require
   [clojure.spec :as s]
   [vimsical.vcs.data.indexed.vector :as sut]
   [clojure.test :refer [deftest is are testing]]
   [vimsical.common.test :refer [is= isnt=]]))


;; * Data

(defn indexed-vector-test-data []
  (let [key->val (fn [k] (when k {:id k}))
        vals     (map key->val (range 0 100 10))
        iv       (sut/indexed-vector vals)
        ivb      (sut/indexed-vector-by :id vals)]
    {:key->val key->val :vals vals :iv iv :ivb ivb}))


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

(deftest indexed-vector-split-test
  (let [{:keys [iv ivb vals]}      (indexed-vector-test-data)
        split-index                3
        insert                     [:a]
        [expect-left expect-right] (split-at split-index vals)
        expect                     (into (into (vec expect-left) insert) expect-right)]
    (testing ""
      (is= expect (seq (sut/splice-at split-index iv insert)))
      (is= expect (seq (sut/splice-at split-index ivb insert))))))

(deftest indexed-vector-key-of-test
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


