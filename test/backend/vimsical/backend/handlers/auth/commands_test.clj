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

(deftest register-test
  (let [register-user {:db/uid           (uuid)
                       ::user/first-name "Foo"
                       ::user/last-name  "Bar"
                       ::user/email      "foo@bar.com"
                       ::user/password   "foobar"}
        actual        (server.test/response-for [::auth.commands/register register-user])]
    (is (server.test/status-ok? actual))
    (is (server.test/active-session? actual))))

(deftest login-test
  (do (register-test)
      (let [login-user {::user/email    "foo@bar.com"
                        ::user/password "foobar"}
            actual     (server.test/response-for [::auth.commands/login login-user])]
        (is (server.test/status-ok? actual))
        (is (server.test/active-session? actual)))))
