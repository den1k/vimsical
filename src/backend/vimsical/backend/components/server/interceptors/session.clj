(ns vimsical.backend.components.server.interceptors.session
  (:require
   [io.pedestal.interceptor :as interceptor]
   [ring.middleware.session :as middlewares.session]))

(defn- context->session-options
  [{:keys [session-store] :as context}]
  {:pre [session-store]}
  {:store session-store})

(def session
  "Same as `io.pedestal.http.ring-middleware/session` but gets the session store
  from the injected system component."
  (interceptor/interceptor
   {:name  ::session-store
    :enter (fn [context]
             (let [options (context->session-options context)]
               (update context :request middlewares.session/session-request options)))
    :leave (fn [{:keys [response] :as context}]
             (let [options (context->session-options context)]
               (cond-> context
                 (some? response) (update :response middlewares.session/session-response options))))}))
