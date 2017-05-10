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
        [clojure.spec.test :as st]
        [vimsical.common.test :refer [uuid]]
        [vimsical.vcs.alg.topo :as sut])]))

(st/instrument)

(deftest topo-test
  (testing "predicate across branches"
    (let [sorted
          [{:branch-id (uuid :master)  :id (uuid :0) :prev-id nil        :pad 0 :op [:str/ins nil      "a"] :file-id (uuid :file)}
           {:branch-id (uuid :master)  :id (uuid :1) :prev-id (uuid :0)  :pad 0 :op [:str/ins (uuid :0) "a"] :file-id (uuid :file)}
           {:branch-id (uuid :child1)  :id (uuid :2) :prev-id (uuid :1)  :pad 0 :op [:str/ins (uuid :1) "a"] :file-id (uuid :file)}
           {:branch-id (uuid :child1)  :id (uuid :3) :prev-id (uuid :2)  :pad 0 :op [:str/ins (uuid :2) "a"] :file-id (uuid :file)}
           {:branch-id (uuid :child2)  :id (uuid :4) :prev-id (uuid :3)  :pad 0 :op [:str/ins (uuid :3) "a"] :file-id (uuid :file)}
           {:branch-id (uuid :child2)  :id (uuid :5) :prev-id (uuid :4)  :pad 0 :op [:str/ins (uuid :4) "a"] :file-id (uuid :file)}
           {:branch-id (uuid :child21) :id (uuid :6) :prev-id (uuid :5)  :pad 0 :op [:str/ins (uuid :5) "a"] :file-id (uuid :file)}
           {:branch-id (uuid :child21) :id (uuid :7) :prev-id (uuid :6)  :pad 0 :op [:str/ins (uuid :6) "a"] :file-id (uuid :file)}
           {:branch-id (uuid :child11) :id (uuid :8) :prev-id (uuid :7)  :pad 0 :op [:str/ins (uuid :7) "a"] :file-id (uuid :file)}
           {:branch-id (uuid :child11) :id (uuid :9) :prev-id (uuid :8)  :pad 0 :op [:str/ins (uuid :8) "a"] :file-id (uuid :file)}]
          nsorted (shuffle sorted)]
      (is (sut/sorted? sorted))
      (is (sut/valid-ops? sorted))
      (is (not (sut/sorted? nsorted)))
      (is (not (sut/valid-ops? nsorted)))))
  (testing "sort across branches"
    (let [deltas
          [{:branch-id (uuid :master)  :id (uuid :0) :prev-id nil       :pad 0 :op [:str/ins nil       "a"] :file-id (uuid :file)}
           {:branch-id (uuid :master)  :id (uuid :1) :prev-id (uuid :0) :pad 0 :op [:str/ins (uuid :0) "a"] :file-id (uuid :file)}
           {:branch-id (uuid :child1)  :id (uuid :2) :prev-id (uuid :1) :pad 0 :op [:str/ins nil       "a"] :file-id (uuid :file)}
           {:branch-id (uuid :child1)  :id (uuid :3) :prev-id (uuid :2) :pad 0 :op [:str/ins (uuid :2) "a"] :file-id (uuid :file)}
           {:branch-id (uuid :child2)  :id (uuid :4) :prev-id (uuid :3) :pad 0 :op [:str/ins (uuid :1) "a"] :file-id (uuid :file)}
           {:branch-id (uuid :child2)  :id (uuid :5) :prev-id (uuid :4) :pad 0 :op [:str/ins (uuid :4) "a"] :file-id (uuid :file)}
           {:branch-id (uuid :child21) :id (uuid :6) :prev-id (uuid :5) :pad 0 :op [:str/ins (uuid :5) "a"] :file-id (uuid :file)}
           {:branch-id (uuid :child21) :id (uuid :7) :prev-id (uuid :6) :pad 0 :op [:str/ins (uuid :6) "a"] :file-id (uuid :file)}
           {:branch-id (uuid :child11) :id (uuid :8) :prev-id (uuid :7) :pad 0 :op [:str/ins (uuid :7) "a"] :file-id (uuid :file)}
           {:branch-id (uuid :child11) :id (uuid :9) :prev-id (uuid :8) :pad 0 :op [:str/ins (uuid :8) "a"] :file-id (uuid :file)}]
          nsorted (shuffle deltas)
          sorted  (sut/sort (shuffle deltas))]
      (is (sut/sorted? sorted))
      (is (sut/valid-ops? sorted))
      (is (not (sut/sorted? nsorted)))
      (is (not (sut/valid-ops? nsorted))))))
