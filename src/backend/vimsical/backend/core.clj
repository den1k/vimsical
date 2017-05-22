(ns vimsical.backend.core
  "TODO
  - Session interceptor (service-map as component, inject session-store)
  - Add a query api to the datomic component so we don't need to destructure in handlers
  - Implement handlers
  - Async session
  - Add an event-based multimethod for authorization
  - Merge session, or user-id in context
  - Have a strategy to measure latency and throughput
  - Add an nRepl server component for prod
  - Logging config for prod "
  (:require
   [com.stuartsierra.component :as cp]
   [vimsical.backend.system :as system]))

(defn add-shutdown-hook!
  [system]
  (.addShutdownHook
   (Runtime/getRuntime)
   (Thread. #(some-> system cp/stop))))

(defn -main [& _]
  (-> (system/new-system)
      (cp/start)
      (add-shutdown-hook!)))
