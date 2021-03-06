(ns vimsical.vcs.state.files-test
  #?@(:clj
      [(:require
        [clojure.test :as t :refer [deftest is testing]]
        [orchestra.spec.test :as st]
        [vimsical.common.test :refer [uuid uuid-gen]]
        [vimsical.vcs.alg.topo :as topo]
        [vimsical.vcs.data.gen.diff :as diff]
        [vimsical.vcs.editor :as editor]
        [vimsical.vcs.state.files :as sut]
        [vimsical.vcs.data.indexed.vector :as indexed]
        [vimsical.vcs.edit-event :as edit-event])]
      :cljs
      [(:require
        [clojure.spec.test.alpha :as st]
        [clojure.test :as t :refer-macros [deftest is testing]]
        [vimsical.common.test :refer [uuid uuid-gen]]
        [vimsical.vcs.alg.topo :as topo]
        [vimsical.vcs.data.gen.diff :as diff]
        [vimsical.vcs.editor :as editor]
        [vimsical.vcs.state.files :as sut]
        [vimsical.vcs.edit-event :as edit-event])]))

(st/instrument)


;;
;; * Edit events tests
;;

(defn diff=
  [a b]
  #?(:clj (let [diff (take 2 (clojure.data/diff a b))]
            (is (every? nil? diff)))
     :cljs (is (= a b))))

