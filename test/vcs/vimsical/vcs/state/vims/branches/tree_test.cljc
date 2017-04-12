(ns vimsical.vcs.state.vims.branches.tree-test
  (:require
   [orchestra.spec.test :as st]
   [clojure.test :as t :refer [deftest is are]]
   [vimsical.common.test :refer [diff= is=]]
   [vimsical.vcs.examples :as examples]
   [vimsical.vcs.state.vims.branches.tree :as sut]))

(st/instrument 'vimsical.vcs.state.vims.branches.tree)

(deftest branch-tree-test
  (is=  examples/branch-tree (sut/branch-tree examples/branches)))
