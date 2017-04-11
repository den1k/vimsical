(ns vimsical.vcs.alg.topo-sort-test
  (:require
   [clojure.test :refer [deftest testing is are]]
   [vimsical.common.test :refer [uuid]]
   [vimsical.vcs.alg.topo-sort :as sut]
   [vimsical.vcs.branch :as branch]))

(deftest sort-test
  (testing "predicate across branches"
    (let [master         (uuid)
          child1         (uuid)
          child11        (uuid)
          child2         (uuid)
          child21        (uuid)
          id0            (uuid)
          id1            (uuid)
          id2            (uuid)
          id3            (uuid)
          id4            (uuid)
          id5            (uuid)
          master-branch  {::branch/name "master" :db/id master}
          child1-branch  {::branch/name "child1" :db/id child1 ::branch/parent master-branch}
          child11-branch {::branch/name "child11" :db/id child11 ::branch/parent child1-branch}
          child2-branch  {::branch/name "child2" :db/id child2 ::branch/parent master-branch}
          child21-branch {::branch/name "child21" :db/id child21 ::branch/parent child2-branch}
          branches       [master-branch child1-branch child11-branch child2-branch child21-branch]
          sorted
          [{:branch-id master  :id id0 :prev-id nil :pad 0 :op [:str/ins nil "a"] :file-id 1}
           {:branch-id master  :id id1 :prev-id id0 :pad 0 :op [:str/ins nil "a"] :file-id 1}
           {:branch-id child1  :id id2 :prev-id id1 :pad 0 :op [:str/ins nil "a"] :file-id 1}
           {:branch-id child1  :id id3 :prev-id id2 :pad 0 :op [:str/ins nil "a"] :file-id 1}
           {:branch-id child2  :id id2 :prev-id id1 :pad 0 :op [:str/ins nil "a"] :file-id 1}
           {:branch-id child2  :id id3 :prev-id id2 :pad 0 :op [:str/ins nil "a"] :file-id 1}
           {:branch-id child21 :id id4 :prev-id id3 :pad 0 :op [:str/ins nil "a"] :file-id 1}
           {:branch-id child21 :id id5 :prev-id id4 :pad 0 :op [:str/ins nil "a"] :file-id 1}
           {:branch-id child11 :id id4 :prev-id id3 :pad 0 :op [:str/ins nil "a"] :file-id 1}
           {:branch-id child11 :id id5 :prev-id id4 :pad 0 :op [:str/ins nil "a"] :file-id 1}]
          nsorted
          [{:branch-id master  :id id0 :prev-id nil :pad 0 :op [:str/ins nil "a"] :file-id 1}
           {:branch-id master  :id id1 :prev-id id0 :pad 0 :op [:str/ins nil "a"] :file-id 1}
           {:branch-id child1  :id id2 :prev-id id1 :pad 0 :op [:str/ins nil "a"] :file-id 1}
           {:branch-id child1  :id id3 :prev-id id2 :pad 0 :op [:str/ins nil "a"] :file-id 1}
           {:branch-id child21 :id id4 :prev-id id3 :pad 0 :op [:str/ins nil "a"] :file-id 1}
           {:branch-id child21 :id id5 :prev-id id4 :pad 0 :op [:str/ins nil "a"] :file-id 1}
           {:branch-id child2  :id id2 :prev-id id1 :pad 0 :op [:str/ins nil "a"] :file-id 1}
           {:branch-id child2  :id id3 :prev-id id2 :pad 0 :op [:str/ins nil "a"] :file-id 1}]]
      (is (sut/topo-sorted? branches sorted))
      (is (not (sut/topo-sorted? branches nsorted)))))
  (testing "sort across branches"
    (let [master         (uuid)
          child1         (uuid)
          child11        (uuid)
          child2         (uuid)
          child21        (uuid)
          id0            (uuid)
          id1            (uuid)
          id2            (uuid)
          id3            (uuid)
          id4            (uuid)
          id5            (uuid)
          master-branch  {::branch/name "master" :db/id master}
          child1-branch  {::branch/name "child1" :db/id child1 ::branch/parent master-branch}
          child11-branch {::branch/name "child11" :db/id child11 ::branch/parent child1-branch}
          child2-branch  {::branch/name "child2" :db/id child2 ::branch/parent master-branch}
          child21-branch {::branch/name "child21" :db/id child21 ::branch/parent child2-branch}
          branches       [master-branch child1-branch child11-branch child2-branch child21-branch]
          deltas
          [{:branch-id master  :id id0 :prev-id nil :pad 0 :op [:str/ins nil "a"] :file-id 1}
           {:branch-id master  :id id1 :prev-id id0 :pad 0 :op [:str/ins nil "a"] :file-id 1}
           {:branch-id child1  :id id2 :prev-id id1 :pad 0 :op [:str/ins nil "a"] :file-id 1}
           {:branch-id child1  :id id3 :prev-id id2 :pad 0 :op [:str/ins nil "a"] :file-id 1}
           {:branch-id child2  :id id2 :prev-id id1 :pad 0 :op [:str/ins nil "a"] :file-id 1}
           {:branch-id child2  :id id3 :prev-id id2 :pad 0 :op [:str/ins nil "a"] :file-id 1}
           {:branch-id child21 :id id4 :prev-id id3 :pad 0 :op [:str/ins nil "a"] :file-id 1}
           {:branch-id child21 :id id5 :prev-id id4 :pad 0 :op [:str/ins nil "a"] :file-id 1}
           {:branch-id child11 :id id4 :prev-id id3 :pad 0 :op [:str/ins nil "a"] :file-id 1}
           {:branch-id child11 :id id5 :prev-id id4 :pad 0 :op [:str/ins nil "a"] :file-id 1}]
          sorted-deltas  (sut/topo-sort branches (shuffle deltas))]
      (is (= (set deltas) (set sorted-deltas)))
      (is (sut/topo-sorted? branches sorted-deltas)))))
