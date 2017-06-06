(ns vimsical.vcs.branch-test
  #?@(:clj
      [(:require
        [clojure.test :as t :refer [are deftest is]]
        [orchestra.spec.test :as st]
        [vimsical.vcs.branch :as sut]
        [vimsical.vcs.examples :as examples])]
      :cljs
      [(:require
        [clojure.spec.test.alpha :as st]
        [clojure.test :as t :refer-macros [are deftest is]]
        [vimsical.vcs.branch :as sut]
        [vimsical.vcs.examples :as examples])]))

(st/instrument)

(deftest lineage-test
  (is (= [examples/master]) (sut/lineage examples/master))
  (is (= [examples/child examples/master]) (sut/lineage examples/child))
  (is (= [examples/gchild examples/child examples/master]) (sut/lineage examples/gchild)))

(deftest ancestors-test
  (is (nil? (sut/ancestors examples/master)))
  (is (= [examples/master] (sut/ancestors examples/child)))
  (is (= [examples/child examples/master] (sut/ancestors examples/gchild))))

(deftest common-ancestor-test
  (are [a b expect] (is (= expect (sut/common-ancestor a b) (sut/common-ancestor b a)))
    examples/master examples/master nil
    examples/master examples/child  nil
    examples/child  examples/gchild examples/master))

(deftest depth-test
  (are [depth branch] (is (= depth) (sut/depth branch))
    0 examples/master
    1 examples/child
    2 examples/gchild))

(deftest relative-depth-test
  (are [depth base child] (is (= depth (sut/relative-depth base child)))
    0   examples/master examples/master
    0   examples/child  examples/child
    0   examples/gchild examples/gchild
    1   examples/master examples/child
    1   examples/child  examples/gchild
    2   examples/master examples/gchild
    nil examples/gchild examples/master
    nil examples/child  examples/master
    nil examples/gchild examples/child))
