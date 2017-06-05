(ns vimsical.integration.remotes.backend.user.queries-test
  (:require
   [clojure.test :refer [deftest is use-fixtures]]
   [com.stuartsierra.mapgraph :as mg]
   [day8.re-frame.test :as re-frame.test]
   [orchestra.spec.test :as st]
   [re-frame.core :as re-frame]
   [vimsical.backend.data :as data]
   [vimsical.backend.handlers.user.queries :as user.queries]
   [vimsical.backend.system.fixture :as system.fixture]
   [vimsical.common.uuid :as uuid]
   [vimsical.common.test :refer [uuid]]
   [vimsical.frontend.db :as db]
   [vimsical.frontend.remotes.fx :as frontend.remotes.fx]
   [vimsical.frontend.user.handlers :as user.handlers]
   [vimsical.queries.user :as queries.user]
   [vimsical.vcs.snapshot :as snapshot]))

(st/instrument)

;;
;; * Re-frame
;;

(def test-db (db/new-db {:app/user nil}))

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
  system.fixture/vims
  system.fixture/snapshots
  re-frame-fixture)

;;
;; * Helpers
;;

(defn get-app-user
  [db-sub]
  (-> @db-sub
      (mg/pull [{[:app/user '_] queries.user/frontend-pull-query}])
      :app/user))

(deftest snapshots-join-test
  (is (= data/me (#'user.queries/user-join-snapshots data/user data/snapshots (constantly (uuid ::data/html-snapshot))))))

;;
;; * Me
;;

(deftest me-test
  (let [status-key    (uuid)
        handler-event [::user.handlers/me status-key]
        status-sub    (re-frame/subscribe [::frontend.remotes.fx/status :backend status-key])
        db-sub        (re-frame/subscribe [::db/db])]
    ;; Can't easily pass the fn down...
    (with-redefs [uuid/uuid (constantly (uuid ::data/html-snapshot))]
      (re-frame/dispatch handler-event)
      (is (= ::frontend.remotes.fx/success @status-sub))
      (is (= data/me (-> db-sub get-app-user))))))
