(ns vimsical.vcs.state.files-test
  (:require [clojure.test :as t :refer [are deftest is]]
            [orchestra.spec.test :as st]
            [vimsical.common.test :refer [uuid]]
            [vimsical.vcs.examples :as examples]
            [vimsical.vcs.state.files :as sut]))

(st/instrument)

(deftest add-deltas-test
  (t/is (sut/add-deltas sut/empty-states examples/deltas)))

(deftest get-deltas-tets
  (let [state (sut/add-deltas {} examples/deltas)]
    (t/is (= "hi"3  (::sut/string (sut/get-file-state state examples/id2 examples/file1-id))))
    (t/is (= "hi!" (::sut/string (sut/get-file-state state examples/id4 examples/file1-id))))))
