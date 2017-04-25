(ns vimsical.vcs.core-test
  (:require
   [vimsical.vcs.core :as sut]
   [clojure.test :as t :refer [deftest is are testing]]
   [orchestra.spec.test :as st]
   [vimsical.common.test :refer [is= diff= uuid uuid-gen]]
   [vimsical.vcs.editor :as editor]
   [vimsical.vcs.examples :as examples]
   [vimsical.vcs.edit-event :as edit-event]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.file :as file]
   [vimsical.vcs.state.files :as state.files]
   [vimsical.vcs.state.timeline :as state.timeline]
   [vimsical.vcs.data.gen.diff :as diff]))

(st/unstrument)


;; * Integration tests

(deftest add-edit-event-test
  (letfn [(add-edit-events [vcs effects file-id edit-events]
            (reduce
             (fn [vcs edit-event]
               (sut/add-edit-event vcs effects file-id edit-event))
             vcs edit-events))]
    (let [{uuids :seq uuid-fn :f} (uuid-gen)
          branches                [examples/master]
          expect-html             "<body><h1>Hello</h1></body>"
          html-edit-events        (diff/diffs->edit-events
                                   ""
                                   ["<body></body>"]
                                   ["<body><h1>YO</h1></body>"]
                                   [expect-html])
          expect-css              "body { color: orange; }"
          css-edit-events         (diff/diffs->edit-events
                                   ""
                                   ["body { color: red; }"]
                                   [expect-css])
          all-edit-events         (into html-edit-events css-edit-events)
          effects                 {::editor/pad-fn       (constantly 1)
                                   ::editor/uuid-fn      (fn [e] (uuid-fn))
                                   ::editor/timestamp-fn (constantly 1)}
          vcs                     (-> (sut/empty-vcs branches)
                                      (add-edit-events effects (uuid :html) html-edit-events)
                                      (add-edit-events effects (uuid :css) css-edit-events))
          actual-html             (sut/file-string vcs (uuid :html))
          actual-css              (sut/file-string vcs (uuid :css))]
      (testing "files"
        (is (= expect-html actual-html))
        (is (= expect-css actual-css)))
      (testing "timeline"
        (is (= (sut/timeline-duration vcs) (count all-edit-events))))
      (testing "time-based state lookups"
        (let [last-html-delta (sut/delta-at-time vcs (count html-edit-events))
              last-css-delta  (sut/delta-at-time vcs (count all-edit-events))]
          (is (some? last-html-delta))
          (is (some? last-css-delta))
          (testing "files"
            ;; NOTE test with prev-id because the last id is a crsr mv
            (is= expect-html (sut/file-string vcs (uuid :html) (:prev-id last-html-delta)))
            (is= nil (sut/file-string vcs (uuid :css) (:prev-id last-html-delta)))
            (is= expect-html (sut/file-string vcs (uuid :html) (:prev-id last-css-delta)))
            (is= expect-css (sut/file-string vcs (uuid :css) (:prev-id last-css-delta)))))))))