(deftest add-edit-events-manual-test
  (letfn [(do-test [{:keys [f seq reset] :as gen} expect-string expect-cursor expect-deltas events]
            (testing expect-string
              (reset)
              (letfn [(test-pad-fn [_] 1)
                      (test-uuid-fn [e] (f))
                      (test-timestamp-fn [_] 123)]
                (let [branch-uid                         (uuid :<branch>)
                      file-uid                           (uuid :<file>)
                      test-effects                       {::editor/pad-fn       test-pad-fn
                                                          ::editor/uuid-fn      test-uuid-fn
                                                          ::editor/timestamp-fn test-timestamp-fn}
                      [states deltas]                    (sut/add-edit-events
                                                          sut/empty-state-by-file-uid test-effects file-uid branch-uid nil
                                                          events)
                      states'                            (sut/add-deltas sut/empty-state-by-file-uid deltas)
                      delta-uid                          (-> deltas last :uid)
                      {::sut/keys [files string cursor]} (get states file-uid)]
                  (is (topo/sorted? deltas))
                  (diff= states states')
                  (diff= expect-deltas deltas)
                  (is (= expect-string string))
                  (is (= expect-cursor cursor))))))]
    (let [{:keys [f seq] :as gen}                                 (uuid-gen uuid "delta")
          [d0 d1 d2 d3 d4 d5 d6 d7 d8 d9 d10 d11 d12 d13 d14 d15] seq]

      (do-test
       gen "ab" 2
       [{:uid d0 :prev-uid nil :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:str/ins nil "a"] :meta {:timestamp 123 :version 0.3}}
        {:uid d1 :prev-uid d0 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:crsr/mv d0] :meta {:timestamp 123 :version 0.3}}
        {:uid d2 :prev-uid d1 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:str/ins d0 "b"] :meta {:timestamp 123 :version 0.3}}
        {:uid d3 :prev-uid d2 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:crsr/mv d2] :meta {:timestamp 123 :version 0.3}}]
       [{::edit-event/op :str/ins, ::edit-event/diff "a", ::edit-event/idx 0}
        {::edit-event/op :crsr/mv, ::edit-event/idx 1}
        {::edit-event/op :str/ins, ::edit-event/diff "b", ::edit-event/idx 1}
        {::edit-event/op :crsr/mv, ::edit-event/idx 2}])

      (do-test
       gen "zab" 1
       [{:uid d0 :prev-uid nil :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:str/ins nil "a"] :meta {:timestamp 123 :version 0.3}}
        {:uid d1 :prev-uid d0 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:crsr/mv d0] :meta {:timestamp 123 :version 0.3}}
        {:uid d2 :prev-uid d1 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:str/ins d0 "b"] :meta {:timestamp 123 :version 0.3}}
        {:uid d3 :prev-uid d2 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:crsr/mv d2] :meta {:timestamp 123 :version 0.3}}
        {:uid d4 :prev-uid d3 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:crsr/mv nil] :meta {:timestamp 123 :version 0.3}}
        {:uid d5 :prev-uid d4 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:str/ins nil "z"] :meta {:timestamp 123 :version 0.3}}
        {:uid d6 :prev-uid d5 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:crsr/mv d5] :meta {:timestamp 123 :version 0.3}}]
       [{::edit-event/op :str/ins, ::edit-event/diff "a", ::edit-event/idx 0}
        {::edit-event/op :crsr/mv, ::edit-event/idx 1}
        {::edit-event/op :str/ins, ::edit-event/diff "b", ::edit-event/idx 1}
        {::edit-event/op :crsr/mv, ::edit-event/idx 2}
        {::edit-event/op :crsr/mv, ::edit-event/idx 0}
        {::edit-event/op :str/ins, ::edit-event/diff "z", ::edit-event/idx 0}
        {::edit-event/op :crsr/mv, ::edit-event/idx 1}])

      (do-test
       gen "abc" 3
       [{:uid d0 :prev-uid nil :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:str/ins nil "a"] :meta {:timestamp 123 :version 0.3}}
        {:uid d1 :prev-uid d0 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:crsr/mv d0] :meta {:timestamp 123 :version 0.3}}
        {:uid d2 :prev-uid d1 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:str/ins d0 "b"] :meta {:timestamp 123 :version 0.3}}
        {:uid d3 :prev-uid d2 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:crsr/mv d2] :meta {:timestamp 123 :version 0.3}}
        {:uid d4 :prev-uid d3 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:str/ins d2 "c"] :meta {:timestamp 123 :version 0.3}}
        {:uid d5 :prev-uid d4 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:crsr/mv d4] :meta {:timestamp 123 :version 0.3}}]
       [{::edit-event/op :str/ins, ::edit-event/diff "a", ::edit-event/idx 0}
        {::edit-event/op :crsr/mv, ::edit-event/idx 1}
        {::edit-event/op :str/ins, ::edit-event/diff "b", ::edit-event/idx 1}
        {::edit-event/op :crsr/mv, ::edit-event/idx 2}
        {::edit-event/op :str/ins, ::edit-event/diff "c", ::edit-event/idx 2}
        {::edit-event/op :crsr/mv, ::edit-event/idx 3}])

      (do-test
       gen "ab" 2
       [{:uid d0 :prev-uid nil :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:str/ins nil "a"] :meta {:timestamp 123 :version 0.3}}
        {:uid d1 :prev-uid d0 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:crsr/mv d0] :meta {:timestamp 123 :version 0.3}}
        {:uid d2 :prev-uid d1 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:str/ins d0 "b"] :meta {:timestamp 123 :version 0.3}}
        {:uid d3 :prev-uid d2 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:crsr/mv d2] :meta {:timestamp 123 :version 0.3}}
        {:uid d4 :prev-uid d3 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:str/ins d2 "c"] :meta {:timestamp 123 :version 0.3}}
        {:uid d5 :prev-uid d4 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:crsr/mv d4] :meta {:timestamp 123 :version 0.3}}
        {:uid d6 :prev-uid d5 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:str/rem d2 1] :meta {:timestamp 123 :version 0.3}}
        {:uid d7 :prev-uid d6 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:crsr/mv d2] :meta {:timestamp 123 :version 0.3}}]
       [{::edit-event/op :str/ins, ::edit-event/diff "a", ::edit-event/idx 0}
        {::edit-event/op :crsr/mv, ::edit-event/idx 1}
        {::edit-event/op :str/ins, ::edit-event/diff "b", ::edit-event/idx 1}
        {::edit-event/op :crsr/mv, ::edit-event/idx 2}
        {::edit-event/op :str/ins, ::edit-event/diff "c", ::edit-event/idx 2}
        {::edit-event/op :crsr/mv, ::edit-event/idx 3}
        {::edit-event/op :str/rem, ::edit-event/idx 2, ::edit-event/amt 1}
        {::edit-event/op :crsr/mv, ::edit-event/idx 2}])

      (do-test
       gen "abd" 3
       [{:uid d0 :prev-uid nil :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:str/ins nil "a"] :meta {:timestamp 123 :version 0.3}}
        {:uid d1 :prev-uid d0 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:crsr/mv d0] :meta {:timestamp 123 :version 0.3}}
        {:uid d2 :prev-uid d1 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:str/ins d0 "b"] :meta {:timestamp 123 :version 0.3}}
        {:uid d3 :prev-uid d2 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:crsr/mv d2] :meta {:timestamp 123 :version 0.3}}
        {:uid d4 :prev-uid d3 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:str/ins d2 "c"] :meta {:timestamp 123 :version 0.3}}
        {:uid d5 :prev-uid d4 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:crsr/mv d4] :meta {:timestamp 123 :version 0.3}}
        {:uid d6 :prev-uid d5 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:str/rem d2 1] :meta {:timestamp 123 :version 0.3}}
        {:uid d7 :prev-uid d6 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:crsr/mv d2] :meta {:timestamp 123 :version 0.3}}
        {:uid d8 :prev-uid d7 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:str/ins d2 "d"] :meta {:timestamp 123 :version 0.3}}
        {:uid d9 :prev-uid d8 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:crsr/mv d8] :meta {:timestamp 123 :version 0.3}}]
       [{::edit-event/op :str/ins, ::edit-event/diff "a", ::edit-event/idx 0}
        {::edit-event/op :crsr/mv, ::edit-event/idx 1}
        {::edit-event/op :str/ins, ::edit-event/diff "b", ::edit-event/idx 1}
        {::edit-event/op :crsr/mv, ::edit-event/idx 2}
        {::edit-event/op :str/ins, ::edit-event/diff "c", ::edit-event/idx 2}
        {::edit-event/op :crsr/mv, ::edit-event/idx 3}
        {::edit-event/op :str/rem, ::edit-event/idx 2, ::edit-event/amt 1}
        {::edit-event/op :crsr/mv, ::edit-event/idx 2}
        {::edit-event/op :str/ins, ::edit-event/diff "d", ::edit-event/idx 2}
        {::edit-event/op :crsr/mv, ::edit-event/idx 3}])

      (do-test
       gen "az" 2
       [{:uid d0 :prev-uid nil :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:str/ins nil "a"] :meta {:timestamp 123 :version 0.3}}
        {:uid d1 :prev-uid d0 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:crsr/mv d0] :meta {:timestamp 123 :version 0.3}}
        {:uid d2 :prev-uid d1 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:str/ins d0 "b"] :meta {:timestamp 123 :version 0.3}}
        {:uid d3 :prev-uid d2 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:crsr/mv d2] :meta {:timestamp 123 :version 0.3}}
        {:uid d4 :prev-uid d3 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:str/ins d2 "c"] :meta {:timestamp 123 :version 0.3}}
        {:uid d5 :prev-uid d4 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:crsr/mv d4] :meta {:timestamp 123 :version 0.3}}
        {:uid d6 :prev-uid d5 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:str/rem d2 1] :meta {:timestamp 123 :version 0.3}}
        {:uid d7 :prev-uid d6 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:crsr/mv d2] :meta {:timestamp 123 :version 0.3}}
        {:uid d8 :prev-uid d7 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:str/ins d2 "d"] :meta {:timestamp 123 :version 0.3}}
        {:uid d9 :prev-uid d8 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:crsr/mv d8] :meta {:timestamp 123 :version 0.3}}
        {:uid d10 :prev-uid d9 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:crsr/sel [d8 d2]] :meta {:timestamp 123 :version 0.3}}
        {:uid d11 :prev-uid d10 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:crsr/sel [d8 d0]] :meta {:timestamp 123 :version 0.3}}
        {:uid d12 :prev-uid d11 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:str/rem  d0 2] :meta {:timestamp 123 :version 0.3}}
        {:uid d13 :prev-uid d12 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:str/ins d0 "z"] :meta {:timestamp 123 :version 0.3}}
        {:uid d14 :prev-uid d13 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:crsr/mv d13] :meta {:timestamp 123 :version 0.3}}]
       [{::edit-event/op :str/ins, ::edit-event/diff "a", ::edit-event/idx 0}
        {::edit-event/op :crsr/mv, ::edit-event/idx 1}
        {::edit-event/op :str/ins, ::edit-event/diff "b", ::edit-event/idx 1}
        {::edit-event/op :crsr/mv, ::edit-event/idx 2}
        {::edit-event/op :str/ins, ::edit-event/diff "c", ::edit-event/idx 2}
        {::edit-event/op :crsr/mv, ::edit-event/idx 3}
        {::edit-event/op :str/rem, ::edit-event/idx 2, ::edit-event/amt 1}
        {::edit-event/op :crsr/mv, ::edit-event/idx 2}
        {::edit-event/op :str/ins, ::edit-event/diff "d", ::edit-event/idx 2}
        {::edit-event/op :crsr/mv, ::edit-event/idx 3}
        {::edit-event/op :crsr/sel, ::edit-event/range [3 2]}
        {::edit-event/op :crsr/sel, ::edit-event/range [3 1]}
        {::edit-event/op :str/rplc, ::edit-event/diff "z", ::edit-event/idx 1, ::edit-event/amt 2}
        {::edit-event/op :crsr/mv, ::edit-event/idx 2}])

      (do-test
       gen "xyz" 3
       [{:uid d0 :prev-uid nil :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:str/ins nil "a"] :meta {:timestamp 123 :version 0.3}}
        {:uid d1 :prev-uid d0 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:crsr/mv d0] :meta {:timestamp 123 :version 0.3}}
        {:uid d2 :prev-uid d1 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:str/ins d0 "b"] :meta {:timestamp 123 :version 0.3}}
        {:uid d3 :prev-uid d2 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:crsr/mv d2] :meta {:timestamp 123 :version 0.3}}
        {:uid d4 :prev-uid d3 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:str/ins d2 "c"] :meta {:timestamp 123 :version 0.3}}
        {:uid d5 :prev-uid d4 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:crsr/mv d4] :meta {:timestamp 123 :version 0.3}}
        {:uid d6 :prev-uid d5 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:str/rem d2 1] :meta {:timestamp 123 :version 0.3}}
        {:uid d7 :prev-uid d6 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:crsr/mv d2] :meta {:timestamp 123 :version 0.3}}
        {:uid d8 :prev-uid d7 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:str/ins d2 "d"] :meta {:timestamp 123 :version 0.3}}
        {:uid d9 :prev-uid d8 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:crsr/mv d8] :meta {:timestamp 123 :version 0.3}}
        {:uid d10 :prev-uid d9 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:crsr/sel [d8 d2]] :meta {:timestamp 123 :version 0.3}}
        {:uid d11 :prev-uid d10 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:crsr/sel [d8 nil]] :meta {:timestamp 123 :version 0.3}}
        {:uid d12 :prev-uid d11 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:str/rem  nil 3] :meta {:timestamp 123 :version 0.3}}
        {:uid d13 :prev-uid d12 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:str/ins nil "x"] :meta {:timestamp 123 :version 0.3}}
        {:uid d14 :prev-uid d13 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:str/ins d13 "y"] :meta {:timestamp 123 :version 0.3}}
        {:uid d15 :prev-uid d14 :branch-uid (uuid :<branch>) :file-uid (uuid :<file>) :pad 1 :op [:str/ins d14 "z"] :meta {:timestamp 123 :version 0.3}}]
       [{::edit-event/op :str/ins, ::edit-event/diff "a", ::edit-event/idx 0}
        {::edit-event/op :crsr/mv, ::edit-event/idx 1}
        {::edit-event/op :str/ins, ::edit-event/diff "b", ::edit-event/idx 1}
        {::edit-event/op :crsr/mv, ::edit-event/idx 2}
        {::edit-event/op :str/ins, ::edit-event/diff "c", ::edit-event/idx 2}
        {::edit-event/op :crsr/mv, ::edit-event/idx 3}
        {::edit-event/op :str/rem, ::edit-event/idx 2, ::edit-event/amt 1}
        {::edit-event/op :crsr/mv, ::edit-event/idx 2}
        {::edit-event/op :str/ins, ::edit-event/diff "d", ::edit-event/idx 2}
        {::edit-event/op :crsr/mv, ::edit-event/idx 3}
        {::edit-event/op :crsr/sel, ::edit-event/range [3 2]}
        {::edit-event/op :crsr/sel, ::edit-event/range [3 0]}
        {::edit-event/op :str/rplc, ::edit-event/diff "xyz", ::edit-event/idx 0, ::edit-event/amt 3}]))))

