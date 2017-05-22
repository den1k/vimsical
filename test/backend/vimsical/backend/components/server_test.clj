(ns vimsical.backend.components.server-test
  (:require
   [clojure.spec :as s]
   [clojure.test :refer [deftest is use-fixtures]]
   [io.pedestal.test :refer [response-for]]
   [orchestra.spec.test :as st]
   [vimsical.backend.components.server :as sut]
   [vimsical.backend.components.server.test :as test]
   [vimsical.backend.components.server.fixture :refer [*server* *service-fn* server]]
   [vimsical.backend.components.service :as service]
   [vimsical.common.util.transit :as transit]
   [vimsical.remotes.backend.status.queries :as status.queries]))

(st/instrument)

;;
;; * Helpers
;;

(defn select-headers [resp & keys]
  (update resp :headers select-keys keys))

;;
;; * Fixtures
;;

(use-fixtures :each server)

;;
;; * Tests
;;

(deftest server-test (is (s/valid? ::sut/server *server*)))

(deftest service-fn-test (is (ifn? *service-fn*)))

(deftest status-test
  (let [expect {:status 200 :headers {"Content-Type" "text/plain"} :body "ok"}
        actual (-> (test/response-for *service-fn* [::status.queries/status])
                   (select-headers "Content-Type"))]
    (is (= expect actual))))
