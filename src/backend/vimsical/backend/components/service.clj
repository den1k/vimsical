(ns vimsical.backend.components.service
  (:require
   [io.pedestal.http :as http]
   [io.pedestal.http.immutant :as immutant]
   [io.pedestal.http.route :as route]
   [vimsical.backend.components.server.interceptors.event :as interceptors.event]
   [vimsical.backend.components.server.interceptors.event-auth :as interceptors.event-auth]
   [vimsical.backend.components.server.interceptors.session :as interceptors.session]
   [vimsical.backend.components.server.interceptors.transit :as interceptors.transit]
   [vimsical.common.env :as env]))

;;
;; * Routes
;;

(def routes
  #{["/events"
     :post [interceptors.event-auth/event-auth
            interceptors.event/handle-event]
     :route-name :events]})

(def url-for
  (route/url-for-routes (route/expand-routes routes)))

;;
;; * Interceptors
;;

(def default-interceptors
  [interceptors.transit/body
   interceptors.session/session])

;;
;; * Service map
;;

(def service-map
  {:env                        (env/env)
   ::http/host                 (env/optional :http-host ::env/string)
   ::http/port                 (env/required :http-port ::env/int)
   ::http/type                 :immutant
   ::http/routes               routes
   ::http/join?                false
   ::http/allowed-origins      {:creds true :allowed-origins (constantly true)}
   ::http/resource-path        "/public"
   ::http/default-interceptors default-interceptors
   ::http/start-fn             immutant/start
   ::http/stop-fn              immutant/stop})
