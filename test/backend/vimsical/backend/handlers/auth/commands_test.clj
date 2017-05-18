(ns vimsical.backend.handlers.auth.commands-test
  (:require
   [clojure.test :refer [deftest is testing use-fixtures]]
   [io.pedestal.test :refer [response-for]]
   [orchestra.spec.test :as st]
   [vimsical.backend.components.datomic :as datomic]
   [vimsical.backend.components.server.interceptors.session :as session]
   [vimsical.backend.components.service :as service]
   [vimsical.backend.handlers.multi :refer [handle]]
   [vimsical.backend.system.fixture :refer [*service-fn* *system* system]]
   [vimsical.backend.util.auth :as util.auth]
   [vimsical.common.test :refer [uuid]]
   [vimsical.common.util.transit :as transit]
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
        actual        (response-for
                       *service-fn*
                       :post (service/url-for :events)
                       :headers {"Content-Type" "application/transit+json"}
                       :body     (transit/write-transit [::auth.commands/register! register-user]))
        expect        {:status 200}]
    (is (= expect actual))))
