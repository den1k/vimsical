(ns vimsical.backend.components.delta-store-test
  (:require
   [clojure.core.async :as a]
   [vimsical.common.test :refer [uuid]]
   [clojure.test :refer [deftest testing is are use-fixtures]]
   [orchestra.spec.test :as st]
   [vimsical.backend.components.delta-store.fixture :as fixture :refer [*delta-store*]]
   [vimsical.backend.components.delta-store.protocol :as p]
   [vimsical.backend.adapters.cassandra.protocol :refer [<? <??]]))

(st/instrument)

(use-fixtures :once fixture/once)
(use-fixtures :each fixture/each)

(deftest deltas-io-test
  (let [deltas [{:id        (uuid :id0) ,
                 :prev-id   nil,
                 :op        [:str/ins nil "h"],
                 :pad       0,
                 :file-id   (uuid :file)
                 :branch-id (uuid :branch)
                 :meta      {:timestamp 1, :version 1.0}}
                {:id        (uuid :id1) ,
                 :prev-id   (uuid :id0) ,
                 :op        [:str/ins (uuid :id0) "i"],
                 :pad       100,
                 :file-id   (uuid :file),
                 :branch-id (uuid :branch),
                 :meta      {:timestamp 1, :version 1.0}}]]
    (testing "IDeltaStoreChan"
      (is (nil? (<?? (p/insert-deltas-chan *delta-store* (uuid :user) (uuid :chan) deltas))))
      (is (= deltas (<?? (a/into [] (p/select-deltas-chan *delta-store* (uuid :user) (uuid :chan)))))))
    (testing "IDeltaStoreAync"
      (let [wc     (a/chan 1)
            rc     (a/chan 1)
            _      (p/insert-deltas-async *delta-store* (uuid :user) (uuid :async) deltas (partial a/put! wc) (partial a/put! wc))
            _      (a/<!! wc)
            _      (p/select-deltas-async *delta-store* (uuid :user) (uuid :async) (partial a/put! rc) (partial a/put! rc))
            actual (a/<!! rc)]
        (is (= deltas actual))))))
