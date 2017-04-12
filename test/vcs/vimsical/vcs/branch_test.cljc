(ns vimsical.vcs.branch-test
  (:require
   [orchestra.spec.test :as st]
   [clojure.test :as t :refer [deftest is are testing]]
   [vimsical.vcs.examples :as examples]
   [vimsical.vcs.branch :as sut]
   [vimsical.common.test :refer [diff= is=]]))

(st/instrument 'vimsical.vcs.branch)

(deftest lineage-test
  (is= [examples/master] (sut/lineage examples/master))
  (is= [examples/child examples/master] (sut/lineage examples/child))
  (is= [examples/gchild examples/child examples/master] (sut/lineage examples/gchild)))

(deftest ancestors-test
  (is= nil (sut/ancestors examples/master))
  (is= [examples/master] (sut/ancestors examples/child))
  (is= [examples/child examples/master] (sut/ancestors examples/gchild)))

(deftest common-ancestor-test
  (is (nil? (sut/common-ancestor examples/master examples/master)))
  (is (nil? (sut/common-ancestor examples/master examples/child)))
  (is= examples/master (sut/common-ancestor examples/child examples/gchild)))

(deftest depth-test
  (are [depth branch] (is= depth (sut/depth branch))
    0 examples/master
    1 examples/child
    2 examples/gchild))

(deftest relative-depth-test
  (are [depth base child] (is= depth (sut/relative-depth base child))
    0   examples/master examples/master
    0   examples/child  examples/child
    0   examples/gchild examples/gchild
    1   examples/master examples/child
    1   examples/child  examples/gchild
    2   examples/master examples/gchild
    nil examples/gchild examples/master
    nil examples/child  examples/master
    nil examples/gchild examples/child))
