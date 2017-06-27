(ns vimsical.vcs.state.chunks-test
  #?@(:clj
      [(:require
        [orchestra.spec.test :as st]
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

#?(:clj (st/instrument))

(def deltas
  [{:branch-uid (uuid :b0),,, :file-uid (uuid :f0), :uid (uuid :d0) :prev-uid nil,,,,,,,, :op [:str/ins nil "h"],,,,,,,, :pad 1, :meta {:timestamp 1, :version 1.0}}
   {:branch-uid (uuid :b0),,, :file-uid (uuid :f0), :uid (uuid :d1) :prev-uid (uuid :d0), :op [:str/ins (uuid :d0) "h"], :pad 1, :meta {:timestamp 1, :version 1.0}}
   {:branch-uid (uuid :b0),,, :file-uid (uuid :f1), :uid (uuid :d2) :prev-uid (uuid :d1), :op [:str/ins (uuid :d1) "h"], :pad 1, :meta {:timestamp 1, :version 1.0}}

   {:branch-uid (uuid :b1-1), :file-uid (uuid :f0), :uid (uuid :d3) :prev-uid (uuid :d2), :op [:str/ins (uuid :d0) "h"], :pad 1, :meta {:timestamp 1, :version 1.0}}
   {:branch-uid (uuid :b1-1), :file-uid (uuid :f0), :uid (uuid :d4) :prev-uid (uuid :d3), :op [:str/ins (uuid :d3) "h"], :pad 1, :meta {:timestamp 1, :version 1.0}}
   {:branch-uid (uuid :b1-1), :file-uid (uuid :f1), :uid (uuid :d5) :prev-uid (uuid :d4), :op [:str/ins (uuid :d4) "h"], :pad 1, :meta {:timestamp 1, :version 1.0}}

   {:branch-uid (uuid :b1-2), :file-uid (uuid :f0), :uid (uuid :d6) :prev-uid (uuid :d5), :op [:str/ins (uuid :d1) "h"], :pad 1, :meta {:timestamp 1, :version 1.0}}
   {:branch-uid (uuid :b1-2), :file-uid (uuid :f0), :uid (uuid :d7) :prev-uid (uuid :d6), :op [:str/ins (uuid :d6) "h"], :pad 1, :meta {:timestamp 1, :version 1.0}}
   {:branch-uid (uuid :b1-2), :file-uid (uuid :f1), :uid (uuid :d8) :prev-uid (uuid :d7), :op [:str/ins (uuid :d7) "h"], :pad 1, :meta {:timestamp 1, :version 1.0}}])

(def branches
  [{:db/uid (uuid :b0)}
   {:db/uid (uuid :b1-1) ::branch/parent {:db/uid (uuid :b0) ::branch/branch-off-delta-uid (uuid :d0)}}
   {:db/uid (uuid :b1-2) ::branch/parent {:db/uid (uuid :b0) ::branch/branch-off-delta-uid (uuid :d1)}}])

(defn chunks-vec
  [a b]
  [(chunk/with-bounds a true nil) (chunk/with-bounds b nil true)])

(deftest add-delta-test
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
                          (fn [chunks-by-branch-uid delta]
                            (sut/add-delta chunks-by-branch-uid branches uuid-fn delta))
                          sut/emtpy-chunks-by-branch-uid deltas)]
    (is (= expect actual))))

(deftest add-deltas-test
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
        actual           (sut/add-deltas sut/emtpy-chunks-by-branch-uid branches uuid-fn deltas)]
    (is (= expect actual))))

(deftest add-deltas-by-branch-uid-test
  (let [{[chk0 chk1
          chk2 chk3
          chk4 chk5] :seq
         uuid-fn     :f}     (uuid-gen)
        [d0 d1 d2
         d3 d4 d5
         d6 d7 d8]           deltas
        deltas-by-branch-uid (group-by :branch-uid deltas)
        expect               {(uuid :b0)   (chunks-vec (chunk/new-chunk chk0 0 [d0 d1] true) (chunk/new-chunk chk1 0 [d2] false))
                              (uuid :b1-1) (chunks-vec (chunk/new-chunk chk2 1 [d3 d4] true) (chunk/new-chunk chk3 1 [d5] false))
                              (uuid :b1-2) (chunks-vec (chunk/new-chunk chk4 1 [d6 d7] true) (chunk/new-chunk chk5 1 [d8] false))}
        actual               (sut/add-deltas-by-branch-uid sut/emtpy-chunks-by-branch-uid branches uuid-fn deltas-by-branch-uid)]
    (is (= expect actual))))
