(ns vimsical.vcs.state.vims.files.latest-test
  (:require
   [clojure.spec :as s]
   [vimsical.vcs.examples :as examples]
   [clojure.test :as t :refer [deftest is are]]
   [vimsical.vcs.state.vims.files.latest :as sut]))

(deftest add-deltas-test
  (is (= examples/latest-file-deltas (sut/add-deltas {} examples/deltas))))
