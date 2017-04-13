(ns vimsical.vcs.state.vims.branches.tree-test
  (:require
   [clojure.test :as t :refer [deftest]]
   [orchestra.spec.test :as st]
   [vimsical.common.test :refer [is=]]
   [vimsical.vcs.examples :as examples]
   [vimsical.vcs.state.vims.branches.tree :as sut]))

(st/instrument)

(deftest branch-tree-test
  (is=  examples/branch-tree (sut/branch-tree examples/branches)))
