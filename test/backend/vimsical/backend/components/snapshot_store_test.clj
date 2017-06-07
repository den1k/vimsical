(ns vimsical.backend.components.snapshot-store-test
  (:require
   [clojure.core.async :as a]
   [clojure.test :refer [deftest is use-fixtures]]
   [orchestra.spec.test :as st]
   [vimsical.backend.components.snapshot-store.fixture :as fixture :refer [*snapshot-store*]]
   [vimsical.backend.components.snapshot-store.protocol :as p]
   [vimsical.backend.util.async :refer [<??]]
   [vimsical.common.test :refer [uuid]]
   [vimsical.vcs.snapshot :as snapshot]))

(st/instrument)

(use-fixtures :once fixture/once)
(use-fixtures :each fixture/each)

(def expect
  [{::snapshot/user-uid (uuid :user)
    ::snapshot/vims-uid (uuid :vims)
    ::snapshot/file-uid (uuid :file1)
    ::snapshot/delta-uid nil
    ::snapshot/text "file1"}
   {::snapshot/user-uid (uuid :user)
    ::snapshot/vims-uid (uuid :vims)
    ::snapshot/file-uid (uuid :file2)
    ::snapshot/delta-uid nil
    ::snapshot/text "file2"}])

(deftest vims-test
  (is (nil? (<?? (p/insert-snapshots-chan *snapshot-store* expect))))
  (is (= (set expect) (set (<?? (a/into [] (p/select-vims-snapshots-chan *snapshot-store* (uuid :user) (uuid :vims))))))))

(deftest user-test
  (is (nil? (<?? (p/insert-snapshots-chan *snapshot-store* expect))))
  (is (= (set expect) (set (<?? (a/into [] (p/select-user-snapshots-chan *snapshot-store* (uuid :user))))))))
