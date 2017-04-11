(ns vimsical.vcs.branch-test
  (:require
   [vimsical.vcs.branch :as sut]
   [vimsical.common.test :refer [diff= is=]]
   #?(:clj [clojure.test :as t]
      :cljs [cljs.test :as t :include-macros true])))

(t/deftest common-ancestor-test
  (t/is (nil? (sut/common-ancestor {:db/id :foo} {:db/id :foo})))
  (t/is (nil? (sut/common-ancestor {:db/id :foo} {:db/id :bar})))
  (is= {:db/id :master} (sut/common-ancestor
                         {:db/id       :gchild
                          ::sut/parent {:db/id       :child1
                                        ::sut/parent {:db/id :master}}}
                         {:db/id       :gchild2
                          ::sut/parent {:db/id       :child2
                                        ::sut/parent {:db/id :master}}})))

(t/deftest depth-test
  (let [master {:db/id :master}
        child  {:db/id :child  ::sut/parent master}
        gchild {:db/id :gchild ::sut/parent child}]
    (t/are [depth base child] (is= depth (sut/depth base child))
      nil master master
      1   master child
      2   master gchild
      1   child  gchild
      nil child  master)))
