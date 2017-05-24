(ns vimsical.integration.remotes.backend.vims.commands-test
  (:require
   [clojure.spec :as s]
   [clojure.test :refer [deftest is use-fixtures]]
   [com.stuartsierra.mapgraph :as mg]
   [day8.re-frame.test :as re-frame.test]
   [orchestra.spec.test :as st]
   [re-frame.core :as re-frame]
   [vimsical.backend.system.fixture :as system.fixture]
   [vimsical.common.test :refer [uuid]]
   [vimsical.frontend.vims.handlers :as frontend.vims.handlers]
   [vimsical.frontend.db :as db]
   [vimsical.frontend.remotes.fx :as frontend.remotes.fx]
   [vimsical.queries.user :as queries.user]
   [vimsical.remotes.backend.vims.commands :as vims.commands]
   [vimsical.vims :as vims]
   [vimsical.user :as user]
   [vimsical.backend.data :as data]))

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
  (:app/user
   (mg/pull
    @db-sub
    [{[:app/user '_] queries.user/pull-query}])))

;;
;; * Vims
;;

(deftest new-vims-test
  (let [ ;;;; DATA
        owner         {:db/uid (uuid ::data/user)}
        status-key    (uuid)
        handler-event [::frontend.vims.handlers/new owner status-key]
        status-sub    (re-frame/subscribe [::frontend.remotes.fx/status :backend status-key])
        db-sub        (re-frame/subscribe [::db/db])]
    (re-frame/dispatch handler-event)
    (is (= ::frontend.remotes.fx/success @status-sub))
    (is (= 2 (-> db-sub get-app-user ::user/vimsae count)))))

;;
;; * TODO Title
;;

;;
;; * TODO Snapshots
;;
