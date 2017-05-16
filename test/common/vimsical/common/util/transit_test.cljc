(ns vimsical.common.util.transit-test
  (:require
   [clojure.test :refer [deftest is are testing]]
   [vimsical.common.util.transit :as sut]))

(deftest io-test
  (let [val    {:foo {:bar (into (sorted-map) {2 2 1 1})}}
        string "[\"^ \",\"~:foo\",[\"^ \",\"~:bar\",[\"~#sorted-map\",[\"^ \",\"~i1\",1,\"~i2\",2]]]]"]
    (is (= val (sut/read-transit string)))
    (is (= string (sut/write-transit val)))
    (is (= val (-> val sut/write-transit sut/read-transit)))))
