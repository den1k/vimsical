(ns vimsical.backend.handlers.auth.commands-test
  (:require
   [clojure.test :refer [deftest is use-fixtures]]
   [orchestra.spec.test :as st]
   [vimsical.backend.components.server.test :as server.test]
   [vimsical.backend.system.fixture :refer [system]]
   [vimsical.common.test :refer [uuid]]
   [vimsical.remotes.backend.auth.commands :as auth.commands]
   [vimsical.user :as user]))

(st/instrument)

(use-fixtures :each system)

(deftest signup-test
  (let [signup-user             {:db/uid           (uuid)
                                 ::user/first-name "Foo"
                                 ::user/last-name  "Bar"
                                 ::user/email      "foo@bar.com"
                                 ::user/password   "foobar"}
        expect                  (dissoc signup-user ::user/password)
        {actual :body :as resp} (server.test/response-for [::auth.commands/signup signup-user])]
    (is (= expect actual))
    (is (server.test/status-ok? resp))
    (is (server.test/auth-session? resp))))

(deftest login-test
  (do (signup-test)
      (let [login-user {:db/uid         (uuid)
                        ::user/email    "foo@bar.com"
                        ::user/password "foobar"}
            actual     (server.test/response-for [::auth.commands/login login-user])]
        (is (server.test/status-ok? actual))
        (is (server.test/auth-session? actual)))))

(deftest logout-test
  (do (signup-test)
      (let [actual (server.test/response-for [::auth.commands/logout])]
        (is (server.test/status-ok? actual))
        (is (not (server.test/auth-session? actual))))))