(deftest add-edit-events-test
  (letfn [(test-pad-fn [_] 1)
          (test-uuid-fn [e] (uuid))
          (test-timestamp-fn [_] 123)]
    (let [branch-uid   (uuid :<branch>)
          file-uid     (uuid :<file>)
          test-effects {::editor/pad-fn       test-pad-fn
                        ::editor/uuid-fn      test-uuid-fn
                        ::editor/timestamp-fn test-timestamp-fn}
          state=       (fn [expect-string expect-cursor [states deltas]]
                         (let [delta-uid                          (-> deltas last :uid)
                               {::sut/keys [files string cursor]} (get states file-uid)]
                           (is (seq deltas))
                           (is (topo/sorted? deltas))
                           (is (= expect-string string))
                           (is (= expect-cursor cursor))))]
      (testing "Spliced insert"
        (state= "abc" 3
                (sut/add-edit-events
                 sut/empty-state-by-file-uid test-effects file-uid branch-uid nil
                 (diff/diffs->edit-events "" ["abc"]))))
      (testing "Unspliced insert"
        (state= "abc" 3
                (sut/add-edit-events
                 sut/empty-state-by-file-uid test-effects file-uid branch-uid nil
                 (diff/diffs->edit-events "" "abc"))))
      (testing "Spliced Delete"
        (state= "a" 1
                (sut/add-edit-events
                 sut/empty-state-by-file-uid test-effects file-uid branch-uid nil
                 (diff/diffs->edit-events "" ["abc"] ["a"])))
        (state= "xyz" 3
                (sut/add-edit-events
                 sut/empty-state-by-file-uid test-effects file-uid branch-uid nil
                 (diff/diffs->edit-events "" ["abc"] ["xyz"]))))
      (testing "Unspliced Delete"
        (state= "a" 1
                (sut/add-edit-events
                 sut/empty-state-by-file-uid test-effects file-uid branch-uid nil
                 (diff/diffs->edit-events "" "abc" "a")))
        (state= "xyz" 3
                (sut/add-edit-events
                 sut/empty-state-by-file-uid test-effects file-uid branch-uid nil
                 (diff/diffs->edit-events "" "abc" "xyz")))
        (state= "axyz" 4
                (sut/add-edit-events
                 sut/empty-state-by-file-uid test-effects file-uid branch-uid nil
                 (diff/diffs->edit-events "" "abc" "axyz")))
        (state= "xyz" 3
                (sut/add-edit-events
                 sut/empty-state-by-file-uid test-effects file-uid branch-uid nil
                 [{:vimsical.vcs.edit-event/op :str/ins,   :vimsical.vcs.edit-event/idx 0, :vimsical.vcs.edit-event/diff "abc"}
                  {:vimsical.vcs.edit-event/op :str/rplc , :vimsical.vcs.edit-event/idx 0, :vimsical.vcs.edit-event/amt 3 :vimsical.vcs.edit-event/diff "xyz"}])))
      (testing "Regressions"
        (let [ipsum "

Lorem ipsum dolor sit amet, consectetur adipiscing elit. Quisque pharetra purus fermentum felis mollis ullamcorper. Nunc et ornare sem. Aenean mollis a odio at fringilla. Nam sit amet euismod felis. Praesent auctor a tortor nec sagittis. Aliquam commodo nulla sed lacus iaculis, quis vestibulum lorem elementum. Donec iaculis turpis sit amet fringilla tempus. Ut rhoncus luctus elit, in ultricies urna dignissim eget. Nam luctus maximus egestas. Fusce rhoncus mattis urna, quis ultrices risus semper aliquam. Nunc ultricies bibendum aliquet. Aenean enim nibh, suscipit vitae mattis ut, gravida eget risus. Integer posuere pellentesque ex, vel venenatis diam tempus eu. Aliquam blandit nisi enim, id imperdiet nibh commodo nec. Curabitur sit amet egestas massa. Nulla cursus condimentum convallis."]
          (state= ipsum (count ipsum)
                  (sut/add-edit-events
                   sut/empty-state-by-file-uid test-effects file-uid branch-uid nil
                   [{:vimsical.vcs.edit-event/op :str/ins, :vimsical.vcs.edit-event/diff ipsum :vimsical.vcs.edit-event/idx 0}
                    {:vimsical.vcs.edit-event/op :crsr/sel, :vimsical.vcs.edit-event/range [0 (count ipsum)]}
                    {:vimsical.vcs.edit-event/op :str/rplc, :vimsical.vcs.edit-event/diff  ipsum :vimsical.vcs.edit-event/idx 0, :vimsical.vcs.edit-event/amt (count ipsum)}]))
          (state= (str ipsum ipsum) (count ipsum)
                  (sut/add-edit-events
                   sut/empty-state-by-file-uid test-effects file-uid branch-uid nil
                   [{:vimsical.vcs.edit-event/op :str/ins, :vimsical.vcs.edit-event/diff ipsum :vimsical.vcs.edit-event/idx 0}
                    {:vimsical.vcs.edit-event/op :str/rplc, :vimsical.vcs.edit-event/diff ipsum :vimsical.vcs.edit-event/idx 0 , :vimsical.vcs.edit-event/amt (count ipsum)}
                    {:vimsical.vcs.edit-event/op :str/ins, :vimsical.vcs.edit-event/diff ipsum :vimsical.vcs.edit-event/idx 0}])))))))
