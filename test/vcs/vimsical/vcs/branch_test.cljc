(ns vimsical.vcs.branch-test
  (:require
   [clojure.test :as t :refer [deftest is are testing]]
   [vimsical.vcs.branch :as sut]
   [vimsical.common.test :refer [diff= is=]]))

(deftest common-ancestor-test
  (is (nil? (sut/common-ancestor {:db/id :foo} {:db/id :foo})))
  (is (nil? (sut/common-ancestor {:db/id :foo} {:db/id :bar})))
  (is= {:db/id :master} (sut/common-ancestor
                         {:db/id       :gchild
                          ::sut/parent {:db/id       :child1
                                        ::sut/parent {:db/id :master}}}
                         {:db/id       :gchild2
                          ::sut/parent {:db/id       :child2
                                        ::sut/parent {:db/id :master}}})))

(deftest depth-test
  (let [master {:db/id :master}
        child  {:db/id :child ::sut/parent master}
        gchild {:db/id :gchild ::sut/parent child}]
    (are [depth base child] (is= depth (sut/depth base child))
      nil master master
      1   master child
      2   master gchild
      1   child  gchild
      nil child  master)))
