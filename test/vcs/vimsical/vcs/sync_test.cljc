(ns vimsical.vcs.sync-test
  #?@(:clj
      [(:require
        [clojure.test :as t :refer [deftest is testing]]
        [orchestra.spec.test :as st]
        [vimsical.common.test :refer [uuid uuid-gen]]
        [vimsical.vcs.branch :as branch]
        [vimsical.vcs.core :as vcs]
        [vimsical.vcs.sync :as sut])]
      :cljs
      [(:require
        [clojure.test :as t :refer-macros [deftest is testing]]
        [vimsical.common.test :refer [uuid uuid-gen]]
        [vimsical.vcs.branch :as branch]
        [vimsical.vcs.core :as vcs]
        [vimsical.vcs.sync :as sut])]))

#?(:clj (st/instrument))

;;
;; * Data
;;

(def deltas1
  [{:branch-uid (uuid :b0),,, :file-uid (uuid :f0), :uid (uuid :d0) :prev-uid nil,,,,,,,, :op [:str/ins nil "h"],,,,,,,, :pad 1, :meta {:timestamp 1, :version 1.0}}
   {:branch-uid (uuid :b0),,, :file-uid (uuid :f0), :uid (uuid :d1) :prev-uid (uuid :d0), :op [:str/ins (uuid :d0) "h"], :pad 1, :meta {:timestamp 1, :version 1.0}}
   {:branch-uid (uuid :b0),,, :file-uid (uuid :f0), :uid (uuid :d2) :prev-uid (uuid :d1), :op [:str/ins (uuid :d1) "h"], :pad 1, :meta {:timestamp 1, :version 1.0}}])
(def deltas2
  [{:branch-uid (uuid :b1-1), :file-uid (uuid :f0), :uid (uuid :d3) :prev-uid (uuid :d0), :op [:str/ins (uuid :d0) "h"], :pad 1, :meta {:timestamp 1, :version 1.0}}
   {:branch-uid (uuid :b1-1), :file-uid (uuid :f0), :uid (uuid :d4) :prev-uid (uuid :d3), :op [:str/ins (uuid :d3) "h"], :pad 1, :meta {:timestamp 1, :version 1.0}}
   {:branch-uid (uuid :b1-1), :file-uid (uuid :f0), :uid (uuid :d5) :prev-uid (uuid :d4), :op [:str/ins (uuid :d4) "h"], :pad 1, :meta {:timestamp 1, :version 1.0}}])
(def deltas3
  [{:branch-uid (uuid :b1-2), :file-uid (uuid :f0), :uid (uuid :d6) :prev-uid (uuid :d1), :op [:str/ins (uuid :d1) "h"], :pad 1, :meta {:timestamp 1, :version 1.0}}
   {:branch-uid (uuid :b1-2), :file-uid (uuid :f0), :uid (uuid :d7) :prev-uid (uuid :d6), :op [:str/ins (uuid :d6) "h"], :pad 1, :meta {:timestamp 1, :version 1.0}}
   {:branch-uid (uuid :b1-2), :file-uid (uuid :f0), :uid (uuid :d8) :prev-uid (uuid :d7), :op [:str/ins (uuid :d7) "h"], :pad 1, :meta {:timestamp 1, :version 1.0}}])

(def branches1 [{:db/uid (uuid :b0)}])
(def branches2 (conj branches1 {:db/uid (uuid :b1-1) ::branch/parent {:db/uid (uuid :b0) ::branch/branch-off-delta-uid (uuid :d0)}}))
(def branches3 (conj branches2 {:db/uid (uuid :b1-2) ::branch/parent {:db/uid (uuid :b0) ::branch/branch-off-delta-uid (uuid :d1)}}))

(def dbbu1 {(uuid :b0) (last deltas1)})
(def dbbu2 {(uuid :b0) (last deltas1) (uuid :b1-1) (last deltas2)})
(def dbbu3 {(uuid :b0) (last deltas1) (uuid :b1-1) (last deltas2) (uuid :b1-2) (last deltas3)})

(defn add-deltas [vcs uuid-fn deltas] (reduce #(vcs/add-delta %1 uuid-fn %2) vcs deltas))

(let [{uuid-fn :f} (uuid-gen)]
  (def vcs1         (add-deltas (vcs/empty-vcs branches3) uuid-fn deltas1))
  (def vcs2         (add-deltas vcs1 uuid-fn deltas2))
  (def vcs3         (add-deltas vcs2 uuid-fn deltas3)))

;;
;; * Tests
;;

(deftest sync-diff-test
  (letfn [(add-deltas [vcs uuid-fn deltas] (reduce #(vcs/add-delta %1 uuid-fn %2) vcs deltas))]
    (let [{uuid-fn :f} (uuid-gen)
          vcs1         (add-deltas (vcs/empty-vcs branches3) uuid-fn deltas1)
          vcs2         (add-deltas vcs1 uuid-fn deltas2)
          vcs3         (add-deltas vcs2 uuid-fn deltas3)]
      (testing "No remote state"
        (is (= {(uuid :b0) deltas1} (sut/diff vcs1 nil)))
        (is (= {(uuid :b0) deltas1 (uuid :b1-1) deltas2} (sut/diff vcs2 nil)))
        (is (= {(uuid :b0) deltas1 (uuid :b1-1) deltas2 (uuid :b1-2) deltas3} (sut/diff vcs3 nil))))
      (testing "Synced"
        (is (= nil (sut/diff vcs1 dbbu1)))
        (is (= nil (sut/diff vcs2 dbbu2)))
        (is (= nil (sut/diff vcs3 dbbu3))))
      (testing "Some remote state"
        (is (= {(uuid :b0) (drop 1 deltas1)} (sut/diff vcs1 {(uuid :b0) (nth deltas1 0)})))
        (is (= {(uuid :b0) (drop 2 deltas1)} (sut/diff vcs1 {(uuid :b0) (nth deltas1 1)})))
        (is (= nil,,,,,,,,,,,,,,,,,,,,,,,,,, (sut/diff vcs1 {(uuid :b0) (nth deltas1 2)})))
        (is (= {(uuid :b0)   (drop 1 deltas1)
                (uuid :b1-1) (drop 2 deltas2)} (sut/diff vcs2 {(uuid :b0)   (nth deltas1 0)
                                                               (uuid :b1-1) (nth deltas2 1)})))
        (is (= {(uuid :b0)   (drop 1 deltas1)
                (uuid :b1-1) deltas2
                (uuid :b1-2) (drop 2 deltas3)} (sut/diff vcs3 {(uuid :b0)   (nth deltas1 0)
                                                               (uuid :b1-2) (nth deltas3 1)})))))))

(deftest diff-deltas-test
  (is (= (concat (drop 1 deltas1)
                 deltas2
                 (drop 2 deltas3)))
      (sut/diff-deltas
       vcs3
       {(uuid :b0)   (nth deltas1 0)
        (uuid :b1-2) (nth deltas3 1)})))
