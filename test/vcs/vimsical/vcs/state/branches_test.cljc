(ns vimsical.vcs.state.branches-test
  #?@(:clj
      [(:require
        [clojure.test :as t :refer [are deftest is]]
        [orchestra.spec.test :as st]
        [vimsical.vcs.examples :as examples]
        [vimsical.vcs.state.branches :as sut])]
      :cljs
      [(:require
        [clojure.spec.test :as st]
        [clojure.test :as t :refer-macros [are deftest is]]
        [vimsical.vcs.examples :as examples]
        [vimsical.vcs.state.branches :as sut])]))

(st/instrument)

(deftest constructor-test
  (is (= examples/deltas-by-branch-uid (sut/add-deltas sut/empty-deltas-by-branch-uid examples/deltas))))

(deftest add-delta-test
  (let [expected examples/deltas-by-branch-uid
        actual   (reduce sut/add-delta sut/empty-deltas-by-branch-uid examples/deltas)]
    (is (= expected actual))))

(deftest add-deltas-test
  (let [expected examples/deltas-by-branch-uid
        batches  (split-at (rand-int (count examples/deltas)) examples/deltas)
        actual   (reduce sut/add-deltas sut/empty-deltas-by-branch-uid batches)]
    (is (= expected actual))))

(deftest index-of-test
  (are [index branch-uid delta] (is (= index (sut/index-of-delta examples/deltas-by-branch-uid delta)))
    0 examples/master-uid examples/d0
    0 examples/child-uid  examples/d2
    0 examples/child-uid  examples/d2
    2 examples/gchild-uid examples/d7))
