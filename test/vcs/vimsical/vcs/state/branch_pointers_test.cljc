(ns vimsical.vcs.state.branch-pointers-test
  #?@(:clj
      [(:require
        [clojure.test :as t :refer [deftest is]]
        [orchestra.spec.test :as st]
        [vimsical.vcs.examples :as examples]
        [vimsical.vcs.state.branch-pointers :as sut])]
      :cljs
      [(:require
        [clojure.spec.test :as st]
        [clojure.test :as t :refer-macros [deftest is]]
        [vimsical.vcs.examples :as examples]
        [vimsical.vcs.state.branch-pointers :as sut])]))

(st/instrument)

(deftest add-delta-test
  (is (= examples/branch-pointers-by-branch-id (reduce sut/add-delta sut/empty-branch-pointers-by-branch-id examples/deltas))))

(deftest add-deltas-test
  (is (= examples/branch-pointers-by-branch-id (sut/add-deltas sut/empty-branch-pointers-by-branch-id examples/deltas))))