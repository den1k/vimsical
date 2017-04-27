(ns vimsical.common.test-test
  (:require
   [clojure.test :as t :refer [deftest testing is]]
   [vimsical.common.test :as sut]))

(t/use-fixtures :each sut/uuid-fixture)

(deftest uuid-test
  (is (uuid? (sut/uuid)))
  (is (= (sut/uuid 1) (sut/uuid 1)))
  (is (not (= (sut/uuid 1) (sut/uuid 2)))))

(deftest uuid-seq-test
  (let [s (sut/uuid-seq)]
    (is (= (nth s 0) (nth s 0)))
    (is (not (= (nth s 0) (nth s 1))))))

(deftest uuid-gen-test
  (let [{:keys [seq f]} (sut/uuid-gen)
        [u0 u1 u2]      seq]
    (is (= u0 (f)))
    (is (= u1 (f)))
    (is (= u2 (f)))))
