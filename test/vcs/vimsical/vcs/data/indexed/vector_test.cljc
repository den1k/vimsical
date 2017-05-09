(ns vimsical.vcs.data.indexed.vector-test
  #?@(:clj
      [(:require
        [clojure.spec :as s]
        [clojure.test :refer [are deftest is testing]]
        [orchestra.spec.test :as st]
        [vimsical.vcs.data.indexed.vector :as sut]
        [vimsical.vcs.data.splittable :as splittable])]
      :cljs
      [(:require
        [cljs.test :refer-macros [are deftest is testing]]
        [clojure.spec :as s]
        [clojure.spec.test :as st]
        [vimsical.vcs.data.indexed.vector :as sut]
        [vimsical.vcs.data.splittable :as splittable])]))

(st/instrument)

;;
;; * Data
;;

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

;;
;; * Tests
;;

(deftest spec-test
  (is (s/valid? ::sut/vector (sut/vector)))
  (is (s/valid? ::sut/vector (sut/vec []))))

(deftest conversion-idempotency-test
  (let [expected [1 2 3]
        actual   (sut/vec expected)]
    (is (= expected (vec actual)))))

(deftest vector-test
  (let [{:keys [v]} (test-data)]
    (are [val idx] (is (= idx (sut/index-of v val)))
      {:id 0}  0
      {:id 50} 5
      {:id 90} 9)))

(deftest vector-by-test
  (let [{:keys [vb]} (test-data)]
    (are [k idx] (is (= idx (sut/index-of vb k)))
      0  0
      50 5
      90 9)))

(deftest nth-test
  (let [{:keys [v vb]} (test-data)]
    (are [val idx] (do (is (= val (nth v idx)))
                       (is (= val (nth vb idx))))
      {:id 0}  0
      {:id 50} 5
      {:id 90} 9)))

(deftest peek-test
  (let [{:keys [v vb]} (test-data)]
    (is (= {:id 90} (peek v)))
    (is (= {:id 90} (peek vb)))))

(deftest update-test
  (let [{:keys [v vb]} (test-data)]
    (is (thrown?
         #?(:clj clojure.lang.ExceptionInfo :cljs cljs.core.ExceptionInfo)
         (= {:id 0 :foo :bar} (first (update v 0 assoc :foo :bar)))))
    (is (= {:id 0 :foo :bar} (first (update vb 0 assoc :foo :bar))))))

(deftest next-test
  (testing "equality"
    (is (= (-> [:z :a :b :c]
               sut/vec
               next)
           (-> [:a :b]
               sut/vec
               (conj :c))
           (-> [:z :a :b]
               sut/vec
               next
               (conj :c)))))
  (testing "internal state"
    (let [v  (next (sut/vec [:a :b :c]))
          v' (conj v :d)]
      (testing "next"
        (is (= #?(:clj (.v v) :cljs (.-v v)) [:b :c]))
        (is (= 0 (sut/index-of v :b))))
      (testing "next + conj"
        (is (= #?(:clj (.v v') :cljs (.-v v')) [:b :c :d]))
        (is (= 0 (sut/index-of v' :b)))
        (is (= 2 (sut/index-of v' :d)))))))

(deftest split-test
  (let [{:keys [v vb]} (test-data)
        split-index    (rand-int (count v))
        split2-index   (rand-int split-index)
        [l-v r-v]      (splittable/split v split-index)
        [ll-v lr-v]    (splittable/split l-v split2-index)
        [l-vb r-vb]    (splittable/split vb split-index)]
    (is (sut/-consistent? l-v))
    (is (sut/-consistent? ll-v))
    (is (sut/-consistent? lr-v))
    (is (sut/-consistent? r-v))
    (is (sut/-consistent? l-vb))
    (is (sut/-consistent? r-vb))
    (is (= v (into l-v r-v)))
    (is (= vb (into l-vb r-vb )))))

(deftest append-test
  (let [{v1 :v vb1 :vb}             (test-data (range 0 3))
        {v2 :v vb2 :vb}             (test-data (range 3 6))
        {v3 :v vb3 :vb}             (test-data (range 6 9))
        {v-expect :v vb-expect :vb} (test-data (range 0 9))
        actual-v                    (splittable/append (splittable/append v1 v2) v3)
        actual-vb                   (splittable/append (splittable/append vb1 vb2) vb3)]
    (is (sut/-consistent? actual-v))
    (is (sut/-consistent? actual-vb))
    (is (= v-expect  (seq actual-v)))
    (is (= vb-expect (seq actual-vb)))
    (are [val idx] (is (= idx (sut/index-of actual-v val)))
      {:id 0} 0
      {:id 4} 4
      {:id 8} 8)))

(deftest split-append-test
  (let [{:keys [v vb]} (test-data)
        v'             (apply splittable/append (splittable/split v (rand-int (count v))))
        vb'            (apply splittable/append (splittable/split vb (rand-int (count v))))]
    (is (sut/-consistent? v'))
    (is (sut/-consistent? vb'))
    (is (= v v'))
    (is (= vb vb'))))

(deftest splice-test
  (let [{:keys [v vb vals]} (test-data)
        idx                 3
        spliced-v1           (sut/vec [{:id 1000}])
        spliced-v2           (sut/vec [{:id 2000}])
        spliced-vb1          (sut/vec-by :id [{:id 1000}])
        spliced-vb2          (sut/vec-by :id [{:id 2000}])
        expect              (splittable/splice (splittable/splice (vec vals) idx (seq spliced-v1)) idx (seq spliced-v2))
        actual-v            (splittable/splice (splittable/splice v idx spliced-v1) idx spliced-v2)
        actual-vb           (splittable/splice (splittable/splice vb idx spliced-vb1) idx spliced-vb2)]
    (is (sut/-consistent? actual-v))
    (is (sut/-consistent? actual-vb))
    (is (= expect (seq actual-v)))
    (is (= expect (seq actual-vb)))))

(deftest index-of-test
  (let [{:keys [key->val v vb]} (update-test-data (test-data))]
    (are [idx k] (do (is (= idx (sut/index-of v (key->val k))))
                     (is (= (key->val k) (nth v idx)))
                     (is (= idx (sut/index-of vb k)))
                     (is (= (key->val k) (nth vb idx))))
      0  0
      5  50
      9  90
      10 100
      14 140)))

(deftest equiv-test
  (let [{:keys [v vb]} (test-data (range 0 10))]
    (are [a b] (= a b)
      v v)))
