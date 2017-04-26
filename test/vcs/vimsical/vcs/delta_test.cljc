(ns vimsical.vcs.delta-test
  #?@(:clj
      [(:require
        [clojure.test :as t :refer [deftest is]]
        [orchestra.spec.test :as st]
        [vimsical.common.test :refer [uuid]]
        [vimsical.vcs.delta :as sut])]
      :cljs
      [(:require
        [clojure.spec.test :as st]
        [clojure.test :as t :refer-macros [deftest is]]
        [vimsical.common.test :refer [uuid]]
        [vimsical.vcs.delta :as sut])]))

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
