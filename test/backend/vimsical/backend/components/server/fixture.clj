(ns vimsical.backend.components.server.fixture
  (:require
   [io.pedestal.http :as http]
   [io.pedestal.interceptor :as interceptor]
   [com.stuartsierra.component :as cp]
   [vimsical.backend.components.server :as server]
   [vimsical.backend.components.server.interceptors.util :as interceptors.util]
   [vimsical.backend.components.service :as service]
   [ring.middleware.session.memory :as middleware.session.memory]))

;;
;; * State
;;

(def ^:dynamic *server* nil)
(def ^:dynamic *service-fn* nil)

;;
;; * Mock dependencies
;;

(def mock-session-store-interceptor
  (interceptor/interceptor
   {:name ::mock-session-store
    :enter (fn [context]
             (assoc context :session-store (middleware.session.memory/memory-store)))}))

;;
;; * Helpers
;;

(def mocked-service-map
  (-> service/service-map
      (interceptors.util/prepend-default-interceptors mock-session-store-interceptor)))

(defn- new-mocked-server
  []
  (server/->server {::server/service-map mocked-service-map}))

;;
;; * Fixtures
;;

(defn server
  [f]
  (binding [*server* (cp/start (new-mocked-server))]
    (binding [*service-fn* (get-in *server* [:service ::http/service-fn])]
      (try
        (f)
        (finally
          (cp/stop *server*))))))
