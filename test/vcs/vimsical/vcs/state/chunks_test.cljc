(ns vimsical.vcs.state.chunks-test
  #?@(:clj
      [(:require
        [clojure.test :as t :refer [deftest is]]
        [vimsical.common.test :refer [uuid uuid-gen]]
        [vimsical.vcs.branch :as branch]
        [vimsical.vcs.state.chunk :as chunk]
        [vimsical.vcs.state.chunks :as sut])]
      :cljs
      [(:require
        [clojure.test :as t :refer-macros [deftest is]]
        [vimsical.common.test :refer [uuid uuid-gen]]
        [vimsical.vcs.branch :as branch]
        [vimsical.vcs.state.chunk :as chunk]
        [vimsical.vcs.state.chunks :as sut])]))

(def deltas
  [{:branch-id (uuid :b0), :file-id (uuid :f0), :id (uuid :d0) :prev-id nil,,,,,,,, :op [:str/ins nil "h"],,,,,,,, :pad 1, :meta {:timestamp 1, :version 1.0}}
   {:branch-id (uuid :b0), :file-id (uuid :f0), :id (uuid :d1) :prev-id (uuid :d0), :op [:str/ins (uuid :d0) "h"], :pad 1, :meta {:timestamp 1, :version 1.0}}
   {:branch-id (uuid :b0), :file-id (uuid :f1), :id (uuid :d2) :prev-id (uuid :d1), :op [:str/ins (uuid :d1) "h"], :pad 1, :meta {:timestamp 1, :version 1.0}}

   {:branch-id (uuid :b1-1), :file-id (uuid :f0), :id (uuid :d3) :prev-id (uuid :d2), :op [:str/ins (uuid :d0) "h"], :pad 1, :meta {:timestamp 1, :version 1.0}}
   {:branch-id (uuid :b1-1), :file-id (uuid :f0), :id (uuid :d4) :prev-id (uuid :d3), :op [:str/ins (uuid :d3) "h"], :pad 1, :meta {:timestamp 1, :version 1.0}}
   {:branch-id (uuid :b1-1), :file-id (uuid :f1), :id (uuid :d5) :prev-id (uuid :d4), :op [:str/ins (uuid :d4) "h"], :pad 1, :meta {:timestamp 1, :version 1.0}}

   {:branch-id (uuid :b1-2), :file-id (uuid :f0), :id (uuid :d6) :prev-id (uuid :d5), :op [:str/ins (uuid :d1) "h"], :pad 1, :meta {:timestamp 1, :version 1.0}}
   {:branch-id (uuid :b1-2), :file-id (uuid :f0), :id (uuid :d7) :prev-id (uuid :d6), :op [:str/ins (uuid :d6) "h"], :pad 1, :meta {:timestamp 1, :version 1.0}}
   {:branch-id (uuid :b1-2), :file-id (uuid :f1), :id (uuid :d8) :prev-id (uuid :d7), :op [:str/ins (uuid :d7) "h"], :pad 1, :meta {:timestamp 1, :version 1.0}}])

(def branches
  [{:db/id (uuid :b0)}
   {:db/id (uuid :b1-1) ::branch/parent {:db/id (uuid :b0) ::branch/branch-off-delta-id (uuid :d0)}}
   {:db/id (uuid :b1-2) ::branch/parent {:db/id (uuid :b0) ::branch/branch-off-delta-id (uuid :d1)}}])

(defn chunks-vec
  [a b]
  [(chunk/with-bounds a true nil) (chunk/with-bounds b nil true)])

(deftest add-delta-to-chunks-by-branch-id-test
  (let [{[chk0 chk1
          chk2 chk3
          chk4 chk5] :seq
         uuid-fn     :f} (uuid-gen)
        [d0 d1 d2
         d3 d4 d5
         d6 d7 d8]       deltas
        expect           {(uuid :b0)   (chunks-vec (chunk/new-chunk chk0 0 [d0 d1] true) (chunk/new-chunk chk1 0 [d2] false))
                          (uuid :b1-1) (chunks-vec (chunk/new-chunk chk2 1 [d3 d4] true) (chunk/new-chunk chk3 1 [d5] false))
                          (uuid :b1-2) (chunks-vec (chunk/new-chunk chk4 1 [d6 d7] true) (chunk/new-chunk chk5 1 [d8] false))}
        actual           (reduce
                          (fn [chunks-by-branch-id delta]
                            (sut/add-delta chunks-by-branch-id branches uuid-fn delta))
                          sut/emtpy-chunks-by-branch-id deltas)]
    (is (= expect actual))))
