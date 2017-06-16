(ns vimsical.integration.remotes.backend.auth.commands-test
  (:require
   [clojure.spec.alpha :as s]
   [clojure.test :refer [deftest is use-fixtures testing]]
   [com.stuartsierra.mapgraph :as mg]
   [day8.re-frame.test :as re-frame.test]
   [orchestra.spec.test :as st]
   [re-frame.core :as re-frame]
   [vimsical.backend.system.fixture :as system.fixture]
   [vimsical.common.test :refer [uuid]]
   [vimsical.frontend.auth.handlers :as frontend.auth.handlers]
   [vimsical.frontend.db :as db]
   [vimsical.frontend.remotes.fx :as frontend.remotes.fx]
   [vimsical.queries.user :as queries.user]
   [vimsical.backend.handlers.auth.commands :refer [create-invite!]]
   [vimsical.user :as user]))

(st/instrument)

;;
;; * Fixtures
;;

(defn re-frame-fixture
  [f]
  (re-frame.test/with-temp-re-frame-state
    (re-frame.test/run-test-sync
     (re-frame/dispatch [::db/init])
     (f))))

(use-fixtures :each
  system.fixture/system
  re-frame-fixture)

;;
;; * Tests
;;

;; Should use the :q subscription but can't get it to work
(defn get-app-user
  [db-sub]
  (:app/user
   (mg/pull
    @db-sub
    [{[:app/user '_] queries.user/pull-query}])))

(deftest invite-signup-test
  (testing "Signup with token"
    (let [token       (create-invite! (:datomic system.fixture/*system*) "foo" "bar")
          status-key  (uuid)
          signup-user {:db/uid (uuid :signup-user)
                       ::user/first-name "foo"
                       ::user/last-name  "bar"
                       ::user/email      "foo@bar.com"
                       ::user/password   "foobar"}
          status-sub  (re-frame/subscribe [::frontend.remotes.fx/status :backend status-key])
          db-sub      (re-frame/subscribe [::db/db])]
      (re-frame/dispatch [::frontend.auth.handlers/invite-signup token signup-user status-key])
      (is (= ::frontend.remotes.fx/success @status-sub))
      (let [app-user (get-app-user db-sub)]
        (is (s/valid? ::user/user app-user))
        (is (= "foo" (::user/first-name app-user)))
        (is (= (uuid :signup-user) (:db/uid app-user))))
      (testing "Token expired"
        (re-frame/dispatch [::frontend.auth.handlers/invite-signup token signup-user status-key])
        (is (s/valid? ::frontend.remotes.fx/error @status-sub))))))

(deftest signup-test
  (let [status-key    (uuid)
        signup-user {:db/uid           (uuid :signup-user)
                     ::user/first-name "foo"
                     ::user/last-name  "bar"
                     ::user/email      "foo@bar.com"
                     ::user/password   "foobar"}
        status-sub    (re-frame/subscribe [::frontend.remotes.fx/status :backend status-key])
        db-sub        (re-frame/subscribe [::db/db])]
    (re-frame/dispatch [::frontend.auth.handlers/signup signup-user status-key])
    (is (= ::frontend.remotes.fx/success @status-sub))
    (let [app-user (get-app-user db-sub)]
      (is (s/valid? ::user/user app-user))
      (is (= "foo" (::user/first-name app-user)))
      (is (= (uuid :signup-user) (:db/uid app-user))))))

(deftest login-test
  (signup-test)
  (re-frame/dispatch [::db/init])
  (let [status-key   (uuid)
        login-user   {:db/uid (uuid :temp-user) ::user/email "foo@bar.com" ::user/password "foobar"}
        status-sub   (re-frame/subscribe [::frontend.remotes.fx/status :backend status-key])
        db-sub       (re-frame/subscribe [::db/db])]
    (re-frame/dispatch [::frontend.auth.handlers/login login-user status-key])
    (is (= ::frontend.remotes.fx/success @status-sub))
    (let [app-user (get-app-user db-sub)]
      (is (s/valid? ::user/user app-user))
      (is (= "foo" (::user/first-name app-user)))
      (is (= (uuid :signup-user) (:db/uid app-user))))))
