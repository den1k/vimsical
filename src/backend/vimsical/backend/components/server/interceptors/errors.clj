(ns vimsical.backend.components.server.interceptors.errors
  (:require
   [clojure.pprint :as pprint]
   [io.pedestal.interceptor :as interceptor]
   [ring.util.response :as response]
   [vimsical.backend.util.log :as log]
   [vimsical.common.env :as env]))

(def debug? (not= :prod (env/env)))

(defn- error-debug
  [context exception]
  (log/error
   {:msg       "Error interceptor caught an exception; Forwarding it as the response."
    :exception exception})
  (assoc context :response
         (response/status
          (when debug?
            (response/response
             (with-out-str (println "Error processing request!")
               (println "Exception:\n")
               (pprint/pprint exception)
               (println "\nContext:\n")
               (pprint/pprint context))))
          500)))

(def debug
  (interceptor/interceptor
   {:name ::debug :error error-debug}))
