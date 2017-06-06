(ns vimsical.vcs.data.gen.diff-test
  #?@(:clj
      [(:require
        [clojure.test :as t :refer [deftest is]]
        [orchestra.spec.test :as st]
        [vimsical.common.test :refer [diff= uuid uuid-gen uuid-fixture]]
        [vimsical.vcs.core :as vcs]
        [vimsical.vcs.data.gen.diff :as sut]
        [vimsical.vcs.delta :as delta]
        [vimsical.vcs.edit-event :as edit-event]
        [vimsical.vcs.editor :as editor])]
      :cljs
      [(:require
        [clojure.spec.test.alpha :as st]
        [clojure.test :as t :refer-macros [deftest is]]
        [vimsical.common.test :refer [uuid uuid-gen uuid-fixture]]
        [vimsical.vcs.core :as vcs]
        [vimsical.vcs.data.gen.diff :as sut]
        [vimsical.vcs.delta :as delta]
        [vimsical.vcs.edit-event :as edit-event]
        [vimsical.vcs.editor :as editor])]))


(st/instrument)

(t/use-fixtures :each uuid-fixture)

(deftest diffs->edit-events
  (is (= (sut/diffs->edit-events "" "foor" "four")
         [{::edit-event/op :str/ins, ::edit-event/idx 0, ::edit-event/diff "foor"}
          {::edit-event/op :crsr/mv, ::edit-event/idx 4}
          {::edit-event/op :crsr/mv, ::edit-event/idx 3}
          {::edit-event/op :crsr/mv, ::edit-event/idx 2}
          {::edit-event/op :str/rem, ::edit-event/idx 2, ::edit-event/amt 1}
          {::edit-event/op :crsr/mv, ::edit-event/idx 1}
          {::edit-event/op :crsr/mv, ::edit-event/idx 2}
          {::edit-event/op :str/ins, ::edit-event/idx 2, ::edit-event/diff "u"}
          {::edit-event/op :crsr/mv, ::edit-event/idx 3}])))

(deftest diffs->edit-deltas
  ;; NOTE can't really track the uuids used for deltas since we update the whole
  ;; vcs, creating chunks etc...
  (let [{uuid-fn :f}      (uuid-gen)
        effects           {::editor/pad-fn       (constantly 1)
                           ::editor/timestamp-fn (constantly 2)
                           ::editor/uuid-fn      uuid-fn}
        branches          [{:db/uid (uuid :master)}]
        vcs               (vcs/empty-vcs branches)
        [vcs _ delta-uid] (sut/diffs->vcs vcs effects (uuid :file) (uuid :master) nil "" ["abc"])
        deltas            (vcs/deltas vcs delta-uid)]
    ;; Assertion is trivial but rely on the deltas spec to check for the topo
    ;; sort as wel as valid delta ops
    (is (= 6 (count deltas)))))
