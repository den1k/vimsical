(ns vimsical.vcs.state.files-test
  (:require [clojure.test :as t :refer [are deftest is testing]]
            [orchestra.spec.test :as st]
            [vimsical.common.test :refer [uuid]]
            [vimsical.vcs.editor :as editor]
            [vimsical.vcs.examples :as examples]
            [vimsical.vcs.alg.topo :as topo]
            [vimsical.vcs.state.files :as sut]
            [vimsical.vcs.data.gen.diff :as diff]
            [vimsical.vcs.edit-event :as edit-event]))

(st/instrument)


;; * Deltas tests

(deftest add-deltas-test
  (let [states                          (sut/add-deltas sut/empty-state-by-file-id examples/deltas)
        {::sut/keys [deltas] :as state} (get states examples/file1-id)]
    (t/is (seq deltas))))


;; * Edit events tests

(deftest add-edit-events-test
  (letfn [(test-pad-fn [_] 1)
          (test-uuid-fn [e] (uuid))
          (test-timestamp-fn [_] 123)]
    (let [branch-id    (uuid :<branch>)
          file-id      (uuid :<file>)
          test-effects {::editor/pad-fn       test-pad-fn
                        ::editor/uuid-fn      test-uuid-fn
                        ::editor/timestamp-fn test-timestamp-fn}
          string= (fn [expected [states deltas]]
                    (let [{::sut/keys [files string]} (get states file-id)]
                      (is (seq deltas))
                      (is (topo/sorted? deltas))
                      (is (= expected string))))]
      (testing "Spliced insert"
        (string= "abc"
                 (sut/add-edit-events
                  sut/empty-state-by-file-id test-effects file-id branch-id nil
                  (diff/diffs->edit-events "" ["abc"]))))
      (testing "Unspliced insert"
        (string= "abc"
                 (sut/add-edit-events
                  sut/empty-state-by-file-id test-effects file-id branch-id nil
                  (diff/diffs->edit-events "" "abc"))))
      (testing "Spliced Delete"
        (string= "a"
                 (sut/add-edit-events
                  sut/empty-state-by-file-id test-effects file-id branch-id nil
                  (diff/diffs->edit-events "" ["abc"] ["a"])))
        (string= "xyz"
                 (sut/add-edit-events
                  sut/empty-state-by-file-id test-effects file-id branch-id nil
                  (diff/diffs->edit-events "" ["abc"] ["xyz"]))))
      (testing "Unspliced Delete"
        (string= "a"
                 (sut/add-edit-events
                  sut/empty-state-by-file-id test-effects file-id branch-id nil
                  (diff/diffs->edit-events "" "abc" "a")))
        (string= "xyz"
                 (sut/add-edit-events
                  sut/empty-state-by-file-id test-effects file-id branch-id nil
                  (diff/diffs->edit-events "" "abc" "xyz")))))))
