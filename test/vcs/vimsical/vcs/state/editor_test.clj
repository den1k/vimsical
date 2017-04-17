(ns vimsical.vcs.state.editor-test
  (:require
   [clojure.spec :as s]
   [orchestra.spec.test :as st]
   [vimsical.vcs.branch :as branch]
   [vimsical.common.test :refer [uuid]]
   [vimsical.vcs.file :as file]
   [vimsical.vcs.delta :as delta]
   [vimsical.vcs.edit-event :as edit-event]
   [vimsical.vcs.state.files :as files]
   [vimsical.vcs.state.editor :as sut]))


(st/unstrument)
(st/instrument)

(def test-state
  {::sut/branch-id (uuid :branch)
   ::sut/file-id   (uuid :file)
   ::sut/delta-id  nil})

(defn test-pad-fn [_] 1)
(defn test-uuid-fn [{::edit-event/keys [op idx]}] (uuid [op idx]))
(defn test-timestamp-fn [_] 123)

(def test-effects
  {::sut/pad-fn       test-pad-fn
   ::sut/uuid-fn      test-uuid-fn
   ::sut/timestamp-fn test-timestamp-fn})


(sut/edit-events->deltas
 files/empty-states
 test-state
 test-effects
 [#:vimsical.vcs.edit-event{:op :str/ins, :idx 0, :diff "foor"}
  ;; #:vimsical.vcs.edit-event{:op :crsr/mv, :idx 4}
  ;; #:vimsical.vcs.edit-event{:op :crsr/mv, :idx 3}
  ;; #:vimsical.vcs.edit-event{:op :crsr/mv, :idx 2}
  ;; #:vimsical.vcs.edit-event{:op :str/rem, :idx 2, :amt 1}
  ;; #:vimsical.vcs.edit-event{:op :crsr/mv, :idx 1}
  ;; #:vimsical.vcs.edit-event{:op :crsr/mv, :idx 2}
  ;; #:vimsical.vcs.edit-event{:op :str/ins, :idx 2, :diff "u"}
  ;; #:vimsical.vcs.edit-event{:op :crsr/mv, :idx 3}
  ;; #:vimsical.vcs.edit-event{:op :str/ins, :idx 0, :diff ""}
  ])
