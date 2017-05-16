(ns vimsical.backend.components.server
  (:require
   [clojure.spec :as s]
   [com.stuartsierra.component :as component]
   [io.pedestal.http :as http]
   [io.pedestal.interceptor :as interceptor]))

;;
;; * Interceptors helpers
;;

(defn prepend [interceptors new]
  (into interceptors (cond (vector? new) new (map? new) [new])))

(defn- default-interceptors
  "Create the default interceptors from the `service-map` and prepend the
  interceptors found in `:http/default-interceptors`"
  [{::http/keys [default-interceptors] :as service-map}]
  (-> service-map
      http/default-interceptors
      (update ::http/interceptors prepend default-interceptors)))

;;
;; * Dependencies injection
;;

(defn- new-context-dependencies-interceptor
  "Create a before interceptor that will merge the dependencies of `component`
  into the context map."
  [component]
  (let [deps (component/dependencies component)]
    (interceptor/interceptor
     {:name  ::insert-context
      :enter (fn [context] (merge context deps))})))

(defn- prepend-default-interceptors
  "Update the `::http/default-injectors` of `service-map` by prepending
  `interceptors`."
  [service-map interceptors]
  (update service-map ::http/default-injectors prepend interceptors))

;;
;; * Component helpers
;;

(defn- start-service
  [service-map component]
  (let [context-dependencies-interceptor (new-context-dependencies-interceptor component)]
    (-> service-map
        default-interceptors
        (prepend-default-interceptors context-dependencies-interceptor)
        http/create-server
        http/start)))

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
  [{::keys [service-map]}]
  (->Server service-map nil))

(s/def ::server (fn [x] (and x (instance? Server x))))
