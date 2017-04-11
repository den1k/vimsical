(ns vimsical.vcs.core-test
  (:require
   [clojure.spec :as s]
   [clojure.test :refer [deftest testing is are]]
   [vimsical.common.test :refer [is= diff=]]
   [vimsical.common.uuid :refer [uuid]]
   [vimsical.vcs.core :as sut]
   [vimsical.vcs.indexed :as indexed]))

(def master-id (uuid))
(def child-id (uuid))
(def gchild-id (uuid))
(def file1-id (uuid))
(def file2-id (uuid))
(def id0 (uuid))
(def id1 (uuid))
(def id2 (uuid))
(def id3 (uuid))
(def id4 (uuid))
(def id5 (uuid))
(def id6 (uuid))
(def id7 (uuid))

(def d0 {:branch-id master-id, :file-id file1-id, :id id0 :prev-id nil, :op [:str/ins nil "h"], :pad 0,   :meta {:timestamp 1, :version 1.0}})
(def d1 {:branch-id master-id, :file-id file1-id, :id id1 :prev-id id0, :op [:crsr/mv 1],       :pad 1,   :meta {:timestamp 1, :version 1.0}})
(def d2 {:branch-id child-id,  :file-id file1-id, :id id2 :prev-id id1, :op [:str/ins 0   "i"], :pad 100, :meta {:timestamp 1, :version 1.0}})
(def d3 {:branch-id child-id,  :file-id file1-id, :id id3 :prev-id id2, :op [:crsr/mv 1],       :pad 1,   :meta {:timestamp 1, :version 1.0}})
(def d4 {:branch-id gchild-id, :file-id file1-id, :id id4 :prev-id id3, :op [:str/ins 1   "!"], :pad 100, :meta {:timestamp 1, :version 1.0}})
(def d5 {:branch-id gchild-id, :file-id file1-id, :id id5 :prev-id id4, :op [:crsr/mv 1],       :pad 1,   :meta {:timestamp 1, :version 1.0}})
(def d6 {:branch-id child-id,  :file-id file2-id, :id id6 :prev-id id5, :op [:crsr/mv 1],       :pad 1,   :meta {:timestamp 1, :version 1.0}})
(def d7 {:branch-id gchild-id, :file-id file2-id, :id id7 :prev-id id6, :op [:crsr/mv 1],       :pad 1,   :meta {:timestamp 1, :version 1.0}})

(def deltas [d0 d1 d2 d3 d4 d5 d6 d7])


;; * Deltas

(deftest deltas-spec-test
  (testing "deltas spec"
    (is (s/valid? ::sut/deltas deltas))))

(deftest deltas-test
  (testing "adding deltas to the vector"
    (let [[l r] (split-at 3 deltas)]
      (diff= deltas (sut/add-deltas ::sut/deltas l r)))))

(deftest indexed-deltas
  (testing "vector equiv"
    (is= deltas (seq (sut/indexed-deltas deltas))))
  (testing "adding deltas to an indexed deltas vector"
    (is= deltas (seq (sut/add-deltas ::sut/deltas (sut/indexed-deltas []) deltas)))))


;; * Indexed deltas by branch uuid

(deftest indexed-deltas-by-branch-id
  (let [expected       {master-id (sut/indexed-deltas [d0 d1])
                        child-id  (sut/indexed-deltas [d2 d3 d6])
                        gchild-id (sut/indexed-deltas [d4 d5 d7])}
        indexed-deltas (sut/indexed-deltas deltas)
        batches        (split-at 3 indexed-deltas)
        actual         (reduce
                        (fn [acc batch]
                          (sut/add-deltas ::sut/indexed-deltas-by-branch-id acc batch))
                        {} batches)]
    (testing "groups with indexed vectors"
      (is (every? indexed/indexed-vector? (vals actual))))
    (testing "deltas are added to the branch index"
      (is= expected actual))
    (testing "can find the index of a delta in a branch"
      (are [index branch-uuid delta] (is= index (-> actual (get branch-uuid) (indexed/key-of delta)))
        0 master-id d0
        0 child-id  d2
        0 child-id  d2
        2 gchild-id d7))))


;; * Topology

(testing "branches are ordered according to their entry delta ids")



;; * Topology index

(testing "can find the ")


;; * Files state

(testing "Given a delta and a traversal, find the previous deltas of other files")


;; * Timeline

;; NON GOAL :o)
;; (testing "Find deltas at time")

(testing "Add deltas")
