(ns vimsical.backend.components.server.interceptors.event-auth
  "Authentication logic for event handlers.

  Event handlers that require user authentication can implement an
  `require-auth?` method that returns true for their event id.

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

(defmulti  require-auth? event/dispatch)
(defmethod require-auth? :default [_] false)

;;
;; * Internal
;;

;; XXX Decomplect request-event-lifting in event handler so that we can insert
;; this interceptor in between the event lifting and handling

(defn- context-require-auth?
  [context]
  (some-> context :request :body require-auth?))

(defn- context->user-uid
  [context]
  (some-> context :request :session ::user/uid))

(defn- unauthorized
  [context]
  (let [error-response (response/status {:body "Unauthorized"} 401)]
    (interceptor.chain/terminate
     (assoc context :response error-response))))

(defn- enter
  [context]
  (if-not (context-require-auth? context)
    context
    (if-some [user-uid (context->user-uid context)]
      (assoc context ::user/uid user-uid)
      (unauthorized context))))

;;
;; * Interceptor
;;

(def event-auth
  (interceptor/interceptor
   {:name ::event-auth :enter enter}))
