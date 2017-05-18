(ns vimsical.backend.components.service
  (:require
   [clojure.spec :as s]
   [io.pedestal.http :as http]
   [io.pedestal.http.route :as route]
   [vimsical.backend.components.server.interceptors.session :as interceptors.session]
   [vimsical.backend.components.server.interceptors.transit :as interceptors.transit]
   vimsical.backend.handler
   [vimsical.backend.handlers.multi :as events.multifn]
   [vimsical.common.env :as env]))

;;
;; * Handler
;;

(s/def ::context map?)
(s/def ::request map?)
(s/def ::response map?)

(s/fdef context-event-handler
        :args (s/cat :context ::context)
        :ret ::context)

(defn context-event-handler
  [context]
  (letfn [(ensure-response [context]
            (-> context
                (update-in [:response :status] (fnil identity 200))
                (update-in [:response :body] (fnil identity {}))))]
    (let [event    (some-> context :request :body)
          context' (events.multifn/handle context event)]
      (ensure-response context'))))

(def ^:private event-interceptor
  {:name  :event
   :enter context-event-handler})

;;
;; * Routes
;;

(def routes
  #{["/events" :post [event-interceptor] :route-name :events]})

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
   ::http/port                 (env/required :http-port ::env/int)
   ::http/type                 :immutant
   ::http/routes               routes
   ::http/join?                false
   ::http/allowed-origins      (constantly true)
   ::http/default-interceptors default-interceptors})
