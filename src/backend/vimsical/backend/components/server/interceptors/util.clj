(ns vimsical.backend.components.server.interceptors.util
  (:require
   [io.pedestal.http :as http]))

(defn prepend [interceptors new]
  (into (cond (vector? new) new (map? new) [new]) (vec interceptors)))

(defn prepend-interceptors [service-map new]
  (update service-map ::http/interceptors prepend new))

(defn prepend-default-interceptors
  "Update the `::http/default-injectors` of `service-map` by prepending `new`."
  [service-map new]
  (update service-map ::http/default-interceptors prepend new))
