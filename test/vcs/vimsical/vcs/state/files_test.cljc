(ns vimsical.vcs.state.files-test
  #?@(:clj
      [(:require
        [clojure.test :as t :refer [deftest is testing]]
        [orchestra.spec.test :as st]
        [vimsical.common.test :refer [uuid]]
        [vimsical.vcs.alg.topo :as topo]
        [vimsical.vcs.data.gen.diff :as diff]
        [vimsical.vcs.editor :as editor]
        [vimsical.vcs.examples :as examples]
        [vimsical.vcs.state.files :as sut])]
      :cljs
      [(:require
        [clojure.spec.test :as st]
        [clojure.test :as t :refer-macros [deftest is testing]]
        [vimsical.common.test :refer [uuid]]
        [vimsical.vcs.alg.topo :as topo]
        [vimsical.vcs.data.gen.diff :as diff]
        [vimsical.vcs.editor :as editor]
        [vimsical.vcs.examples :as examples]
        [vimsical.vcs.state.files :as sut])]))

(st/instrument)

;;
;; * Deltas tests
;;

(deftest add-deltas-test
  (let [states                          (sut/add-deltas sut/empty-state-by-file-id examples/deltas)
        {::sut/keys [deltas] :as state} (get states examples/file1-id)]
    (t/is (seq deltas))))

;;
;; * Edit events tests
;;

(deftest add-edit-events-test
  (letfn [(test-pad-fn [_] 1)
          (test-uuid-fn [e] (uuid))
          (test-timestamp-fn [_] 123)]
    (let [branch-id    (uuid :<branch>)
          file-id      (uuid :<file>)
          test-effects {::editor/pad-fn       test-pad-fn
                        ::editor/uuid-fn      test-uuid-fn
                        ::editor/timestamp-fn test-timestamp-fn}
          state=       (fn [expected-string expected-cursor [states deltas]]
                         (let [{::sut/keys [files string cursor]} (get states file-id)]
                           (is (seq deltas))
                           (is (topo/sorted? deltas))
                           (is (= expected-string string))
                           (is (= expected-cursor cursor))))]
      (testing "Spliced insert"
        (state= "abc" 3
                (sut/add-edit-events
                 sut/empty-state-by-file-id test-effects file-id branch-id nil
                 (diff/diffs->edit-events "" ["abc"]))))
      (testing "Unspliced insert"
        (state= "abc" 3
                (sut/add-edit-events
                 sut/empty-state-by-file-id test-effects file-id branch-id nil
                 (diff/diffs->edit-events "" "abc"))))
      (testing "Spliced Delete"
        (state= "a" 1
                (sut/add-edit-events
                 sut/empty-state-by-file-id test-effects file-id branch-id nil
                 (diff/diffs->edit-events "" ["abc"] ["a"])))
        (state= "xyz" 3
                (sut/add-edit-events
                 sut/empty-state-by-file-id test-effects file-id branch-id nil
                 (diff/diffs->edit-events "" ["abc"] ["xyz"]))))
      (testing "Unspliced Delete"
        (state= "a" 1
                (sut/add-edit-events
                 sut/empty-state-by-file-id test-effects file-id branch-id nil
                 (diff/diffs->edit-events "" "abc" "a")))
        (state= "xyz" 3
                (sut/add-edit-events
                 sut/empty-state-by-file-id test-effects file-id branch-id nil
                 (diff/diffs->edit-events "" "abc" "xyz")))))))