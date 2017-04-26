(ns vimsical.vcs.alg.traversal-test
  #?@(:clj
      [(:require
        [clojure.test :as t :refer [are deftest is]]
        [orchestra.spec.test :as st]
        [vimsical.vcs.alg.traversal :as sut]
        [vimsical.vcs.examples :as examples])]
      :cljs
      [(:require
        [cljs.test :as t :refer-macros [are deftest is]]
        [clojure.spec.test :as st]
        [vimsical.vcs.alg.traversal :as sut]
        [vimsical.vcs.examples :as examples])]))

(st/instrument)

(defn ascending?  [comparison-result] (== sut/asc comparison-result))
(defn descending? [comparison-result] (== sut/desc comparison-result))

(deftest comparator-test
  (let [cpr (sut/new-branch-comparator examples/deltas-by-branch-id)]
    (are [pred left right] (is (pred (cpr left right))) ascending?  examples/master examples/child
         ascending?  examples/master examples/gchild
         ascending?  examples/child  examples/gchild
         descending? examples/child  examples/master
         descending? examples/gchild examples/master
         descending? examples/gchild examples/child)))

(deftest inlining-test
  (is (= examples/deltas (sut/inline examples/deltas-by-branch-id examples/branches))))
