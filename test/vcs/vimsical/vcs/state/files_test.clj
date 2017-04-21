
(ns vimsical.vcs.state.files-test
  (:require [clojure.test :as t :refer [are deftest is testing]]
            [orchestra.spec.test :as st]
            [vimsical.common.test :refer [uuid is=]]
            [vimsical.vcs.examples :as examples]
            [vimsical.vcs.state.files :as sut]
            [vimsical.vcs.data.gen.diff :as diff]
            [vimsical.vcs.edit-event :as edit-event]))

(st/unstrument)

;; * Deltas tests

(deftest add-deltas-test
  (t/is (sut/add-deltas sut/empty-states examples/deltas)))


;; * Edit events tests

(deftest add-edit-events-test
  (letfn [(test-pad-fn [_] 1)
          (test-uuid-fn [e] (uuid))
          (test-timestamp-fn [_] 123)]
    (let [branch-id    (uuid :<branch>)
          file-id      (uuid :<file>)
          test-state   {::sut/branch-id branch-id
                        ::sut/file-id   file-id
                        ::sut/delta-id  nil}
          test-effects {::sut/pad-fn       test-pad-fn
                        ::sut/uuid-fn      test-uuid-fn
                        ::sut/timestamp-fn test-timestamp-fn}
          string= (fn [expected [states current-delta-id]]
                    (is= expected (sut/get-file-string states current-delta-id file-id)))]
      (testing "Spliced insert"
        (string= "abc"
                 (sut/add-edit-events
                  sut/empty-states test-state test-effects
                  (diff/diffs->edit-events "" ["abc"]))))
      (testing "Unspliced insert"
        (string= "abc"
                 (sut/add-edit-events
                  sut/empty-states test-state test-effects
                  (diff/diffs->edit-events "" "abc"))))
      (testing "Spliced Delete"
        (string= "a"
                 (sut/add-edit-events
                  sut/empty-states test-state test-effects
                  (diff/diffs->edit-events "" ["abc"] ["a"])))
        (string= "xyz"
                 (sut/add-edit-events
                  sut/empty-states test-state test-effects
                  (diff/diffs->edit-events "" ["abc"] ["xyz"]))))
      (testing "Unspliced Delete"
        (string= "a"
                 (sut/add-edit-events
                  sut/empty-states test-state test-effects
                  (diff/diffs->edit-events "" "abc" "a")))
        (string= "xyz"
                 (sut/add-edit-events
                  sut/empty-states test-state test-effects
                  (diff/diffs->edit-events "" "abc" "xyz")))))))


;; * Accessors tests

(deftest get-deltas-tets
  (let [state (sut/add-deltas {} examples/deltas)]
    (t/are [s id] (t/is (= s (::sut/string (sut/get-file-state state id examples/file1-id))))
      "h"   examples/id0
      "h"   examples/id1
      "hi"  examples/id2
      "hi"  examples/id3
      "hi!" examples/id4
      "hi!" examples/id5
      "hi"  examples/id6
      "h"   examples/id7
      "hey" examples/id8)))
