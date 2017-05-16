(ns vimsical.backend.components.server-test
  (:require
   [clojure.spec :as s]
   [clojure.test :refer [deftest is use-fixtures]]
   [io.pedestal.test :refer [response-for]]
   [vimsical.backend.components.server :as sut]
   [vimsical.backend.components.server-fixture :refer [*server* *service-fn* server]]
   [vimsical.backend.components.service :as service]
   [vimsical.remotes.backend :as backend]
   [vimsical.common.util.transit :as transit]))

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
  (let [expect {:status  200
                :headers {"Content-Type" "application/transit+json"}
                :body    (transit/write-transit [::backend/status-response :ok])}
        actual (-> (response-for
                    *service-fn*
                    :post (service/url-for :events)
                    :headers {"Content-Type" "application/transit+json"}
                    :body    (transit/write-transit [::backend/status]))
                   (select-headers "Content-Type"))]
    (is (= expect actual))))
