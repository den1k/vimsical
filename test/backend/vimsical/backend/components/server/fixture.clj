(ns vimsical.backend.components.server-fixture
  (:require
   [io.pedestal.http :as http]
   [com.stuartsierra.component :as cp]
   [vimsical.backend.components.server :as server]
   [vimsical.backend.components.service :as service]))

;;
;; * State
;;

(def ^:dynamic *server* nil)
(def ^:dynamic *service-fn* nil)

;;
;; * Helpers
;;

(defn- new-server
  []
  (server/->server
   {::server/service-map service/service-map}))

;;
;; * Fixtures
;;

(defn server
  [f]
  (binding [*server* (cp/start (new-server))]
    (binding [*service-fn* (get-in *server* [:service ::http/service-fn])]
      (try
        (f)
        (finally
          (cp/stop *server*))))))
