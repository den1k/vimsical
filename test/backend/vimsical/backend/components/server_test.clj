(ns vimsical.backend.components.server-test
  (:require
   [clojure.spec.alpha :as s]
   [clojure.test :refer [deftest is use-fixtures]]
   [orchestra.spec.test :as st]
   [vimsical.common.util.transit :as transit]
   [vimsical.backend.components.server :as sut]
   [vimsical.backend.components.server.fixture :refer [*server* *service-fn* server]]
   [vimsical.backend.components.server.test :as test]
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
  (let [expect {:status 200 :headers {"Content-Type" "application/transit+json"} :body {:status :ok}}
        actual (-> (test/response-for *service-fn* [::status.queries/status])
                   (select-headers "Content-Type"))]
    (is (= expect actual))))
