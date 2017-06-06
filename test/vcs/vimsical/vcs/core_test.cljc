(ns vimsical.vcs.core-test
  #?@(:clj
      [(:require
        [clojure.test.check :as tc]
        [clojure.test :as t :refer [are deftest is testing]]
        [orchestra.spec.test :as st]
        [vimsical.common.test :refer [uuid uuid-fixture uuid-gen]]
        [vimsical.vcs.core :as sut]
        [vimsical.vcs.data.gen.diff :as diff]
        [vimsical.vcs.delta :as delta]
        [vimsical.vcs.editor :as editor]
        [vimsical.vcs.examples :as examples])]
      :cljs
      [(:require
        [clojure.spec.test.alpha :as st]
        [clojure.test :as t :refer-macros [are deftest is testing]]
        [vimsical.common.test :refer [uuid uuid-fixture uuid-gen]]
        [vimsical.vcs.core :as sut]
        [vimsical.vcs.data.gen.diff :as diff]
        [vimsical.vcs.delta :as delta]
        [vimsical.vcs.editor :as editor]
        [vimsical.vcs.examples :as examples])]))

(st/instrument)

(t/use-fixtures :each uuid-fixture)

;;
;; * Integration tests
;;

(defn- add-edit-events
  [vcs effects file-uid branch-uid delta-uid edit-events]
  (reduce
   (fn [[vcs _ delta-uid] edit-event]
     (sut/add-edit-event vcs effects file-uid branch-uid delta-uid edit-event))
   [vcs nil delta-uid] edit-events))

(deftest add-edit-event-test
  (let [{uuid-fn :f}      (uuid-gen)
        branches          [examples/master]
        expect-html       "<body><h1>Hello</h1></body>"
        html-edit-events  (diff/diffs->edit-events
                           ""
                           ["<body></body>"]
                           ["<body><h1>YO</h1></body>"]
                           [expect-html])
        expect-css        "body { color: orange; }"
        css-edit-events   (diff/diffs->edit-events
                           ""
                           ["body { color: red; }"]
                           [expect-css])
        all-edit-events   (into html-edit-events css-edit-events)
        effects           {::editor/pad-fn       (constantly 1)
                           ::editor/uuid-fn      (fn [& _] (uuid-fn))
                           ::editor/timestamp-fn (constantly 1)}
        [vcs _ delta-uid] (-> (sut/empty-vcs branches)
                              (add-edit-events effects (uuid :html) (uuid :master) nil html-edit-events))
        [vcs _ delta-uid] (-> vcs
                              (add-edit-events effects (uuid :css) (uuid :master) delta-uid css-edit-events))
        html-deltas       (sut/file-deltas vcs (uuid :html) delta-uid)
        css-deltas        (sut/file-deltas vcs (uuid :css) delta-uid)
        all-deltas        (into html-deltas css-deltas)]
    (testing "file"
      (testing "string"
        (is (= expect-html (sut/file-string vcs (uuid :html) delta-uid)))
        (is (= expect-css (sut/file-string vcs (uuid :css) delta-uid)))
        (are [file-uid delta-index string] (is (= string (sut/file-string vcs file-uid (->> delta-index (nth all-deltas) :uid))))
          (uuid :html) 0 "<"
          (uuid :html) 1 "<b"
          (uuid :html) 2 "<bo"
          (uuid :html) 3 "<bod"))
      (testing "cursor"
        (are [file-uid delta-index cursor] (is (= cursor (sut/file-cursor vcs file-uid (->> delta-index (nth all-deltas) :uid))))
          (uuid :html) 0 0
          (uuid :html) 1 1
          (uuid :html) 2 2
          (uuid :html) 3 3)))
    (testing "timeline"
      (is (= (sut/timeline-duration vcs) (count all-edit-events)))
      (is (= 2 (count (sut/timeline-chunks-by-absolute-start-time vcs)))))
    (testing "time-based state lookups"
      (let [last-html-delta (sut/timeline-delta-at-time vcs (count html-edit-events))
            last-css-delta  (sut/timeline-delta-at-time vcs (count all-edit-events))]
        (is (some? last-html-delta))
        (is (some? last-css-delta))
        (testing "files"
          ;; NOTE test with prev-uid because the last id is a crsr mv
          (is (= expect-html (sut/file-string vcs (uuid :html) (:prev-uid last-html-delta))))
          (is (nil? (sut/file-string vcs (uuid :css) (:prev-uid last-html-delta))))
          (is (= expect-html (sut/file-string vcs (uuid :html) (:prev-uid last-css-delta))))
          (is (= expect-css (sut/file-string vcs (uuid :css) (:prev-uid last-css-delta)))))))))

