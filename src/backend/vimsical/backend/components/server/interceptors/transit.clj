(ns vimsical.backend.components.server.interceptors.transit
  (:require
   [io.pedestal.interceptor :as interceptor]
   [vimsical.common.util.transit :as transit]))
;;
;; * Pedestal interceptor
;;

(defn new-body-interceptor
  ([] (new-body-interceptor nil))
  ([{:keys [reader reader-options writer writer-options]
     :or   {reader transit/default-reader reader-options transit/default-reader-options
            writer transit/default-writer writer-options transit/default-writer-options}}]
   ;; Difference in case between Ring and Pedestal??
   (let [writer-options' (assoc writer-options :content-type-key "Content-Type")]
     (interceptor/interceptor
      {:name  ::transit-body-interceptor
       :enter (fn [{:keys [request] :as context}]
                (cond-> context
                  (some? request) (update :request transit/decode-transit-request reader reader-options)))
       :leave (fn [{:keys [response] :as context}]
                (cond-> context
                  (some? response) (update :response transit/encode-transit-response writer writer-options')))}))))

(def body (new-body-interceptor))
