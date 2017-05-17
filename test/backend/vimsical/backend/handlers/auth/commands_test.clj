(ns vimsical.backend.handlers.auth.commands-test
  (:require
   [clojure.test :refer [deftest is testing use-fixtures]]
   [vimsical.backend.system.fixture :refer [system *system* *service-fn*]]
   [orchestra.spec.test :as st]
   [io.pedestal.test :refer [response-for]]
   [vimsical.common.test :refer [uuid]]
   [vimsical.backend.components.service :as service]
   [vimsical.backend.components.datomic :as datomic]
   [vimsical.backend.components.server.interceptors.session :as session]
   [vimsical.backend.handlers.mutlifn :refer [handle]]
   [vimsical.backend.util.auth :as util.auth]
   [vimsical.common.util.transit :as transit]
   [vimsical.remotes.backend.auth.commands :as commands]
   [vimsical.user :as user]
   [vimsical.remotes.backend.auth.commands :as auth.commands]))

(st/instrument)

(use-fixtures :each system)

(deftest register-test
  (let [register-user {:db/id            (uuid)
                       ::user/first-name "Foo"
                       ::user/last-name  "Bar"
                       ::user/email      "foo@bar.com"
                       ::user/password   "foobar"}
        actual        (response-for
                       *service-fn*
                       :post (service/url-for :events)
                       :headers {"Content-Type" "application/transit+json"}
                       :body     (transit/write-transit [::auth.commands/register! register-user]))
        expect        {:status 200}]
    (is (= expect actual))))
