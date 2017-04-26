(ns vimsical.vcs.data.gen.diff-test
  #?@(:clj
      [(:require
        [clojure.test :as t :refer [deftest is]]
        [orchestra.spec.test :as st]
        [vimsical.vcs.data.gen.diff :as sut]
        [vimsical.vcs.edit-event :as edit-event])]
      :cljs
      [(:require
        [clojure.spec.test :as st]
        [clojure.test :as t :refer-macros [deftest is]]
        [vimsical.vcs.data.gen.diff :as sut]
        [vimsical.vcs.edit-event :as edit-event])]))


(st/instrument)

(deftest str->edit-events
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
