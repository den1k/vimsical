(ns vimsical.backend.core
  "TODO
  - Have a strategy to measure latency and throughput
  - Add an nRepl server component for prod
  - Logging"
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
