(ns vimsical.backend.core
  (:require
   [com.stuartsierra.component :as cp]
   [vimsical.backend.system :as system])
  (:gen-class))

;; Statically define the system so that the repl can access it at runtime

(defonce system nil)

(defn set-shutdown-hook! [sys]
  (.addShutdownHook
   (Runtime/getRuntime)
   (Thread. #(some-> sys cp/stop))))

(defn set-system! [sys]
  (alter-var-root #'system (constantly sys)))

(defn -main [& _]
  (doto (cp/start
         (system/new-system))
    (set-system!)
    (set-shutdown-hook!)))
