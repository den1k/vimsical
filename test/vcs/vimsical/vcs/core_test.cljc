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

(deftest add-edit-event-test
  (let [{uuid-fn :f}           (uuid-gen)
        branches               [examples/master]
        expect-html            "<body><h1>Hello</h1></body>"
        html-edit-events       (diff/diffs->edit-events
                                ""
                                ["<body></body>"]
                                ["<body><h1>YO</h1></body>"]
                                [expect-html])
        expect-css             "body { color: orange; }"
        css-edit-events        (diff/diffs->edit-events
                                ""
                                ["body { color: red; }"]
                                [expect-css])
        all-edit-events        (into html-edit-events css-edit-events)
        effects                {::editor/pad-fn       (constantly 1)
                                ::editor/uuid-fn      (fn [& _] (uuid-fn))
                                ::editor/timestamp-fn (constantly 1)}
        [vcs deltas delta-uid] (-> (sut/empty-vcs branches)
                                   (sut/add-edit-events effects (uuid :html) (uuid :master) nil html-edit-events))
        [vcs _ delta-uid]      (-> vcs
                                   (sut/add-edit-events effects (uuid :css) (uuid :master) (-> deltas last :uid) css-edit-events))
        html-deltas            (sut/file-deltas vcs (uuid :html) delta-uid)
        css-deltas             (sut/file-deltas vcs (uuid :css) delta-uid)
        all-deltas             (into html-deltas css-deltas)]
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
          (uuid :html) 0 1
          (uuid :html) 1 2
          (uuid :html) 2 3
          (uuid :html) 3 4)))
    (testing "timeline"
      (is (= (sut/timeline-duration vcs) (count all-edit-events)))
      (is (= 2 (count (sut/timeline-chunks-by-absolute-start-time vcs)))))
    (testing "time-based state lookups"
      (let [last-html-delta (sut/timeline-delta-at-time vcs (count html-edit-events))
            last-css-delta  (sut/timeline-delta-at-time vcs (count all-edit-events))]
        (is (some? last-html-delta))
        (is (some? last-css-delta))
        (testing "files"
          (is (= expect-html (sut/file-string vcs (uuid :html) (:uid last-html-delta))))
          (is (nil? (sut/file-string vcs (uuid :css) (:prev-uid last-html-delta))))
          (is (= expect-html (sut/file-string vcs (uuid :html) (:uid last-css-delta))))
          (is (= expect-css (sut/file-string vcs (uuid :css) (:uid last-css-delta)))))))))

(def ipsum
  "

  Lorem ipsum dolor sit amet, consectetur adipiscing elit. Quisque pharetra purus fermentum felis mollis ullamcorper. Nunc et ornare sem. Aenean mollis a odio at fringilla. Nam sit amet euismod felis. Praesent auctor a tortor nec sagittis. Aliquam commodo nulla sed lacus iaculis, quis vestibulum lorem elementum. Donec iaculis turpis sit amet fringilla tempus. Ut rhoncus luctus elit, in ultricies urna dignissim eget. Nam luctus maximus egestas. Fusce rhoncus mattis urna, quis ultrices risus semper aliquam. Nunc ultricies bibendum aliquet. Aenean enim nibh, suscipit vitae mattis ut, gravida eget risus. Integer posuere pellentesque ex, vel venenatis diam tempus eu. Aliquam blandit nisi enim, id imperdiet nibh commodo nec. Curabitur sit amet egestas massa. Nulla cursus condimentum convallis.
  ")

(deftest add-edit-event-and-add-deltas-equivalence-test
  (are [string cursor edit-events] (let [{uuid-fn :f} (uuid-gen)
                                         branches     [examples/master]
                                         effects      {::editor/pad-fn       (constantly 1)
                                                       ::editor/uuid-fn      (fn [& _] (uuid-fn))
                                                       ::editor/timestamp-fn (constantly 1)}
                                         [vcs deltas delta-uid] (-> (sut/empty-vcs branches)
                                                                    (sut/add-edit-events effects (uuid :html) (uuid :master) nil edit-events))
                                         vcs2                   (-> (sut/empty-vcs branches)
                                                                    (sut/add-deltas uuid-fn deltas))]
                                     (is (= string
                                            (sut/file-string vcs (uuid :html) delta-uid)
                                            (sut/file-string vcs2 (uuid :html) delta-uid)))
                                     (is (= cursor
                                            (sut/file-cursor vcs (uuid :html) delta-uid)
                                            (sut/file-cursor vcs2 (uuid :html) delta-uid))))
    "cb" 1
    [{:vimsical.vcs.edit-event/op :str/ins, :vimsical.vcs.edit-event/diff "a" :vimsical.vcs.edit-event/idx 0}
     {:vimsical.vcs.edit-event/op :str/rplc, :vimsical.vcs.edit-event/diff "b" :vimsical.vcs.edit-event/idx 0, :vimsical.vcs.edit-event/amt 1}
     {:vimsical.vcs.edit-event/op :str/ins, :vimsical.vcs.edit-event/diff "c" :vimsical.vcs.edit-event/idx 0}]

    (str ipsum ipsum) (count ipsum)
    [{:vimsical.vcs.edit-event/op :str/ins, :vimsical.vcs.edit-event/diff ipsum :vimsical.vcs.edit-event/idx 0}
     {:vimsical.vcs.edit-event/op :str/rplc, :vimsical.vcs.edit-event/diff ipsum :vimsical.vcs.edit-event/idx 0 , :vimsical.vcs.edit-event/amt (count ipsum)}
     {:vimsical.vcs.edit-event/op :str/ins, :vimsical.vcs.edit-event/diff ipsum :vimsical.vcs.edit-event/idx 0}]))

(deftest add-deltas-gen-test
  (let [{uuids   :seq
         uuid-fn :f}           (uuid-gen)
        branches               [examples/master]
        editor-effects         {::editor/pad-fn       (constantly 1)
                                ::editor/timestamp-fn (constantly 2)
                                ::editor/uuid-fn      uuid-fn}
        expect-html            "<body><h1>Hello</h1></body>"
        expect-css             "body { color: orange; }"
        [vcs deltas delta-uid] (-> (sut/empty-vcs branches)
                                   (diff/diffs->vcs
                                    editor-effects (uuid :html) (uuid :master) nil
                                    ""
                                    ["<body></body>"]
                                    ["<body><h1>YO</h1></body>"]
                                    [expect-html]))
        [vcs deltas delta-uid]      (-> vcs
                                        (diff/diffs->vcs
                                         editor-effects (uuid :css) (uuid :master) (-> deltas last :uid)
                                         ""
                                         ["body { color: red; }"]
                                         [expect-css]))
        actual-html            (sut/file-string vcs (uuid :html) (-> deltas last :uid))
        actual-css             (sut/file-string vcs (uuid :css) (-> deltas last :uid))]
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
         (delta/new-delta {:uid (uuid :d3) :prev-uid (uuid :d2) :branch-uid (uuid :master) :file-uid (uuid :html) :op [:str/rem (uuid :d1) 1],, :pad 1 :timestamp 1})
         (delta/new-delta {:uid (uuid :d4) :prev-uid (uuid :d3) :branch-uid (uuid :master) :file-uid (uuid :html) :op [:str/rem (uuid :d0) 1],, :pad 1 :timestamp 1})
         (delta/new-delta {:uid (uuid :d5) :prev-uid (uuid :d4) :branch-uid (uuid :master) :file-uid (uuid :html) :op [:str/ins (uuid :d0) "z"] :pad 1 :timestamp 1})]
        vcs                     (sut/add-deltas (sut/empty-vcs branches) uuid-fn html-deltas)
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
