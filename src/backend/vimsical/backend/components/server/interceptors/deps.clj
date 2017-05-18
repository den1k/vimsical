(ns vimsical.backend.components.server.interceptors.deps
  (:require
   [com.stuartsierra.component :as cp]
   [io.pedestal.interceptor :as interceptor]))

;;
;; * Component helpers
;;

(defn- component-depencies-map
  [component]
  (reduce-kv
   (fn [m k _]
     (assoc m k (get component k)))
   {} (cp/dependencies component)))

;;
;; * Interceptor
;;

(defn new-context-dependencies-injector
  "Create a before interceptor that will merge the dependencies of `component`
  into the context map."
  [component]
  (let [dependencies-map (component-depencies-map component)]
    (interceptor/interceptor
     {:name  ::deps
      :enter (fn [context] (merge context dependencies-map))})))
