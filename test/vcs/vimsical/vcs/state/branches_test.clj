(ns vimsical.vcs.state.branches-test
  (:require [clojure.test :as t :refer [are deftest]]
            [orchestra.spec.test :as st]
            [vimsical.common.test :refer [is=]]
            [vimsical.vcs.examples :as examples]
            [vimsical.vcs.state.branches :as sut]))

(st/instrument)

(deftest constructor-test
  (is= examples/delta-index (sut/new-delta-index examples/deltas)))

(deftest add-deltas-test
  (let [expected    examples/delta-index
        batches     (split-at (rand-int (count examples/deltas)) examples/deltas)
        actual      (reduce sut/add-deltas (sut/new-delta-index) batches)]
    (is= expected actual)))

(deftest index-of-test
  (are [index branch-uuid delta] (is= index (sut/index-of examples/delta-index delta))
    0 examples/master-id examples/d0
    0 examples/child-id  examples/d2
    0 examples/child-id  examples/d2
    2 examples/gchild-id examples/d7))