(deftest add-deltas-gen-test
  (let [{uuids   :seq
         uuid-fn :f}      (uuid-gen)
        branches          [examples/master]
        editor-effects    {::editor/pad-fn       (constantly 1)
                           ::editor/timestamp-fn (constantly 2)
                           ::editor/uuid-fn      uuid-fn}
        expect-html       "<body><h1>Hello</h1></body>"
        expect-css        "body { color: orange; }"
        [vcs _ delta-uid] (-> (sut/empty-vcs branches)
                              (diff/diffs->vcs
                               editor-effects (uuid :html) (uuid :master) nil
                               ""
                               ["<body></body>"]
                               ["<body><h1>YO</h1></body>"]
                               [expect-html]))
        [vcs _ delta-uid] (-> vcs
                              (diff/diffs->vcs
                               editor-effects (uuid :css) (uuid :master) delta-uid
                               ""
                               ["body { color: red; }"]
                               [expect-css]))
        actual-html       (sut/file-string vcs (uuid :html) delta-uid)
        actual-css        (sut/file-string vcs (uuid :css) delta-uid)]
    (testing "files"
      (is (= expect-html actual-html))
      (is (= expect-css actual-css)))))

(deftest add-deltas-small-test
  (let [{uuids :seq uuid-fn :f} (uuid-gen)
        branches                [examples/master]
        expect-html             "az"
        html-deltas
        [(delta/new-delta {:uid (uuid :d0) :prev-uid nil,,,,,,, :branch-uid (uuid :master) :file-uid (uuid :html) :op [:str/ins nil        "a"] :pad 1 :timestamp 1})
         (delta/new-delta {:uid (uuid :d1) :prev-uid (uuid :d0) :branch-uid (uuid :master) :file-uid (uuid :html) :op [:str/ins (uuid :d0) "b"] :pad 1 :timestamp 1})
         (delta/new-delta {:uid (uuid :d2) :prev-uid (uuid :d1) :branch-uid (uuid :master) :file-uid (uuid :html) :op [:str/ins (uuid :d1) "c"] :pad 1 :timestamp 1})
         (delta/new-delta {:uid (uuid :d3) :prev-uid (uuid :d2) :branch-uid (uuid :master) :file-uid (uuid :html) :op [:str/rem (uuid :d2) 1],, :pad 1 :timestamp 1})
         (delta/new-delta {:uid (uuid :d4) :prev-uid (uuid :d3) :branch-uid (uuid :master) :file-uid (uuid :html) :op [:str/rem (uuid :d1) 1],, :pad 1 :timestamp 1})
         (delta/new-delta {:uid (uuid :d5) :prev-uid (uuid :d4) :branch-uid (uuid :master) :file-uid (uuid :html) :op [:str/ins (uuid :d0) "z"] :pad 1 :timestamp 1})]
        vcs                     (reduce #(sut/add-delta %1 uuid-fn %2) (sut/empty-vcs branches) html-deltas)
        actual-html             (sut/file-string vcs (uuid :html) (-> html-deltas last :uid))]
    (testing "files"
      (is (= expect-html actual-html)))
    (testing "timeline"
      (is (= (sut/timeline-duration vcs) (count html-deltas))))
    (testing "time-based state lookups"
      (let [last-html-delta (sut/timeline-delta-at-time vcs (count html-deltas))]
        (is (some? last-html-delta))
        (testing "files"
          (is (= expect-html actual-html)))))))
