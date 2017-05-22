(ns vimsical.backend.components.server.interceptors.event
  (:require
   [io.pedestal.interceptor :as interceptor]
   [io.pedestal.interceptor.chain :as interceptor.chain]
   [ring.util.response :as response]
   vimsical.backend.handler             ; require for side-effects
   [vimsical.backend.handlers.multi :as events.multi]
   [clojure.spec :as s]))

;;
;; * Event helpers
;;

(defn- context-lift-request
  [context]
  (assoc context :event (some-> context :request :body)))

(defn- context-lift-result
  [result request-context]
  (letfn [(context?  [result] (some? (:request result)))
          (response? [result] (some? (:body result)))
          (ensure-response [context]
            (update context :response (fnil identity (response/response nil))))]
    (interceptor.chain/terminate
     (ensure-response
      (cond
        (context? result)  result
        (response? result) (assoc request-context :response result)
        :else              (assoc request-context :response (response/response result)))))))

;;
;; * Handler
;;

(defn handle-event [{:keys [event] :as context}]
  (events.multi/handle-event context event))

(s/fdef enter
        :args (s/cat :context ::events.multi/context-in)
        :ret ::events.multi/context-out)

(defn enter
  [context]
  (-> context
      (context-lift-request)
      (handle-event)
      (context-lift-result context)))

;;
;; * Interceptor
;;

(def event
  (interceptor/interceptor
   {:name ::event :enter enter}))
