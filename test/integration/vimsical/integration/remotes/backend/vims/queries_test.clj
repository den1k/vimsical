(ns vimsical.integration.remotes.backend.vims.queries-test
  (:require
   [clojure.spec.alpha :as s]
   [clojure.test :refer [deftest is use-fixtures]]
   [com.stuartsierra.mapgraph :as mg]
   [day8.re-frame.test :as re-frame.test]
   [orchestra.spec.test :as st]
   [re-frame.core :as re-frame]
   [vimsical.backend.data :as data]
   [vimsical.backend.system.fixture :as system.fixture]
   [vimsical.common.test :refer [uuid]]
   [vimsical.frontend.db :as db]
   [vimsical.frontend.remotes.fx :as frontend.remotes.fx]
   [vimsical.frontend.vims.handlers :as frontend.vims.handlers]
   [vimsical.queries.vims :as queries.vims]
   [vimsical.vcs.core :as vcs]
   [vimsical.vims :as vims]))

(st/instrument)

;;
;; * Re-frame
;;

(def test-db (db/new-db {}))

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
  system.fixture/deltas
  re-frame-fixture)

;;
;; * Helpers
;;

(defn get-vims
  [db-sub]
  (mg/pull @db-sub queries.vims/frontend-pull-query [:db/uid (uuid ::data/vims)]))

;;
;; * Vims
;;

(deftest vims-test
  (let [status-key    (uuid)
        handler-event [::frontend.vims.handlers/vims (uuid ::data/vims) status-key]
        status-sub    (re-frame/subscribe [::frontend.remotes.fx/status :backend status-key])
        db-sub        (re-frame/subscribe [::db/db])]
    (re-frame/dispatch handler-event)
    (is (= ::frontend.remotes.fx/success @status-sub))
    (is (s/valid? ::vims/vims (-> db-sub get-vims)))))

;;
;; * Deltas
;;

(deftest deltas-test
  (vims-test)
  (let [status-key    (uuid)
        handler-event [::frontend.vims.handlers/deltas uuid (uuid ::data/vims) status-key]
        status-sub    (re-frame/subscribe [::frontend.remotes.fx/status :backend status-key])
        db-sub        (re-frame/subscribe [::db/db])]
    (re-frame/dispatch handler-event)
    (is (= ::frontend.remotes.fx/success @status-sub))
    (is (s/valid? ::vcs/vcs (-> db-sub get-vims ::vims/vcs)))
    (let [last-delta-uid (-> data/deltas last :uid)
          vcs-deltas     (-> db-sub get-vims ::vims/vcs (vcs/deltas last-delta-uid))]
      (is (= (count data/deltas) (count vcs-deltas))))))
