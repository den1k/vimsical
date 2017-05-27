(ns vimsical.backend.components.server
  "Order of interceptors (front to back):

  - context-dependencies-interceptor
  - ::http/default-interceptors from service map
  - pedestal default-interceptors
  "

  (:require
   [clojure.spec :as s]
   [com.stuartsierra.component :as component]
   [io.pedestal.http :as http]
   [vimsical.backend.components.server.interceptors.deps :as interceptors.deps]
   [vimsical.backend.components.server.interceptors.util :as interceptors.util]))

;;
;; * Interceptors helpers
;;

(defn- add-default-interceptors
  "Create the default interceptors from the `service-map` and prepend the
  interceptors found in `:http/default-interceptors`"
  [{::http/keys [default-interceptors] :as service-map}]
  (-> service-map
      (http/default-interceptors)
      (http/dev-interceptors)
      (interceptors.util/prepend-interceptors default-interceptors)))

;;
;; * Component helpers
;;

(defn- start-service
  [service-map component]
  (let [context-dependencies-interceptor (interceptors.deps/new-context-dependencies-injector component)
        new-interceptors                 [context-dependencies-interceptor]]
    (-> service-map
        (add-default-interceptors)
        (interceptors.util/prepend-interceptors new-interceptors)
        (http/create-server)
        (http/start))))

;;
;; * Component
;;

(defrecord Server [service-map service]
  component/Lifecycle
  (start [this]
    (cond-> this
      (nil? service) (assoc :service (start-service service-map this))))
  (stop [this]
    (update this :service #(some-> % http/stop))))

;;
;; * Api
;;

(defn ->server
  ([] (->server nil))
  ([{::keys [service-map]}]
   (->Server service-map nil)))

(s/def ::server (fn [x] (and x (instance? Server x))))
