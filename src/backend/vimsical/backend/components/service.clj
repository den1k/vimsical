(ns vimsical.backend.components.service
  (:require
   [io.pedestal.http :as http]
   [io.pedestal.http.ring-middlewares :as middlewares]
   [io.pedestal.http.route :as route]
   [io.pedestal.http.cors :as cors]
   [vimsical.backend.components.server.interceptors.index :as interceptors.index]
   [vimsical.backend.components.server.interceptors.errors :as interceptors.errors]
   [vimsical.backend.components.server.interceptors.event :as interceptors.event]
   [vimsical.backend.components.server.interceptors.event-auth :as interceptors.event-auth]
   [vimsical.backend.components.server.interceptors.session :as interceptors.session]
   [vimsical.backend.components.server.interceptors.transit :as interceptors.transit]
   [vimsical.common.env :as env]
   [clojure.java.io :as io]))

;;
;; * Routes
;;

(def routes
  (route/expand-routes
   #{["/events"
      :post       [interceptors.event-auth/event-auth interceptors.event/handle-event]
      :route-name :events]}))

(def url-for
  (route/url-for-routes routes))

;;
;; * Interceptors
;;

(def default-interceptors
  [http/log-request
   interceptors.errors/debug
   (cors/allow-origin {:creds true :allowed-origins (constantly true)})
   interceptors.index/index
   http/not-found
   route/query-params
   (route/method-param :_method)
   (middlewares/content-type)
   (middlewares/resource "/public")
   middlewares/cookies
   interceptors.session/session
   interceptors.transit/body
   (route/router routes)])

;;
;; * Service map
;;

(def service-map
  {:env                (env/env)
   ::http/host         (env/optional :http-host ::env/string)
   ::http/port         (env/required :http-port ::env/int)
   ::http/type         :immutant
   ::http/join?        false
   ::http/interceptors default-interceptors
   ::http/start-fn     http/start
   ::http/stop-fn      http/stop})
