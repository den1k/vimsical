(ns vimsical.vcs.delta-test
  #?@(:clj
      [(:require
        [clojure.test :as t :refer [deftest is]]
        [orchestra.spec.test :as st]
        [vimsical.common.test :refer [uuid]]
        [vimsical.vcs.delta :as sut])]
      :cljs
      [(:require
        [clojure.spec.test.alpha :as st]
        [clojure.test :as t :refer-macros [deftest is]]
        [vimsical.common.test :refer [uuid]]
        [vimsical.vcs.delta :as sut])]))

(st/instrument)


(deftest spec-test
  (is (sut/new-delta
       {:branch-uid (uuid :branch-uid)
        :file-uid   (uuid :file-uid)
        :prev-uid   (uuid :delta-uid)
        :uid        (uuid :delta-uid')
        :op         [:str/ins (uuid :op-uid) "a"]
        :pad        1
        :timestamp  1})))

(deftest accessors-test
  (let [delta (sut/new-delta
               {:branch-uid (uuid :branch-uid)
                :file-uid   (uuid :file-uid)
                :prev-uid   (uuid :delta-uid)
                :uid        (uuid :delta-uid')
                :op         [:str/ins (uuid :op-uid) "a"]
                :pad        1
                :timestamp  1})]
    (is (= (uuid :op-uid) (sut/op-uid delta)))
    (is (= :str/ins (sut/op-type delta)))
    (is (= "a" (sut/op-diff delta)))))
