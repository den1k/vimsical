(ns vimsical.backend.components.delta-store-test
  (:require
   [clojure.core.async :as a]
   [clojure.test :refer [deftest is testing use-fixtures]]
   [orchestra.spec.test :as st]
   [vimsical.backend.components.delta-store :as sut]
   [vimsical.backend.components.delta-store.fixture :as fixture :refer [*delta-store*]]
   [vimsical.backend.components.delta-store.protocol :as p]
   [vimsical.backend.util.async :refer [<??]]
   [vimsical.common.test :refer [uuid uuid-gen]]
   [vimsical.vcs.core :as vcs]
   [vimsical.vcs.data.gen.diff :as diff]
   [vimsical.vcs.editor :as editor]
   [vimsical.vcs.validation :as validation]))

(st/instrument)

(use-fixtures :once fixture/once)
(use-fixtures :each fixture/each)

(defn delta->session-delta   [delta]  (select-keys delta [:uid :prev-uid :branch-uid]))
(defn deltas->session-deltas [deltas] (mapv delta->session-delta deltas))

(defn deltas->delta-by-branch-uid
  [deltas]
  (reduce-kv
   (fn [m branch-uid deltas]
     (assoc m branch-uid (last deltas)))
   {} (group-by :branch-uid deltas)))

(defn deltas->order-by-branch-uid
  [deltas]
  (reduce-kv
   (fn [m branch-uid deltas]
     (assoc m branch-uid (dec (count deltas))))
   {} (group-by :branch-uid deltas)))

;;
;; * Data
;;

;; NOTE
;;
;; The clustering key requires that a branch's uid sorts AFTER its parent's uid
;;

