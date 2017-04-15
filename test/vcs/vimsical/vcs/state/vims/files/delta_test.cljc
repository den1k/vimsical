(ns vimsical.vcs.state.vims.files.delta-test
  (:require
   [orchestra.spec.test :as st]
   [clojure.test :as t :refer [deftest is are]]
   [vimsical.vcs.examples :as examples]
   [vimsical.common.test :refer [uuid]]
   [vimsical.vcs.state.vims.files.delta :as sut]))

(st/instrument)

(deftest add-deltas-test
  (t/is (sut/add-deltas {} examples/deltas)))

(deftest get-deltas-test
  (let [state (sut/add-deltas {} examples/deltas)]
    (t/is (= "hi"  (::sut/string (sut/get-deltas state examples/id2 examples/file1-id))))
    (t/is (= "hi!" (::sut/string (sut/get-deltas state examples/id4 examples/file1-id))))))
