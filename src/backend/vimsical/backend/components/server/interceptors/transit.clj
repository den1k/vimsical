(ns vimsical.backend.components.server.interceptors.transit
  (:require
   [vimsical.backend.util.log :as log]
   [io.pedestal.interceptor :as interceptor]
   [vimsical.common.util.transit :as transit]))

;;
;; * Internal
;;

(defn- new-enter
  [reader reader-options]
  (fn enter [context]
    (update context :request transit/decode-transit-request reader reader-options)))

(defn- new-leave
  [writer writer-options]
  (fn leave [context]
    (update context :response transit/encode-transit-response writer writer-options)))

;;
;; * Interceptor Factory
;;

(defn new-body-interceptor
  ([] (new-body-interceptor nil))
  ([{:keys [reader reader-options
            writer writer-options]
     :or   {reader         transit/default-reader
            reader-options transit/default-reader-options
            writer         transit/default-writer
            writer-options transit/default-writer-options}}]
   ;; Difference in case between Ring and Pedestal??
   (let [writer-options' (assoc writer-options :content-type-key "Content-Type")]
     (interceptor/interceptor
      {:name  ::transit-body-interceptor
       :enter (new-enter reader reader-options)
       :leave (new-leave writer writer-options')}))))

;;
;; * Default interceptor
;;

(def body (new-body-interceptor))
