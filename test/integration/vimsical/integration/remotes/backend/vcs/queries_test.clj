(ns vimsical.integration.remotes.backend.vcs.queries-test
  (:require
   [clojure.test :refer [deftest is use-fixtures]]
   [day8.re-frame.test :as re-frame.test]
   [orchestra.spec.test :as st]
   [re-frame.core :as re-frame]
   [vimsical.backend.data :as data]
   [vimsical.backend.system.fixture :as system.fixture]
   [vimsical.common.test :refer [uuid]]
   [vimsical.frontend.db :as db]
   [vimsical.frontend.remotes.fx :as frontend.remotes.fx]
   [vimsical.frontend.vcs.sync.db :as vcs.sync.db]
   [vimsical.frontend.vcs.sync.handlers :as vcs.sync.handlers]
   [vimsical.frontend.vcs.handlers :as vcs.handlers]))

(st/instrument)

;;
;; * Re-frame
;;

(def test-db (db/new-db
              {:app/user data/user
               :app/vims data/vims}))

(defn re-frame-fixture
  [f]
  (re-frame.test/with-temp-re-frame-state
    (re-frame.test/run-test-sync
     (re-frame/reg-event-db ::test-db-init (constantly test-db))
     (re-frame/dispatch-sync [::test-db-init])
     (re-frame/dispatch-sync [::vcs.handlers/init (uuid ::data/vims) data/deltas])
     (f))))

;;
;; * Fixtures
;;

(use-fixtures :each
  system.fixture/system
  (system.fixture/with-user data/user)
  system.fixture/session
  system.fixture/vims
  system.fixture/deltas
  re-frame-fixture)

;;
;; * Helpers
;;

(defn get-sync [db-sub] (get-in @db-sub (vcs.sync.db/path (uuid ::data/vims))))

;;
;; * Deltas by branch-uid
;;

(deftest deltas-by-branch-uid-test
  (let [status-key    (uuid)
        handler-event [::vcs.sync.handlers/start (uuid ::data/vims) status-key]
        status-sub    (re-frame/subscribe [::frontend.remotes.fx/status :backend status-key])
        db-sub        (re-frame/subscribe [::db/db])]
    (re-frame/dispatch handler-event)
    (do (is (= ::frontend.remotes.fx/success @status-sub))
        (is (= data/deltas-by-branch-uid (-> db-sub get-sync ::vcs.sync.db/delta-by-branch-uid))))))