(def master-uid #uuid "59397c40-e05f-4f59-8fc2-081fc9792300")
(def child1-uid #uuid "59397c40-e05f-4f59-8fc2-081fc9792301")
(def child2-uid #uuid "59397c40-e05f-4f59-8fc2-081fc9792302")

(defn stub-deltas
  ([] (stub-deltas child1-uid))
  ([branch-uid]
   [{:uid (uuid 0) :prev-uid nil :op [:str/ins nil "H"] :pad 100 :file-uid (uuid ::file) :branch-uid branch-uid :meta {:timestamp 100 :version 2}}
    {:uid (uuid 1) :prev-uid (uuid 0) :op [:str/ins (uuid 0) "i"] :pad 100 :file-uid (uuid ::file) :branch-uid branch-uid :meta {:timestamp 200 :version 2}}]))

(defn ok-next-deltas
  ([] (ok-next-deltas child1-uid))
  ([branch-uid]
   [{:uid (uuid 2) :prev-uid (uuid 1) :op [:str/ins (uuid 1) "!"] :pad 100 :file-uid (uuid ::file) :branch-uid branch-uid :meta {:timestamp 300 :version 2}}]))

(defn stub-new-branch-deltas []
  [{:uid (uuid :new-branch0) :prev-uid (uuid 1) :op [:str/ins (uuid 0) "H"] :pad 100 :file-uid (uuid ::file) :branch-uid child2-uid :meta {:timestamp 100 :version 2}}])

(let [{uuid-fn :f}        (uuid-gen)
      effects             {::editor/pad-fn       (constantly 1)
                           ::editor/timestamp-fn (constantly 2)
                           ::editor/uuid-fn      uuid-fn}
      branches            [{:db/uid master-uid}]
      vcs                 (vcs/empty-vcs branches)
      [vcs  _ delta-uid]  (diff/diffs->vcs vcs effects (uuid ::file) master-uid nil "" ["abc"] ["def"] ["abcde"])
      [vcs' _ delta-uid'] (diff/diffs->vcs vcs effects (uuid ::file) child1-uid delta-uid "" ["abcdefg"] ["defg"])]
  (def master-deltas (vcs/deltas vcs delta-uid))
  (def branch-deltas (vcs/deltas vcs' delta-uid'))
  (def branch-deltas-only (remove (fn [{:keys [branch-uid]}] (= master-uid branch-uid)) branch-deltas))
  (def branch-deltas1 (take 5 branch-deltas))
  (def branch-deltas2 (drop 5 branch-deltas))
  (def session-master-deltas (deltas->session-deltas master-deltas))
  (def session-branch-deltas (deltas->session-deltas branch-deltas))
  (def session-branch-deltas1 (deltas->session-deltas branch-deltas1))
  (def session-branch-deltas2 (deltas->session-deltas branch-deltas2)))

;;
;; * Vims session validation tests
;;

(deftest vims-session-test
  (testing "Session initialization from cassandra"
    (let [expect {::validation/delta-by-branch-uid (deltas->delta-by-branch-uid session-branch-deltas)
                  ::validation/order-by-branch-uid (deltas->order-by-branch-uid session-branch-deltas)}]
      (is (nil? (<?? (p/insert-deltas-chan *delta-store* (uuid :vims) (uuid :user) {} branch-deltas))))
      (is (= expect (a/<!! (p/select-vims-session-chan *delta-store* (uuid :vims) (uuid :user)))))))
  (testing "Session updates"
    (testing "Batches over a single branch"
      (let [all-deltas      (concat (stub-deltas) (ok-next-deltas))
            [batch1 batch2] (split-at (rand-int (count all-deltas)) all-deltas)]
        (is (= {::validation/delta-by-branch-uid (deltas->delta-by-branch-uid all-deltas)
                ::validation/order-by-branch-uid (deltas->order-by-branch-uid all-deltas)}
               (-> {}
                   (sut/vims-session batch1)
                   (sut/vims-session batch2)
                   (sut/vims-session nil))))))
    (testing "Batches across branches"
      (let [all-deltas      (concat (stub-deltas) (stub-new-branch-deltas))
            [batch1 batch2] (split-at (rand-int (count all-deltas)) all-deltas)]
        (is (= {::validation/delta-by-branch-uid (deltas->delta-by-branch-uid all-deltas)
                ::validation/order-by-branch-uid (deltas->order-by-branch-uid all-deltas)}
               (-> {}
                   (sut/vims-session batch1)
                   (sut/vims-session batch2)
                   (sut/vims-session nil))))))))

(deftest validation-chan-test
  (let [session-stub-deltas            (deltas->session-deltas (stub-deltas))
        session-stub-new-branch-deltas (deltas->session-deltas (stub-new-branch-deltas))
        session-deltas                 (concat session-stub-deltas session-stub-new-branch-deltas)
        expect                         {::validation/delta-by-branch-uid
                                        {child1-uid     (last session-stub-deltas)
                                         child2-uid (last session-stub-new-branch-deltas)}
                                        ::validation/order-by-branch-uid
                                        {child1-uid     (dec (count session-stub-deltas))
                                         child2-uid (dec (count session-stub-new-branch-deltas))}}
        actual                         (sut/vims-session {} session-deltas)
        actual-chan                    (a/<!! (doto (sut/vims-session-chan 10) (a/onto-chan session-deltas)))]
    (is (= expect actual))
    (is (= expect actual-chan))))

;;
;; * I/O and ordering tests
;;

(deftest deltas-ordering-test
  (is (nil? (<?? (p/insert-deltas-chan *delta-store* (uuid :vims) (uuid :user) {} master-deltas))))
  (is (nil? (<?? (p/insert-deltas-chan *delta-store* (uuid :vims) (uuid :user) (sut/vims-session {} master-deltas) branch-deltas-only))))
  (is (thrown? clojure.lang.ExceptionInfo (<?? (p/insert-deltas-chan *delta-store* (uuid :vims) (uuid :user) {} branch-deltas-only))))
  (is (= branch-deltas (<?? (a/into [] (p/select-deltas-chan *delta-store* (uuid :vims))))))
  (is (= branch-deltas (<?? (a/into [] (p/select-deltas-chan *delta-store* (uuid :vims)))))))
