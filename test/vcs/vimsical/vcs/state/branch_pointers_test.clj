(ns vimsical.vcs.state.branch-pointers-test
  (:require
   [clojure.test :as t :refer [are deftest is testing]]
   [orchestra.spec.test :as st]
   [vimsical.common.test :refer [uuid is=]]
   [vimsical.vcs.examples :as examples]
   [vimsical.vcs.state.branch-pointers :as sut]))

(st/instrument)

(deftest add-deltas-test
  (is= examples/branch-pointers-by-branch-id (sut/add-deltas sut/empty-branch-pointers-by-branch-id examples/deltas)))
