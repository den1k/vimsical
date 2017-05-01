(ns vimsical.vcs.state.timeline2-test
  #?@(:clj
      [(:require
        [clojure.test :as t :refer [are deftest is testing]]
        [clojure.data.avl :as avl]
        [orchestra.spec.test :as st]
        [vimsical.vcs.examples :as examples]
        [vimsical.vcs.branch :as branch]
        [vimsical.vcs.data.indexed.vector :as indexed]
        [vimsical.vcs.state.branches :as state.branches]
        [vimsical.common.test :refer [uuid uuid-gen diff=]]
        [vimsical.vcs.state.timeline2 :as sut]
        [vimsical.vcs.state.chunk :as chunk])]
      :cljs
      [(:require
        [clojure.spec.test :as st]
        [clojure.data.avl :as avl]
        [clojure.test :as t :refer-macros [are deftest is testing]]
        [vimsical.vcs.branch :as branch]
        [vimsical.vcs.data.indexed.vector :as indexed]
        [vimsical.vcs.state.branches :as state.branches]
        [vimsical.vcs.examples :as examples]
        [vimsical.common.test :refer [uuid uuid-gen]]
        [vimsical.vcs.state.timeline2 :as sut]
        [vimsical.vcs.state.chunk :as chunk])]))

(st/instrument)

;; NOTE deltas aren't valid in terms of their string ops
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
         d6 d7 d8]    deltas
        expect           {(uuid :b0)   [(chunk/new-chunk chk0 0 [d0 d1] true) (chunk/new-chunk chk1 0 [d2] false)]
                          (uuid :b1-1) [(chunk/new-chunk chk2 1 [d3 d4] true) (chunk/new-chunk chk3 1 [d5] false)]
                          (uuid :b1-2) [(chunk/new-chunk chk4 1 [d6 d7] true) (chunk/new-chunk chk5 1 [d8] false)]}
        actual           (reduce
                          (fn [cbb delta]
                            (sut/add-delta-to-chunks-by-branch-id cbb branches uuid-fn delta))
                          nil  deltas)]
    (is (= expect actual))))


(def deltas-by-branch-id
  (state.branches/add-deltas state.branches/empty-deltas-by-branch-id deltas))

(defn dissoc-chunk-ids [coll]
  (println coll)
  (cond
    (sequential? coll)
    (mapv dissoc-chunk-ids coll)

    (and (map? coll) (sorted? coll))
    (reduce-kv
     (fn [coll k chunks]
       (assoc coll k (dissoc-chunk-ids chunks)))
     (empty coll) coll)

    (::chunk/id coll) (dissoc coll ::chunk/id)

    :else
    (do (assert (map? coll))
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
        actual           (second
                          (reduce
                           (fn [[deltas-by-branch-id timeline] delta]
                             (let [deltas-by-branch-id' (state.branches/add-delta deltas-by-branch-id delta)
                                   timeline'            (sut/add-delta timeline deltas-by-branch-id' branches uuid-fn delta)]
                               ;; (println timeline')
                               [deltas-by-branch-id' timeline']))
                           [state.branches/empty-deltas-by-branch-id sut/empty-timeline] deltas))]
    (testing "chunks-by-branch-id"
      (let [[chunk0 chunk1 :as b0-chunks]   [(chunk/new-chunk chk0 0 [d0 d1] true) (chunk/new-chunk chk1 0 [d2] false)]
            [chunk2 chunk3 :as b1-1-chunks] [(chunk/new-chunk chk2 1 [d3 d4] true) (chunk/new-chunk chk3 1 [d5] false)]
            [chunk4 chunk5 :as b1-2-chunks] [(chunk/new-chunk chk4 1 [d6 d7] true) (chunk/new-chunk chk5 1 [d8] false)]
            expect                          {(uuid :b0) b0-chunks (uuid :b1-1) b1-1-chunks (uuid :b1-2) b1-2-chunks}
            actual                          (::sut/chunks-by-branch-id actual)]
        (diff= (dissoc-chunk-ids expect) (dissoc-chunk-ids actual))))
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
        ;; (clojure.pprint/pprint
        ;;  {:expect expect
        ;;   :expect-d (dissoc-chunk-ids expect)
        ;;   :actual actual
        ;;   :actual-d (dissoc-chunk-ids actual)})
        (diff= (dissoc-chunk-ids expect) (dissoc-chunk-ids actual))))
    ))

;; {0 #:vimsical.vcs.state.chunk{:branch-id #uuid "5906a033-6cb2-49fc-ab76-2fdd071ea9f7", :file-id #uuid "5906a033-eaa2-4313-8172-5564dc9a8677", :delta-branch-off-id nil, :duration 2, :delta-end-id #uuid "5906a033-c74e-4d61-ae32-4a1c137f1142", :deltas-by-relative-time {1 {:branch-id #uuid "5906a033-6cb2-49fc-ab76-2fdd071ea9f7", :file-id #uuid "5906a033-eaa2-4313-8172-5564dc9a8677", :id #uuid "5906a033-7e2b-479f-8d11-c96fbe1e025e", :prev-id nil, :op [:str/ins nil h], :pad 1, :meta {:timestamp 1, :version 1.0}}, 2 {:branch-id #uuid "5906a033-6cb2-49fc-ab76-2fdd071ea9f7", :file-id #uuid "5906a033-eaa2-4313-8172-5564dc9a8677", :id #uuid "5906a033-c74e-4d61-ae32-4a1c137f1142", :prev-id #uuid "5906a033-7e2b-479f-8d11-c96fbe1e025e", :op [:str/ins #uuid "5906a033-7e2b-479f-8d11-c96fbe1e025e" h], :pad 1, :meta {:timestamp 1, :version 1.0}}}, :delta-start-id #uuid "5906a033-7e2b-479f-8d11-c96fbe1e025e", :id #uuid "59074eb2-f4b9-4c16-af5b-ccb2d6743834", :count 2, :depth 0},
;;  2 #:vimsical.vcs.state.chunk{:branch-id #uuid "5906a033-6cb2-49fc-ab76-2fdd071ea9f7", :file-id #uuid "5906a033-06c6-42e8-99c0-2a6d90d1aed6", :duration 1, :delta-end-id #uuid "5906a033-a084-4402-a97f-ff9f26a73d3d", :deltas-by-relative-time {1 {:branch-id #uuid "5906a033-6cb2-49fc-ab76-2fdd071ea9f7", :file-id #uuid "5906a033-06c6-42e8-99c0-2a6d90d1aed6", :id #uuid "5906a033-a084-4402-a97f-ff9f26a73d3d", :prev-id #uuid "5906a033-c74e-4d61-ae32-4a1c137f1142", :op [:str/ins #uuid "5906a033-c74e-4d61-ae32-4a1c137f1142" h], :pad 1, :meta {:timestamp 1, :version 1.0}}}, :delta-start-id #uuid "5906a033-a084-4402-a97f-ff9f26a73d3d", :id #uuid "59074eb2-2c82-48d0-a69e-89c27641bd00", :count 1, :depth 0}}
