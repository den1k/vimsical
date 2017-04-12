(ns  vimsical.vcs.state.vims.branches.delta-index-test
  (:require
   [clojure.spec :as s]
   [orchestra.spec.test :as st]
   [clojure.test :as t :refer [deftest testing is are]]
   [vimsical.common.test :refer [is= diff=]]
   [vimsical.vcs.examples :as examples]
   [vimsical.vcs.state.vims.branches.delta-index :as sut]))

(st/instrument 'vimsical.vcs.state.vims.branches.delta-index)

(deftest add-deltas-test
  (let [expected    examples/delta-index
        batches     (split-at 3 examples/deltas)
        actual      (reduce sut/add-deltas (sut/new-delta-index) batches)]
    (is= expected actual)))

(deftest index-of-test
  (are [index branch-uuid delta] (is= index (sut/index-of examples/delta-index delta))
    0 examples/master-id examples/d0
    0 examples/child-id  examples/d2
    0 examples/child-id  examples/d2
    2 examples/gchild-id examples/d7))
