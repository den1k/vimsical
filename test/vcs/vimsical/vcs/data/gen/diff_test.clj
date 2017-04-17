(ns vimsical.vcs.data.gen.diff-test
  (:require
   [clojure.test :as t :refer [deftest testing is are]]
   [vimsical.common.test :refer [is= diff=]]
   [orchestra.spec.test :as st]
   [vimsical.vcs.data.gen.diff :as sut]
   [vimsical.vcs.edit-event :as edit-event]))

(st/instrument)


(deftest str->edit-events
  (diff=
   (sut/diffs->edit-events "" "foor" "four")
   [#:vimsical.vcs.edit-event{:op :str/ins, :idx 0, :diff "foor"}
    #:vimsical.vcs.edit-event{:op :crsr/mv, :idx 4}
    #:vimsical.vcs.edit-event{:op :crsr/mv, :idx 3}
    #:vimsical.vcs.edit-event{:op :crsr/mv, :idx 2}
    #:vimsical.vcs.edit-event{:op :str/rem, :idx 2, :amt 1}
    #:vimsical.vcs.edit-event{:op :crsr/mv, :idx 1}
    #:vimsical.vcs.edit-event{:op :crsr/mv, :idx 2}
    #:vimsical.vcs.edit-event{:op :str/ins, :idx 2, :diff "u"}
    #:vimsical.vcs.edit-event{:op :crsr/mv, :idx 3}]))
