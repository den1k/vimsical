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
  [{:keys [session-store]}]
  {:pre  [session-store]
   :post [(contains? % :store)]}
  ((deref #'middlewares.session/session-options) {:store session-store}))

(defn- enter
  [{:keys [request] :as context}]
  (let [options                               (context->session-options context)
        {:keys [session] :as session-request} (middlewares.session/session-request request options)]
    (assoc context :request session-request :session session)))

(defn- leave
  [{:keys [request response] :as context}]
  (let [options          (context->session-options context)
        session-response (middlewares.session/session-response response request options)]
    (assoc context :response session-response)))

;;
;; * Interceptor
;;

(def session
  "Same as `io.pedestal.http.ring-middleware/session` but gets the session store
  from the injected system component."
  (interceptor/interceptor
   {:name  ::session-store
    :enter enter
    :leave leave}))
