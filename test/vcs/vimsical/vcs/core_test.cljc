(ns vimsical.vcs.core-test
  #?@(:clj
      [(:require
        [clojure.test.check :as tc]     ; fixes cider test report
        [clojure.test :as t :refer [deftest is are testing]]
        [orchestra.spec.test :as st]
        [vimsical.common.test :refer [uuid-fixture uuid uuid-gen]]
        [vimsical.vcs.core :as sut]
        [vimsical.vcs.data.gen.diff :as diff]
        [vimsical.vcs.delta :as delta]
        [vimsical.vcs.editor :as editor]
        [vimsical.vcs.examples :as examples])]
      :cljs
      [(:require
        [clojure.spec.test :as st]
        [clojure.test :as t :refer-macros [deftest is are testing]]
        [vimsical.common.test :refer [uuid-fixture uuid uuid-gen]]
        [vimsical.vcs.core :as sut]
        [vimsical.vcs.data.gen.diff :as diff]
        [vimsical.vcs.delta :as delta]
        [vimsical.vcs.editor :as editor]
        [vimsical.vcs.examples :as examples])]))

(st/instrument)


(t/use-fixtures :each uuid-fixture)

;; * Integration tests

(deftest add-edit-event-test
  (letfn [(add-edit-events [vcs effects file-id edit-events]
            (reduce
             (fn [vcs edit-event]
               (sut/add-edit-event vcs effects file-id edit-event))
             vcs edit-events))]
    (let [{uuid-fn :f}     (uuid-gen)
          branches         [examples/master]
          expect-html      "<body><h1>Hello</h1></body>"
          html-edit-events (diff/diffs->edit-events
                            ""
                            ["<body></body>"]
                            ["<body><h1>YO</h1></body>"]
                            [expect-html])
          expect-css       "body { color: orange; }"
          css-edit-events  (diff/diffs->edit-events
                            ""
                            ["body { color: red; }"]
                            [expect-css])
          all-edit-events  (into html-edit-events css-edit-events)
          effects          {::editor/pad-fn       (constantly 1)
                            ::editor/uuid-fn      (fn [& _] (uuid-fn))
                            ::editor/timestamp-fn (constantly 1)}
          vcs              (-> (sut/empty-vcs branches)
                               (add-edit-events effects (uuid :html) html-edit-events)
                               (add-edit-events effects (uuid :css) css-edit-events))
          html-deltas      (sut/file-deltas vcs (uuid :html))
          css-deltas       (sut/file-deltas vcs (uuid :css))
          all-deltas       (into html-deltas css-deltas)]
      (testing "file"
        (testing "string"
          (is (= expect-html (sut/file-string vcs (uuid :html))))
          (is (= expect-css (sut/file-string vcs (uuid :css))))
          (are [file-id delta-index string] (is (= string (sut/file-string vcs file-id (->> delta-index (nth all-deltas) :id))))
            (uuid :html) 0 "<"
            (uuid :html) 1 "<b"
            (uuid :html) 2 "<bo"
            (uuid :html) 3 "<bod"))
        (testing "cursor"
          (are [file-id delta-index cursor] (is (= cursor (sut/file-cursor vcs file-id (->> delta-index (nth all-deltas) :id))))
            (uuid :html) 0 1
            (uuid :html) 1 2
            (uuid :html) 2 3
            (uuid :html) 3 4)))
      (testing "timeline"
        (is (= (sut/timeline-duration vcs) (count all-edit-events))))
      (testing "time-based state lookups"
        (let [last-html-delta (sut/timeline-delta-at-time vcs (count html-edit-events))
              last-css-delta  (sut/timeline-delta-at-time vcs (count all-edit-events))]
          (is (some? last-html-delta))
          (is (some? last-css-delta))
          (testing "files"
            ;; NOTE test with prev-id because the last id is a crsr mv
            (is (= expect-html (sut/file-string vcs (uuid :html) (:prev-id last-html-delta))))
            (is (nil? (sut/file-string vcs (uuid :css) (:prev-id last-html-delta))))
            (is (= expect-html (sut/file-string vcs (uuid :html) (:prev-id last-css-delta))))
            (is (= expect-css (sut/file-string vcs (uuid :css) (:prev-id last-css-delta))))))))))

(deftest add-deltas-gen-test
  (let [{uuids :seq uuid-fn :f} (uuid-gen)
        branches                [examples/master]
        editor-effects          {::editor/pad-fn       (constantly 1)
                                 ::editor/timestamp-fn (constantly 2)
                                 ::editor/uuid-fn      uuid-fn}
        expect-html             "<body><h1>Hello</h1></body>"
        expect-css              "body { color: orange; }"
        vcs                     (-> (sut/empty-vcs branches)
                                    (diff/diffs->deltas
                                     editor-effects (uuid :html)
                                     ""
                                     ["<body></body>"]
                                     ["<body><h1>YO</h1></body>"]
                                     [expect-html])
                                    (diff/diffs->deltas
                                     editor-effects (uuid :css)
                                     ""
                                     ["body { color: red; }"]
                                     [expect-css]))
        actual-html             (sut/file-string vcs (uuid :html))
        actual-css              (sut/file-string vcs (uuid :css))]
    (testing "files"
      (is (= expect-html actual-html))
      (is (= expect-css actual-css)))))

(deftest add-deltas-small-test
  (let [{uuids :seq uuid-fn :f} (uuid-gen)
        branches                [examples/master]
        expect-html             "az"
        html-deltas             [(delta/new-delta {:id (uuid :d0) :prev-id nil        :branch-id (uuid :master) :file-id (uuid :html) :op [:str/ins nil        "a"] :pad 1 :timestamp 1})
                                 (delta/new-delta {:id (uuid :d1) :prev-id (uuid :d0) :branch-id (uuid :master) :file-id (uuid :html) :op [:str/ins (uuid :d0) "b"] :pad 1 :timestamp 1})
                                 (delta/new-delta {:id (uuid :d2) :prev-id (uuid :d1) :branch-id (uuid :master) :file-id (uuid :html) :op [:str/ins (uuid :d1) "c"] :pad 1 :timestamp 1})
                                 (delta/new-delta {:id (uuid :d3) :prev-id (uuid :d2) :branch-id (uuid :master) :file-id (uuid :html) :op [:str/rem (uuid :d2) 1]   :pad 1 :timestamp 1})
                                 (delta/new-delta {:id (uuid :d4) :prev-id (uuid :d3) :branch-id (uuid :master) :file-id (uuid :html) :op [:str/rem (uuid :d1) 1]   :pad 1 :timestamp 1})
                                 (delta/new-delta {:id (uuid :d5) :prev-id (uuid :d4) :branch-id (uuid :master) :file-id (uuid :html) :op [:str/ins (uuid :d0) "z"] :pad 1 :timestamp 1})]
        vcs                     (reduce #(sut/add-delta %1 uuid-fn %2) (sut/empty-vcs branches) html-deltas)
        actual-html             (sut/file-string vcs (uuid :html))]
    (testing "files"
      (is (= expect-html actual-html)))
    (testing "timeline"
      (is (= (sut/timeline-duration vcs) (count html-deltas))))
    (testing "time-based state lookups"
      (let [last-html-delta (sut/timeline-delta-at-time vcs (count html-deltas))]
        (is (some? last-html-delta))
        (testing "files"
          (is (= expect-html (sut/file-string vcs (uuid :html)))))))))
