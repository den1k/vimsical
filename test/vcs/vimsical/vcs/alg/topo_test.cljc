(ns vimsical.vcs.alg.topo-test
  #?@(:clj
      [(:require
        [clojure.test :as t :refer [deftest is testing]]
        [orchestra.spec.test :as st]
        [vimsical.common.test :refer [uuid]]
        [vimsical.vcs.alg.topo :as sut])]
      :cljs
      [(:require
        [cljs.test :as t :refer [deftest is testing]]
        [clojure.spec.test.alpha :as st]
        [vimsical.common.test :refer [uuid]]
        [vimsical.vcs.alg.topo :as sut])]))

(st/instrument)

(deftest topo-test
  (testing "predicate across branches"
    (let [sorted
          [{:branch-uid (uuid :master), :uid (uuid :0) :prev-uid nil :pad 0 :op,,,,,, [:str/ins nil       "a"] :file-uid (uuid :file)}
           {:branch-uid (uuid :master), :uid (uuid :1) :prev-uid (uuid :0) :pad 0 :op [:str/ins (uuid :0) "a"] :file-uid (uuid :file)}
           {:branch-uid (uuid :child1), :uid (uuid :2) :prev-uid (uuid :1) :pad 0 :op [:str/ins (uuid :1) "a"] :file-uid (uuid :file)}
           {:branch-uid (uuid :child1), :uid (uuid :3) :prev-uid (uuid :2) :pad 0 :op [:str/ins (uuid :2) "a"] :file-uid (uuid :file)}
           {:branch-uid (uuid :child2), :uid (uuid :4) :prev-uid (uuid :3) :pad 0 :op [:str/ins (uuid :3) "a"] :file-uid (uuid :file)}
           {:branch-uid (uuid :child2), :uid (uuid :5) :prev-uid (uuid :4) :pad 0 :op [:str/ins (uuid :4) "a"] :file-uid (uuid :file)}
           {:branch-uid (uuid :child21) :uid (uuid :6) :prev-uid (uuid :5) :pad 0 :op [:str/ins (uuid :5) "a"] :file-uid (uuid :file)}
           {:branch-uid (uuid :child21) :uid (uuid :7) :prev-uid (uuid :6) :pad 0 :op [:str/ins (uuid :6) "a"] :file-uid (uuid :file)}
           {:branch-uid (uuid :child11) :uid (uuid :8) :prev-uid (uuid :7) :pad 0 :op [:str/ins (uuid :7) "a"] :file-uid (uuid :file)}
           {:branch-uid (uuid :child11) :uid (uuid :9) :prev-uid (uuid :8) :pad 0 :op [:str/ins (uuid :8) "a"] :file-uid (uuid :file)}]
          nsorted (shuffle sorted)]
      (is (sut/sorted? sorted))
      (is (not (sut/sorted? nsorted)))))
  (testing "sort across branches"
    (let [deltas
          [{:branch-uid (uuid :master) :uid (uuid :0), :prev-uid nil :pad 0 :op,,,,,, [:str/ins nil       "a"] :file-uid (uuid :file)}
           {:branch-uid (uuid :master) :uid (uuid :1), :prev-uid (uuid :0) :pad 0 :op [:str/ins (uuid :0) "a"] :file-uid (uuid :file)}
           {:branch-uid (uuid :child1) :uid (uuid :2), :prev-uid (uuid :1) :pad 0 :op [:str/ins nil       "a"] :file-uid (uuid :file)}
           {:branch-uid (uuid :child1) :uid (uuid :3), :prev-uid (uuid :2) :pad 0 :op [:str/ins (uuid :2) "a"] :file-uid (uuid :file)}
           {:branch-uid (uuid :child2) :uid (uuid :4), :prev-uid (uuid :3) :pad 0 :op [:str/ins (uuid :1) "a"] :file-uid (uuid :file)}
           {:branch-uid (uuid :child2) :uid (uuid :5), :prev-uid (uuid :4) :pad 0 :op [:str/ins (uuid :4) "a"] :file-uid (uuid :file)}
           {:branch-uid (uuid :child21) :uid (uuid :6) :prev-uid (uuid :5) :pad 0 :op [:str/ins (uuid :5) "a"] :file-uid (uuid :file)}
           {:branch-uid (uuid :child21) :uid (uuid :7) :prev-uid (uuid :6) :pad 0 :op [:str/ins (uuid :6) "a"] :file-uid (uuid :file)}
           {:branch-uid (uuid :child11) :uid (uuid :8) :prev-uid (uuid :7) :pad 0 :op [:str/ins (uuid :7) "a"] :file-uid (uuid :file)}
           {:branch-uid (uuid :child11) :uid (uuid :9) :prev-uid (uuid :8) :pad 0 :op [:str/ins (uuid :8) "a"] :file-uid (uuid :file)}]
          nsorted (shuffle deltas)
          sorted  (sut/sort (shuffle deltas))]
      (is (sut/sorted? sorted))
      (is (not (sut/sorted? nsorted))))))
