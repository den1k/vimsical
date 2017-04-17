(ns vimsical.vcs.state.editor-test
  (:require
   [clojure.spec :as s]
   [clojure.test :refer [deftest is are testing]]
   [orchestra.spec.test :as st]
   [vimsical.vcs.branch :as branch]
   [vimsical.common.test :refer [uuid is=]]
   [vimsical.vcs.file :as file]
   [vimsical.vcs.delta :as delta]
   [vimsical.vcs.edit-event :as edit-event]
   [vimsical.vcs.state.files :as files]
   [vimsical.vcs.state.editor :as sut]
   [vimsical.vcs.data.gen.diff :as diff]))


(st/instrument)

(def branch-id (uuid :<branch>))
(def file-id (uuid :<file>))

(def test-state
  {::sut/branch-id    branch-id
   ::sut/file-id      file-id
   ::sut/delta-id     nil})

(defn test-pad-fn [_] 1)
(defn test-uuid-fn [{::edit-event/keys [op idx]}] (uuid [op idx]))
(defn test-timestamp-fn [_] 123)

(def test-effects
  {::sut/pad-fn       test-pad-fn
   ::sut/uuid-fn      test-uuid-fn
   ::sut/timestamp-fn test-timestamp-fn})

(defn get-state-for-latest-delta-id
  [{::sut/keys [files-states delta-id]} {::sut/keys [file-id]}]
  (get-in files-states [delta-id file-id]))

(def get-string-for-latest-delta-id (comp ::files/string get-state-for-latest-delta-id))

(deftest edit-events->deltas-test
  (is= "foo"
       (get-string-for-latest-delta-id
        (sut/edit-events->deltas
         files/empty-states
         test-state
         test-effects
         [#:vimsical.vcs.edit-event{:op :str/ins, :idx 0, :diff "foo"}])
        test-state))
  (is= "xyz"
       (get-string-for-latest-delta-id
        (sut/edit-events->deltas
         files/empty-states
         test-state
         test-effects
         (diff/diffs->edit-events "" "abc" "xyz"))
        test-state)))


(comment

  (sut/edit-events->deltas
   files/empty-states
   test-state
   test-effects
   [#:vimsical.vcs.edit-event{:op :str/ins, :idx 0, :diff "f"}
    #:vimsical.vcs.edit-event{:op :crsr/mv, :idx 1}])


  (get-string-for-latest-delta-id
   (sut/edit-events->deltas
    files/empty-states
    test-state
    test-effects
    [#:vimsical.vcs.edit-event{:op :str/ins, :idx 0, :diff "foo"}

     #:vimsical.vcs.edit-event{:op :crsr/mv, :idx 4}
     #:vimsical.vcs.edit-event{:op :crsr/mv, :idx 3}
     #:vimsical.vcs.edit-event{:op :crsr/mv, :idx 2}

     #:vimsical.vcs.edit-event{:op :crsr/mv, :idx 1}
     #:vimsical.vcs.edit-event{:op :crsr/mv, :idx 2}
     ;; #:vimsical.vcs.edit-event{:op :str/ins, :idx 2, :diff "u"}
     ;; #:vimsical.vcs.edit-event{:op :crsr/mv, :idx 3}
     ;; #:vimsical.vcs.edit-event{:op :str/ins, :idx 0, :diff ""}
     ])
   test-state)
  )
