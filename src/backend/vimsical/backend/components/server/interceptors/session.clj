(ns vimsical.backend.components.server.interceptors.session
  (:require
   [io.pedestal.interceptor :as interceptor]
   [ring.middleware.session :as middlewares.session]))

;;
;; * Helpers
;;

(defn- context->session-options
  "Return valid options for `middlewares.session/session-response` using the
  context's `:session-store`."
  [{:keys [session-store] :as context}]
  {:pre [session-store] :post [(contains? % :store)]}
  ((deref #'middlewares.session/session-options) {:store session-store}))

(defn- session-request
  [context]
  (let [options (context->session-options context)]
    (update context :request middlewares.session/session-request options)))

(defn- session-response
  [{:keys [response request session] :as context}]
  (let [options         (context->session-options context)
        session-reponse (middlewares.session/session-response
                         (assoc response :session session)
                         request options)]
    (assoc context :response session-reponse)))

;;
;; * Interceptor
;;

(def session
  "Same as `io.pedestal.http.ring-middleware/session` but gets the session store
  from the injected system component."
  (interceptor/interceptor
   {:name  ::session-store
    :enter session-request
    :leave session-response}))
