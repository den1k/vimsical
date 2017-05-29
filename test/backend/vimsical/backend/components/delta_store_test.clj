(ns vimsical.backend.components.delta-store-test
  (:require
   [vimsical.vcs.validation-test :as validation-test]
   [clojure.core.async :as a]
   [clojure.test :refer [deftest is testing use-fixtures]]
   [orchestra.spec.test :as st]
   [vimsical.backend.components.delta-store.fixture :as fixture :refer [*delta-store*]]
   [vimsical.backend.components.delta-store.protocol :as p]
   [vimsical.backend.components.delta-store :as sut]
   [vimsical.backend.util.async :refer [<??]]
   [vimsical.common.test :refer [uuid]]))

(st/instrument)

(use-fixtures :once fixture/once)
(use-fixtures :each fixture/each)

(deftest validation-chan-test
  (let [deltas (concat (validation-test/stub-deltas) (validation-test/stub-new-branch-deltas))
        expect {(uuid :branch)     (last (validation-test/stub-deltas))
                (uuid :new-branch) (last (validation-test/stub-new-branch-deltas))}
        actual (a/<!! (doto (sut/group-by-branch-uid-chan 10) (a/onto-chan deltas)))]
    (is (= expect actual))))

(deftest deltas-io-test
  (let [deltas [{:uid        (uuid :uid0) ,
                 :prev-uid   nil,
                 :op         [:str/ins nil "h"],
                 :pad        0,
                 :file-uid   (uuid :file)
                 :branch-uid (uuid :branch)
                 :meta       {:timestamp 1, :version 1.0}}
                {:uid        (uuid :uid1) ,
                 :prev-uid   (uuid :uid0) ,
                 :op         [:str/ins (uuid :uid0) "i"],
                 :pad        100,
                 :file-uid   (uuid :file),
                 :branch-uid (uuid :branch),
                 :meta       {:timestamp 1, :version 1.0}}]]
    (testing "IDeltaStoreChan"
      (is (nil? (<?? (p/insert-deltas-chan *delta-store* (uuid :chan) deltas))))
      (is (= deltas (<?? (a/into [] (p/select-deltas-chan *delta-store* (uuid :chan)))))))
    (testing "IDeltaStoreAync"
      (let [wc     (a/chan 1)
            rc     (a/chan 1)
            _      (p/insert-deltas-async *delta-store* (uuid :async) deltas (partial a/put! wc) (partial a/put! wc))
            _      (a/<!! wc)
            _      (p/select-deltas-async *delta-store* (uuid :async) (partial a/put! rc) (partial a/put! rc))
            actual (a/<!! rc)]
        (is (= deltas actual))))))
