(ns vimsical.backend.core
  "TODO
  - Implement handlers
  - Async session
  - Have a strategy to measure latency and throughput
  - Add an nRepl server component for prod
  - Silence logs in test
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
