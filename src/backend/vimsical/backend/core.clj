(ns vimsical.backend.core
  (:require
   [com.stuartsierra.component :as cp]
   [vimsical.backend.system :as system])
  (:gen-class))

(defn add-shutdown-hook!
  [system]
  (.addShutdownHook
   (Runtime/getRuntime)
   (Thread. #(some-> system cp/stop))))

(defn -main [& _]
  (-> (system/new-system)
      (cp/start)
      (add-shutdown-hook!)))
