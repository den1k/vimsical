(ns vimsical.vcs.data.indexed.vector-test
  (:require
   [clojure.spec :as s]
   [clojure.test :refer [is are deftest testing]]
   [orchestra.spec.test :as st]
   [vimsical.common.test :refer [is= isnt=]]
   [vimsical.vcs.data.splittable :as splittable]
   [vimsical.vcs.data.indexed.vector :as sut]))

(st/instrument)

;; * Data

(defn test-data
  ([] (test-data (range 0 100 10)))
  ([range]
   (let [key->val (fn [k] (when k {:id k}))
         vals     (map key->val range)
         v        (sut/vec vals)
         vb       (sut/vec-by :id vals)]
     {:key->val key->val :vals vals :v v :vb vb})))


(defn update-test-data [{:keys [key->val] :as data}]
  (let [new-val   (key->val 100)
        new-vals  (map key->val (range 110 150 10))
        update-fn (fn [v]  (-> v  (conj new-val) (into new-vals)))]
    (-> data
        (update :v  update-fn)
        (update :vb update-fn))))


;; * Tests

(deftest spec-test
  (is (s/valid? ::sut/vector (sut/vector)))
  (is (s/valid? ::sut/vector (sut/vec []))))

(deftest vector-test
  (let [{:keys [v]} (test-data)]
    (are [val idx] (is= idx (sut/index-of v val))
      {:id 0}  0
      {:id 50} 5
      {:id 90} 9)))

(deftest vector-by-test
  (let [{:keys [vb]} (test-data)]
    (are [k idx] (is= idx (sut/index-of vb k))
      0  0
      50 5
      90 9)))

(deftest nth-test
  (let [{:keys [v vb]} (test-data)]
    (are [val idx] (do (is= val (nth v idx))
                       (is= val (nth vb idx)))
      {:id 0}  0
      {:id 50} 5
      {:id 90} 9)))

(deftest next-test
  (testing "equality"
    (is= (-> [:z :a :b :c]
             sut/vec
             next)
         (-> [:a :b]
             sut/vec
             (conj :c))
         (-> [:z :a :b]
             sut/vec
             next
             (conj :c))))
  (testing "internal state"
    (let [v  (next (sut/vec [:a :b :c]))
          v' (conj v :d)]
      (testing "next"
        (is= (.v v) [:b :c])
        (is= 0 (sut/index-of v :b)))
      (testing "next + conj"
        (is= (.v v') [:b :c :d])
        (is= 0 (sut/index-of v' :b))
        (is= 2 (sut/index-of v' :d))))))

(deftest split-test
  (let [{:keys [v vb]} (test-data)
        [l-v r-v]      (splittable/split v (rand-int (count v)))
        [l-vb r-vb]    (splittable/split vb (rand-int (count v)))]
    (is (sut/-consistent? l-v))
    (is (sut/-consistent? r-v))
    (is (sut/-consistent? l-vb))
    (is (sut/-consistent? r-vb))
    (is= v (into l-v r-v))
    (is= vb (into l-vb r-vb ))))

(deftest append-test
  (let [{iv1 :v ivb1 :vb}             (test-data (range 0 10))
        {iv2 :v ivb2 :vb}             (test-data (range 10 20))
        {iv-expect :v ivb-expect :vb} (test-data (range 0 20))
        actual-v                      (splittable/append iv1 iv2)
        actual-vb                     (splittable/append ivb1 ivb2)]
    (is (sut/-consistent? actual-v))
    (is (sut/-consistent? actual-vb))
    (is= iv-expect  (seq actual-v))
    (is= ivb-expect (seq actual-vb))
    (are [val idx] (is= idx (sut/index-of actual-v val))
      {:id 0}  0
      {:id 10} 10
      {:id 19} 19)))

(deftest split-append-test
  (let [{:keys [v vb]} (test-data)
        v'             (apply splittable/append (splittable/split v (rand-int (count v))))
        vb'            (apply splittable/append (splittable/split vb (rand-int (count v))))]
    (is (sut/-consistent? v'))
    (is (sut/-consistent? vb'))
    (is= v v')
    (is= vb vb')))

(deftest splice-test
  (let [{:keys [v vb vals]} (test-data)
        idx                 3
        spliced-v           (sut/vec [{:id 1000}])
        spliced-vb          (sut/vec-by :id [{:id 1000}])
        expect              (splittable/splice (vec vals) idx spliced-v)
        actual-v            (splittable/splice v idx spliced-v)
        actual-vb           (splittable/splice vb idx spliced-vb)]
    (is (sut/-consistent? actual-v))
    (is (sut/-consistent? actual-vb))
    (is= expect (seq actual-v))
    (is= expect (seq actual-vb))))

(deftest index-of-test
  (let [{:keys [key->val v vb]} (update-test-data (test-data))]
    (are [idx k] (do (is= idx (sut/index-of v (key->val k)))
                     (is= (key->val k) (nth v idx))
                     (is= idx (sut/index-of vb k))
                     (is= (key->val k) (nth vb idx)))
      0  0
      5  50
      9  90
      10 100
      14 140)))

(deftest equiv-test
  (let [{:keys [v vb]} (test-data (range 0 10))]
    (are [a b] (= a b)
      v v)))
