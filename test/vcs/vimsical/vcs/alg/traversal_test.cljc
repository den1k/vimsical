(ns vimsical.vcs.alg.traversal-test
  #?@(:clj
      [(:require
        [clojure.test :as t :refer [are deftest is testing]]
        [orchestra.spec.test :as st]
        [vimsical.common.test :refer [uuid]]
        [vimsical.vcs.alg.traversal :as sut]
        [vimsical.vcs.branch :as branch]
        [vimsical.vcs.examples :as examples]
        [vimsical.vcs.state.branches :as state.branches])]
      :cljs
      [(:require
        [cljs.test :as t :refer-macros [are deftest is testing]]
        [clojure.spec.test :as st]
        [vimsical.common.test :refer [uuid]]
        [vimsical.vcs.alg.traversal :as sut]
        [vimsical.vcs.branch :as branch]
        [vimsical.vcs.examples :as examples]
        [vimsical.vcs.state.branches :as state.branches])]))

(st/instrument)

(deftest walk-tree-test
  (let [pre    (atom -1)
        post   (atom -1)
        tree   {:children
                [{:children [{:children [{}]}]}
                 {:children [{}]}]}
        expect {:pre  0
                :post 5
                :children
                [{:pre 1 :post 2 :children [{:pre 2 :post 1 :children [{:pre 3 :post 0}]}]}
                 {:pre 4 :post 4 :children [{:pre 5 :post 3}]}]}
        actual (sut/walk-tree
                :children
                (fn node-fn [node children]
                  (assoc node :children children))
                (fn pre-fn [node]
                  (is (nil? (:post node)))
                  (assoc node :pre (swap! pre inc)))
                (fn post-fn [node]
                  (is (some? (:pre node)))
                  (assoc node :post (swap! post inc)))
                tree)]
    (is (= expect actual))))

(deftest reduce-tree-test
  (is (= [0 1 2 3 4 5]
         (sut/reduce-tree
          :children
          (fn f [acc {:keys [id]}]
            (conj acc id))
          [] {:id        5
              :children [{:id 2 :children [{:id 1 :children [{:id 0}]}]}
                         {:id 4 :children [{:id 3}]}]}))))

(defn ascending?  [comparison-result] (== sut/asc comparison-result))
(defn descending? [comparison-result] (== sut/desc comparison-result))

(deftest comparator-test
  (let [cpr (sut/new-branch-comparator examples/deltas-by-branch-id)]
    (are [pred left right] (is (pred (cpr left right))) ascending?  examples/master examples/child
         ascending?  examples/master examples/gchild
         ascending?  examples/child  examples/gchild
         descending? examples/child  examples/master
         descending? examples/gchild examples/master
         descending? examples/gchild examples/child)))


(deftest inlining-test
  (testing "Nested branches"
    (is (= (sut/inline examples/deltas-by-branch-id examples/branches))))

  (testing "Multiple children"
    (let [branches
          [{:db/id (uuid :b0)}
           {:db/id (uuid :b1-1) ::branch/parent {:db/id (uuid :b0)} ::branch/entry-delta-id (uuid :d0) }
           {:db/id (uuid :b1-2) ::branch/parent {:db/id (uuid :b0)} ::branch/entry-delta-id (uuid :d1)}]

          [d00 d01 d02
           d10 d11 d12
           d20 d21 d22 :as deltas]
          [{:branch-id (uuid :b0),   :file-id (uuid :f0), :id (uuid :d0) :prev-id nil,        :op [:str/ins nil        "h"], :pad 1, :meta {:timestamp 1, :version 1.0}}
           {:branch-id (uuid :b0),   :file-id (uuid :f0), :id (uuid :d1) :prev-id (uuid :d0), :op [:str/ins (uuid :d0) "h"], :pad 1, :meta {:timestamp 1, :version 1.0}}
           {:branch-id (uuid :b0),   :file-id (uuid :f0), :id (uuid :d2) :prev-id (uuid :d1), :op [:str/ins (uuid :d1) "h"], :pad 1, :meta {:timestamp 1, :version 1.0}}

           {:branch-id (uuid :b1-1), :file-id (uuid :f0), :id (uuid :d3) :prev-id (uuid :d2), :op [:str/ins (uuid :d0) "h"], :pad 1, :meta {:timestamp 1, :version 1.0}}
           {:branch-id (uuid :b1-1), :file-id (uuid :f0), :id (uuid :d4) :prev-id (uuid :d3), :op [:str/ins (uuid :d3) "h"], :pad 1, :meta {:timestamp 1, :version 1.0}}
           {:branch-id (uuid :b1-1), :file-id (uuid :f0), :id (uuid :d5) :prev-id (uuid :d4), :op [:str/ins (uuid :d4) "h"], :pad 1, :meta {:timestamp 1, :version 1.0}}

           {:branch-id (uuid :b1-2), :file-id (uuid :f0), :id (uuid :d6) :prev-id (uuid :d5), :op [:str/ins (uuid :d1) "h"], :pad 1, :meta {:timestamp 1, :version 1.0}}
           {:branch-id (uuid :b1-2), :file-id (uuid :f0), :id (uuid :d7) :prev-id (uuid :d6), :op [:str/ins (uuid :d6) "h"], :pad 1, :meta {:timestamp 1, :version 1.0}}
           {:branch-id (uuid :b1-2), :file-id (uuid :f0), :id (uuid :d8) :prev-id (uuid :d7), :op [:str/ins (uuid :d7) "h"], :pad 1, :meta {:timestamp 1, :version 1.0}}]

          deltas-by-branch-id (state.branches/add-deltas state.branches/empty-deltas-by-branch-id deltas)]
      (is (= [d00
              d10 d11 d12
              d01
              d20 d21 d22
              d02]
             (sut/inline deltas-by-branch-id branches))))))
