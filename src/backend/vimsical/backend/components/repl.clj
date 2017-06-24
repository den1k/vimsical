(ns vimsical.backend.components.repl
  (:require
   [com.stuartsierra.component :as cp]
   [clojure.tools.nrepl.server :as server]))

(defrecord nRepl [port server]
  cp/Lifecycle
  (start [this]
    (cond-> this
      (some? port) (assoc :server (server/start-server :port port))))
  (stop [this]
    (cond-> this
      (some? server) (update :server server/stop-server))))

(defn ->repl
  [port]
  (nRepl. port nil))
