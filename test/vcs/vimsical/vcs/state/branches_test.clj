(ns vimsical.vcs.state.branches-test
  (:require
   [clojure.test :as t :refer [is are deftest]]
   [orchestra.spec.test :as st]
   [vimsical.vcs.examples :as examples]
   [vimsical.vcs.state.branches :as sut]))

(st/instrument)

(deftest constructor-test
  (is (= examples/deltas-by-branch-id (sut/add-deltas sut/empty-deltas-by-branch-id examples/deltas))))

(deftest add-deltas-test
  (let [expected    examples/deltas-by-branch-id
        batches     (split-at (rand-int (count examples/deltas)) examples/deltas)
        actual      (reduce sut/add-deltas sut/empty-deltas-by-branch-id batches)]
    (is (= expected actual))))

(deftest index-of-test
  (are [index branch-uuid delta] (is (= index (sut/index-of-delta examples/deltas-by-branch-id delta)))
    0 examples/master-id examples/d0
    0 examples/child-id  examples/d2
    0 examples/child-id  examples/d2
    2 examples/gchild-id examples/d7))
