(ns vimsical.vcs.delta-test
  (:require
   [orchestra.spec.test :as st]
   [clojure.test :as t :refer [deftest is are]]
   [vimsical.common.test :refer [uuid]]
   [vimsical.vcs.examples :as examples]
   [vimsical.vcs.delta :as sut]))

(st/instrument)


(deftest spec-test
  (is (sut/new-delta
       {:branch-id (uuid :branch-id)
        :file-id   (uuid :file-id)
        :prev-id   (uuid :delta-id)
        :id        (uuid :delta-id')
        :op        [:str/ins (uuid :op-id) "a"]
        :pad       1
        :timestamp 1})))
