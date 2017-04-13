(ns vimsical.vcs.alg.traversal-test
  (:require
   [orchestra.spec.test :as st]
   [vimsical.common.test :refer [is= diff=]]
   [clojure.test :as t :refer [deftest testing is are]]
   [vimsical.vcs.examples :as examples]
   [vimsical.vcs.alg.traversal :as sut]))

(st/instrument)

(defn ascending?  [comparison-result] (== (var-get #'sut/asc) comparison-result))
(defn descending? [comparison-result] (== (var-get #'sut/desc) comparison-result))

(deftest comparator-test
  (let [cpr (sut/new-branch-comparator examples/delta-index)]
    (are [pred left right] (is (pred (cpr left right)))
      ascending?  examples/master examples/child
      ascending?  examples/master examples/gchild
      ascending?  examples/child  examples/gchild
      descending? examples/child  examples/master
      descending? examples/gchild examples/master
      descending? examples/gchild examples/child)))

(deftest inlining-test
  (is= examples/deltas (sut/inline examples/delta-index examples/branches)))
