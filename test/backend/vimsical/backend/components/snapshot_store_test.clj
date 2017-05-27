(ns vimsical.backend.components.snapshot-store-test
  (:require
   [clojure.core.async :as a]
   [clojure.test :refer [deftest is testing use-fixtures]]
   [orchestra.spec.test :as st]
   [vimsical.backend.components.snapshot-store.fixture :as fixture :refer [*snapshot-store*]]
   [vimsical.backend.components.snapshot-store.protocol :as p]
   [vimsical.backend.util.async :refer [<??]]
   [vimsical.common.test :refer [uuid]]
   [vimsical.vcs.snapshot :as snapshot]))

(st/instrument)

(use-fixtures :once fixture/once)
(use-fixtures :each fixture/each)

(deftest snapshots-io-test
  (let [expect
        [{::snapshot/user-uid (uuid :user) ::snapshot/vims-uid (uuid :vims) ::snapshot/file-uid (uuid :file1) ::snapshot/delta-uid nil ::snapshot/text "file1"}
         {::snapshot/user-uid (uuid :user) ::snapshot/vims-uid (uuid :vims) ::snapshot/file-uid (uuid :file2) ::snapshot/delta-uid nil ::snapshot/text "file2"}]]
    (testing "ISnapshotStoreChan"
      (is (nil? (<?? (p/insert-snapshots-chan *snapshot-store* expect))))
      (is (= (set expect) (set (<?? (a/into [] (p/select-snapshots-chan *snapshot-store* (uuid :user) (uuid :vims))))))))
    (testing "ISnapshotStoreAync"
      (let [wc     (a/chan 1)
            rc     (a/chan 1)
            _      (p/insert-snapshots-async *snapshot-store* expect (partial a/put! wc) (partial a/put! wc))
            _      (a/<!! wc)
            _      (p/select-snapshots-async *snapshot-store* (uuid :user) (uuid :vims) (partial a/put! rc) (partial a/put! rc))
            actual (a/<!! rc)]
        (is (= (set expect) (set actual)))))))
