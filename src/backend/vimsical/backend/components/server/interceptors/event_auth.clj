(ns vimsical.backend.components.server.interceptors.event-auth
  "Authentication logic for event handlers.

  Event handlers that require user authentication can implement an
  `authenticated-event?` method that returns true for their event id.

  If the session does not contain a `::user/uid` the context will be terminated
  and the appropriate HTTP status will be returned.

  If found, the `::user/uid` will be assoc-ed into the context."
  (:require
   [io.pedestal.interceptor :as interceptor]
   [io.pedestal.interceptor.chain :as interceptor.chain]
   [ring.util.response :as response]
   [vimsical.remotes.event :as event]
   [vimsical.user :as user]))

;;
;; * Event predicate
;;

(defmulti  authenticated-event? event/dispatch)
(defmethod authenticated-event? :default [_] false)

;;
;; * Internal
;;

;; XXX Decomplect request-event-lifting in event handler so that we can insert
;; this interceptor in between the event lifting and handling

(defn- authenticate?
  [context]
  (some-> context :request :body authenticated-event?))

(defn- context->user-uid
  [context]
  (some-> context :request :session ::user/uid))

(defn- terminate
  [context]
  (let [error-response (response/status {} 401)]
    (interceptor.chain/terminate
     (assoc context :response error-response))))

(defn- enter
  [context]
  (if-not (authenticate? context)
    context
    (if-some [user-uid (context->user-uid context)]
      (assoc context ::user/uid user-uid)
      (terminate context))))

;;
;; * Interceptor
;;

(def event-auth
  (interceptor/interceptor
   {:name ::event-auth :enter enter}))
