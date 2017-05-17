(ns vimsical.backend.components.service
  (:require
   [clojure.spec :as s]
   [io.pedestal.http :as http]
   [io.pedestal.http.route :as route]
   [vimsical.backend.handler]          ; Side-effects
   [vimsical.backend.handlers.mutlifn :as events.multifn]
   [vimsical.common.env :as env]
   [vimsical.common.util.transit :as transit]))

;;
;; * Handler
;;

(s/def ::context map?)
(s/def ::request map?)
(s/def ::response map?)

(s/fdef event-context-handler
        :args (s/cat :context ::context)
        :ret ::context)

(defn event-context-handler
  [context]
  (let [event    (some-> context :request :body)
        result   (events.multifn/handle context event)
        response {:status 200 :body result}]
    (assoc context :response response)))

(def ^:private event-interceptor
  {:name :event :enter event-context-handler})

;;
;; * Routes
;;

(def routes
  (route/expand-routes
   #{["/events" :post event-interceptor :route-name :events]}))

(def url-for
  (route/url-for-routes routes))

;;
;; * Interceptors
;;

(def default-interceptors
  [(transit/new-transit-body-interceptor)])

;;
;; * Service map
;;

(def service-map
  {:env                        (env/env)
   ::http/port                 (env/required :http-port ::env/int)
   ::http/type                 :immutant
   ::http/routes               routes
   ::http/join?                false
   ::http/allowed-origins      (constantly true)
   ::http/default-interceptors default-interceptors})
