(ns vimsical.backend.components.server.interceptors.event
  (:require
   [io.pedestal.interceptor :as interceptor]
   [io.pedestal.interceptor.chain :as interceptor.chain]
   [ring.util.response :as response]
   vimsical.backend.handler             ; require for side-effects
   [vimsical.backend.handlers.multi :as events.multi]
   [clojure.spec :as s]
   [vimsical.backend.util.log :as log]))

;;
;; * Event helpers
;;

(defn- request-context->event-context
  [context]
  (assoc context :event (some-> context :request :body)))

(defn- result-context->response-context
  [result request-context]
  (letfn [(context?  [result] (some? (:request result)))
          (response? [result] (some? (:body result)))
          (throwable? [ex] (and ex (instance? Throwable ex)))
          (throwable->error [t]
            (-> t Throwable->map (select-keys [:msg])))
          (valid-response [{:keys [status headers body] :as response}]
            (cond-> response
              (nil? status)  (assoc :status 200)
              (nil? headers) (assoc :headers {})
              (nil? body)    (assoc :body {})))
          (result-response [result] (response/response result))
          (error-response [ex]
            {:status 500 :body (throwable->error ex)})
          (lift-maybe [result]
            (cond
              (context? result)   result
              ;; When returning a response assoc it into the context
              (response? result)  (assoc request-context :response result)
              ;; If we caught and exception we'll return it in an error response
              (throwable? result) (assoc request-context :response (error-response result))
              ;; Else we have a value that we should use as a response body
              :else               (assoc request-context :response (result-response result))))
          (valid-context-response [context]
            (update context :response valid-response))]
    (-> result
        lift-maybe
        valid-context-response
        interceptor.chain/terminate)))

;;
;; * Handler
;;

(defn handle-event-context
  [{:keys [event] :as context}]
  (try
    (events.multi/handle-event context event)
    (catch Throwable t
      (doto t (log/error)))))

(s/fdef enter
        :args (s/cat :context ::events.multi/context-in)
        :ret ::events.multi/context-out)

;; Don't know what's going on here, the fdef never checks during
;; instrumentation, even s/assert doesn't seem to throw, we'll just rely on
;; s/assert* for now

;; TODO Only check context specs during dev

(defn enter
  [context]
  (s/assert* ::events.multi/context-in context)
  (let [context-out (-> context
                        (request-context->event-context)
                        (handle-event-context)
                        (result-context->response-context context))]
    (s/assert* ::events.multi/context-out context-out)
    context-out))

;;
;; * Interceptor
;;

(def event
  (interceptor/interceptor
   {:name ::event :enter enter}))
