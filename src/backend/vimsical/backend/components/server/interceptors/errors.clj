(ns vimsical.backend.components.server.interceptors.errors
  (:require
   [ring.util.response :as response]
   [clojure.pprint :as pprint]
   [io.pedestal.interceptor :as interceptor]
   [vimsical.backend.util.log :as log]))

(defn- error-debug
  [context exception]
  (log/error
   {:msg       "Error interceptor caught an exception; Forwarding it as the response."
    :exception exception})
  (assoc context :response
         (-> (response/response
              (with-out-str (println "Error processing request!")
                (println "Exception:\n")
                (pprint/pprint exception)
                (println "\nContext:\n")
                (pprint/pprint context)))
             (response/status 500))))

(def debug
  (interceptor/interceptor
   {:name ::debug :error error-debug}))
