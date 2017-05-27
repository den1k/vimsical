(ns vimsical.integration.remotes.backend.vims.commands-test
  "NOTE these tests are used from the remote queries test to assert that remote"
  (:require
   [clojure.test :refer [deftest is testing use-fixtures]]
   [com.stuartsierra.mapgraph :as mg]
   [day8.re-frame.test :as re-frame.test]
   [orchestra.spec.test :as st]
   [re-frame.core :as re-frame]
   [vimsical.backend.data :as data]
   [vimsical.backend.system.fixture :as system.fixture]
   [vimsical.common.test :refer [uuid]]
   [vimsical.frontend.db :as db]
   [vimsical.frontend.remotes.fx :as frontend.remotes.fx]
   [vimsical.frontend.vcs.handlers :as vcs.handlers]
   [vimsical.frontend.vims.handlers :as frontend.vims.handlers]
   [vimsical.queries.user :as queries.user]
   [vimsical.user :as user]
   [vimsical.vcs.snapshot :as snapshot]
   [vimsical.vims :as vims]))

(st/instrument)

;;
;; * Re-frame
;;

(def test-db
  (db/new-db {:app/user data/user}))

(defn re-frame-fixture
  [f]
  (re-frame.test/with-temp-re-frame-state
    (re-frame.test/run-test-sync
     (re-frame/reg-event-db ::test-db-init (constantly test-db))
     (re-frame/dispatch [::test-db-init])
     (f))))

;;
;; * Fixtures
;;

(use-fixtures :each
  system.fixture/system
  (system.fixture/with-user data/user)
  system.fixture/session
  re-frame-fixture)

;;
;; * Helpers
;;

(defn get-app-user
  [db-sub]
  (-> @db-sub
      (mg/pull [{[:app/user '_] queries.user/frontend-pull-query}])
      :app/user))

;;
;; * Vims
;;

(deftest new-vims-test
  (let [owner         {:db/uid (uuid ::data/user)}
        status-key    (uuid)
        handler-event [::frontend.vims.handlers/new owner status-key]
        status-sub    (re-frame/subscribe [::frontend.remotes.fx/status :backend status-key])
        db-sub        (re-frame/subscribe [::db/db])]
    (re-frame/dispatch handler-event)
    (is (= ::frontend.remotes.fx/success @status-sub))
    (is (= 2 (-> db-sub get-app-user ::user/vimsae count)))))

;;
;; * Title
;;

(deftest set-title-test
  (let [status-key    (uuid)
        handler-event [::frontend.vims.handlers/title {:db/uid (uuid ::data/vims)} "Updated"
                       status-key]
        status-sub    (re-frame/subscribe [::frontend.remotes.fx/status :backend status-key])
        db-sub        (re-frame/subscribe [::db/db])]
    (re-frame/dispatch handler-event)
    (is (= ::frontend.remotes.fx/success @status-sub))
    (is (= "Updated" (-> db-sub get-app-user ::user/vimsae first ::vims/title)))))

;;
;; * Snapshots
;;

(deftest update-snapshots-test
  (let [status-key    (uuid)
        vims+vcs      (vcs.handlers/init-vims-vcs uuid data/vims data/deltas)
        handler-event [::frontend.vims.handlers/update-snapshots vims+vcs status-key]
        status-sub    (re-frame/subscribe [::frontend.remotes.fx/status :backend status-key])
        db-sub        (re-frame/subscribe [::db/db])]
    (re-frame/dispatch handler-event)
    (is (= ::frontend.remotes.fx/success @status-sub))
    (let [snapshots (->> db-sub get-app-user ::user/vimsae first ::vims/snapshots)]
      (testing "Created snapshots for files that have deltas"
        (is (= (->> data/deltas (map :file-uid) set count)
               (->>  snapshots count))))
      (testing "snapshot text"
        (is (= "abcdef" (->> snapshots first ::snapshot/text)))))))
