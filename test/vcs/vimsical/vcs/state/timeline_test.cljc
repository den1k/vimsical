(ns vimsical.vcs.state.timeline-test
  #?@(:clj
      [(:require
        [clojure.test :as t :refer [are deftest is testing]]
        [orchestra.spec.test :as st]
        [vimsical.common.test :refer [uuid uuid-gen]]
        [vimsical.vcs.branch :as branch]
        [vimsical.vcs.state.branches :as state.branches]
        [vimsical.vcs.state.chunk :as chunk]
        [vimsical.vcs.state.timeline :as sut])]
      :cljs
      [(:require
        [clojure.spec.test :as st]
        [clojure.test :as t :refer-macros [are deftest is testing]]
        [vimsical.common.test :refer [uuid uuid-gen]]
        [vimsical.vcs.branch :as branch]
        [vimsical.vcs.state.branches :as state.branches]
        [vimsical.vcs.state.chunk :as chunk]
        [vimsical.vcs.state.timeline :as sut])]))

(st/instrument)

(def deltas
  [{:branch-id (uuid :b0),   :file-id (uuid :f0), :id (uuid :d0) :prev-id nil,        :op [:str/ins nil        "h"], :pad 1, :meta {:timestamp 1, :version 1.0}}
   {:branch-id (uuid :b0),   :file-id (uuid :f0), :id (uuid :d1) :prev-id (uuid :d0), :op [:str/ins (uuid :d0) "h"], :pad 1, :meta {:timestamp 1, :version 1.0}}
   {:branch-id (uuid :b0),   :file-id (uuid :f1), :id (uuid :d2) :prev-id (uuid :d1), :op [:str/ins (uuid :d1) "h"], :pad 1, :meta {:timestamp 1, :version 1.0}}

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

(deftest add-delta-to-chunks-by-branch-id-test
  (let [{[chk0 chk1
          chk2 chk3
          chk4 chk5] :seq
         uuid-fn     :f} (uuid-gen)
        [d0 d1 d2
         d3 d4 d5
         d6 d7 d8]       deltas
        expect           {(uuid :b0)   [(chunk/new-chunk chk0 0 [d0 d1] true) (chunk/new-chunk chk1 0 [d2] false)]
                          (uuid :b1-1) [(chunk/new-chunk chk2 1 [d3 d4] true) (chunk/new-chunk chk3 1 [d5] false)]
                          (uuid :b1-2) [(chunk/new-chunk chk4 1 [d6 d7] true) (chunk/new-chunk chk5 1 [d8] false)]}
        actual           (reduce
                          (fn [cbb delta]
                            (sut/add-delta-to-chunks-by-branch-id cbb branches uuid-fn delta))
                          nil  deltas)]
    (is (= expect actual))))


(defn dissoc-chunk-ids [coll]
  (cond
    (nil? coll) nil

    (sequential? coll)
    (mapv dissoc-chunk-ids coll)

    (and (map? coll) (sorted? coll))
    (reduce-kv
     (fn [coll k chunks]
       (assoc coll k (dissoc-chunk-ids chunks)))
     (empty coll) coll)

    (::chunk/id coll) (dissoc coll ::chunk/id)

    :else
    (do
      (assert (map? coll))
      (reduce-kv
       (fn [coll k chunks]
         (assoc coll k (dissoc-chunk-ids chunks)))
       (empty coll) coll))))

(deftest add-delta-test
  (let [{[chk0 chk1
          chk2 chk3
          chk4 chk5] :seq
         uuid-fn     :f} (uuid-gen)
        [d0 d1 d2
         d3 d4 d5
         d6 d7 d8]       deltas
        actual           (sut/add-deltas sut/empty-timeline branches uuid-fn deltas)]
    (testing "chunks-by-branch-id"
      (let [[chunk0 chunk1 :as b0-chunks]   [(chunk/new-chunk chk0 0 [d0 d1] true) (chunk/new-chunk chk1 0 [d2] false)]
            [chunk2 chunk3 :as b1-1-chunks] [(chunk/new-chunk chk2 1 [d3 d4] true) (chunk/new-chunk chk3 1 [d5] false)]
            [chunk4 chunk5 :as b1-2-chunks] [(chunk/new-chunk chk4 1 [d6 d7] true) (chunk/new-chunk chk5 1 [d8] false)]
            expect                          {(uuid :b0) b0-chunks (uuid :b1-1) b1-1-chunks (uuid :b1-2) b1-2-chunks}
            actual                          (::sut/chunks-by-branch-id actual)]
        (is (= (dissoc-chunk-ids expect) (dissoc-chunk-ids actual)))))
    (testing "chunks-by-absolute-start-time"
      (let [expect {0 (chunk/new-chunk (uuid :chk0) 0 [d0] false)
                    ;; b1-1, f0
                    1 (chunk/new-chunk (uuid :chk1) 1 [d3 d4] true)
                    ;; b1-1, f1
                    3 (chunk/new-chunk (uuid :chk2) 1 [d5] false)
                    ;; b0, f0
                    4 (chunk/new-chunk (uuid :chk) 0 [d1] false)
                    ;; b1-2, f0
                    5 (chunk/new-chunk (uuid :chk1) 1 [d6 d7] true)
                    ;; b1-2, f1
                    7 (chunk/new-chunk (uuid :chk1) 1 [d8] false)
                    ;; b0, f1
                    8 (chunk/new-chunk (uuid :chk0) 0 [d2] false)}
            actual (::sut/chunks-by-absolute-start-time actual)]
        (is (= (dissoc-chunk-ids expect) (dissoc-chunk-ids actual)))))
    (testing "timeline-duration"
      (let [expect (reduce + (map :pad deltas))
            actual (sut/duration actual)]
        (is (= expect actual))))
    (testing "delta-at-absolute-time"
      (are [delta-id t] (is (= delta-id (:id (sut/delta-at-absolute-time actual t))))
        (uuid :d0) 1
        (uuid :d3) 2
        (uuid :d4) 3
        (uuid :d5) 4
        (uuid :d1) 5
        (uuid :d6) 6
        (uuid :d7) 7
        (uuid :d8) 8
        (uuid :d2) 9))
    (testing "next-delta"
      )))
