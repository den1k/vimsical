(ns vimsical.vcs.validation-test
  (:require
   #?(:clj  [clojure.test :as t]
      :cljs [cljs.test :as t :include-macros true])
   [vimsical.common.test :refer [uuid]]
   [vimsical.vcs.alg.topo :as topo]
   [vimsical.vcs.validation :as sut]))

;;
;; * Mock data
;;

(defn stub-deltas
  ([] (stub-deltas (uuid :branch)))
  ([branch-uid]
   [{:uid (uuid 0) :prev-uid nil :op [:str/ins nil "H"] :pad 100 :file-uid (uuid :file) :branch-uid branch-uid :meta {:timestamp 100 :version 2}}
    {:uid (uuid 1) :prev-uid (uuid 0) :op [:str/ins (uuid 0) "i"] :pad 100 :file-uid (uuid :file) :branch-uid branch-uid :meta {:timestamp 200 :version 2}}]))

(defn ok-next-deltas
  ([] (ok-next-deltas (uuid :branch)))
  ([branch-uid]
   [{:uid (uuid 2) :prev-uid (uuid 1) :op [:str/ins (uuid 1) "!"] :pad 100 :file-uid (uuid :file) :branch-uid branch-uid :meta {:timestamp 300 :version 2}}]))

(defn bad-next-deltas
  ([] (bad-next-deltas (uuid)))
  ([uid]
   [{:uid uid :prev-uid (uuid :oops) :op [:str/ins (uuid :oops) "?"] :pad 100 :file-uid (uuid :file) :branch-uid (uuid :branch) :meta {:timestamp 400 :version 2}}]))

(defn stub-new-branch-deltas []
  [{:uid (uuid :new-branch0) :prev-uid (uuid 1) :op [:str/ins (uuid 0) "H"] :pad 100 :file-uid (uuid :file) :branch-uid (uuid :new-branch) :meta {:timestamp 100 :version 2}}])

(defn ok-new-branch-next-deltas []
  [{:uid (uuid 3) :prev-uid (uuid 2) :op [:str/ins (uuid 1) "i"] :pad 100 :file-uid (uuid :file) :branch-uid (uuid :new-branch) :meta {:timestamp 100 :version 2}}])

(defn bad-new-branch-next-deltas []
  [{:uid (uuid) :prev-uid (uuid :oops) :op [:str/ins (uuid :oops) "X"] :pad 100 :file-uid (uuid :file) :branch-uid (uuid :new-branch) :meta {:timestamp 100 :version 2}}])

(def delta-by-branch-uid
  {(uuid :branch) (last (stub-deltas))})

(def order-by-branch-uid
  {(uuid :branch) (count (stub-deltas))})

(def next-delta-by-branch-uid
  {(uuid :branch) (last (ok-next-deltas))})

(def next-order-by-branch-uid
  {(uuid :branch) (+ (count (stub-deltas)) (count (ok-next-deltas)))})

;;
;; * Heleprs
;;

(defn ->state
  [deltas]
  (reduce-kv
   (fn [m branch-uid deltas]
     (assoc m branch-uid (-> deltas last (select-keys [:uid :prev-uid :branch-uid]))))
   {} (group-by :branch-uid deltas)))

(defn validate! [delta-by-branch-uid deltas]
  (= deltas (into [] (sut/validate-deltas-xf delta-by-branch-uid) deltas)))

(defn reduce-validate! [& ndeltas]
  (reduce
   (fn [state deltas]
     (if (validate! state deltas)
       (->state deltas)
       (reduced false)))
   {} ndeltas))

;;
;; * Tests
;;

(t/deftest validate-fixtures-test
  (t/testing "Ensure our test data is correct!"
    (let [base      (concat (stub-deltas) (ok-next-deltas))
          next-ok   (concat base (stub-new-branch-deltas) (ok-new-branch-next-deltas))
          next-bad1 (concat base (bad-next-deltas))
          next-bad2 (concat next-ok (bad-next-deltas))]
      (t/is (topo/sorted? base))
      (t/is (topo/sorted? next-ok))
      (t/is (not (topo/sorted? next-bad1)))
      (t/is (not (topo/sorted? next-bad2))))))

(t/deftest simple-validation-test
  (t/testing "Throws"
    (t/is (thrown?
           #?(:clj clojure.lang.ExceptionInfo :cljs cljs.core.ExceptionInfo)
           (validate! {} (ok-next-deltas)))
          "rejects when deltas don't start at begining")
    (t/is (thrown?
           #?(:clj clojure.lang.ExceptionInfo :cljs cljs.core.ExceptionInfo)
           (validate! delta-by-branch-uid (bad-next-deltas)))
          "reject discontinuity from delta-by-branch-uid")
    (t/is (thrown?
           #?(:clj clojure.lang.ExceptionInfo :cljs cljs.core.ExceptionInfo)
           (validate! delta-by-branch-uid (into (ok-next-deltas) (bad-next-deltas))))
          "rejects any discontinuity"))
  (t/testing "Proceeds"
    (t/is (validate! {} (stub-deltas))
          "accepts changes from beginning with empty delta-by-branch-uid")
    (t/is (validate! delta-by-branch-uid (ok-next-deltas))
          "accepts continuity from delta-by-branch-uid")
    (t/is (validate! delta-by-branch-uid (ok-next-deltas (uuid)))
          "accepts new branches")))

(t/deftest batched-validation-test
  (t/is (validate! delta-by-branch-uid (into (ok-next-deltas) (stub-new-branch-deltas)))
        "accepts valid batches containing a new branch")
  (t/is (reduce-validate!
         (stub-deltas)
         (stub-new-branch-deltas)
         (into
          (ok-next-deltas)
          (ok-new-branch-next-deltas)))
        "accepts valid batches with multiple branches")
  (t/testing "reject invallid batches across branches"
    (t/testing "Rejects whole batch if one partition is invalid"
      (t/is
       (thrown?
        #?(:clj clojure.lang.ExceptionInfo :cljs cljs.core.ExceptionInfo)
        (reduce-validate! (stub-deltas) (ok-next-deltas) (into (stub-new-branch-deltas) (bad-next-deltas)))))
      (t/is
       (thrown?
        #?(:clj clojure.lang.ExceptionInfo :cljs cljs.core.ExceptionInfo)
        (reduce-validate! (stub-deltas) (ok-next-deltas) (into (stub-new-branch-deltas) (bad-new-branch-next-deltas))))))))

(t/deftest update-delta-by-branch-uid-test
  (let [ldbb {(uuid :branch) (last (stub-deltas))}
        next-ldbb {(uuid :branch) (last (ok-next-deltas))}]
    (t/testing "Bootstrap"
      (t/is (= ldbb (sut/update-delta-by-branch-uid {} (stub-deltas)))))
    (t/testing "Update"
      (t/is (= next-ldbb (-> {}
                             (sut/update-delta-by-branch-uid (stub-deltas))
                             (sut/update-delta-by-branch-uid (ok-next-deltas))
                             (sut/update-delta-by-branch-uid nil)))))
    (t/testing "Update with branches"
      (let [first-deltas  (stub-deltas)
            second-deltas (stub-new-branch-deltas)
            expect        {(:branch-uid (last first-deltas))  (last first-deltas)
                           (:branch-uid (last second-deltas)) (last second-deltas)}
            actual        (-> {}
                              (sut/update-delta-by-branch-uid first-deltas)
                              (sut/update-delta-by-branch-uid second-deltas))]
        (t/is (= expect actual))))))
