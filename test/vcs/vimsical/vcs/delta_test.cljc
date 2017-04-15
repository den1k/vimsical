(ns vimsical.vcs.delta-test
  (:require
   [clojure.test :as t :refer [deftest is are]]
   [vimsical.vcs.examples :as examples]
   [vimsical.vcs.delta :as sut]))

(deftest add-deltas-test
  (sut/add-deltas {} examples/deltas))
