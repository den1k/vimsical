(ns vimsical.vcs.core-test
  (:require
   [clojure.spec :as s]
   [orchestra.spec.test :as st]
   [clojure.test :as t :refer [deftest testing is are]]
   [vimsical.common.test :refer [is= diff=]]
   [vimsical.common.uuid :refer [uuid]]
   [vimsical.vcs.core :as sut]
   [vimsical.vcs.data.indexed.vector :as indexed.vector]))

;; * Deltas
;; * Topology
(testing "branches are ordered according to their entry delta ids")
;; * Topology index
(testing "can find the ")
;; * Files state
(testing "Given a delta and a traversal, find the previous deltas of other files")
;; * Timeline
;; NON GOAL :o)
;; (testing "Find deltas at time")
