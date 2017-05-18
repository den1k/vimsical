(ns vimsical.backend.components.server.interceptors.session
  (:require
   [io.pedestal.interceptor :as interceptor]
   [ring.middleware.session :as middlewares.session]))

;;
;; * Helpers
;;

(defn- context->session-options
  [{:keys [session-store] :as context}]
  {:pre [session-store] :post [(contains? % :store)]}
  ((deref #'middlewares.session/session-options) {:store session-store}) )

;;
;; * Interceptor
;;

(defn- enter
  [context]
  (let [options (context->session-options context)]
    (update context :request middlewares.session/session-request options)))

(defn- leave [{:keys [request] :as context}]
  (let [options (context->session-options context)]
    (update context :response middlewares.session/session-response request options)))

(def session
  "Same as `io.pedestal.http.ring-middleware/session` but gets the session store
  from the injected system component."
  (interceptor/interceptor
   {:name ::session-store :enter enter :leave leave}))

;;
;; * Interceptor API
;;

(def empty-session ^:recreate {})

(defn recreate [session] ^:recreate session)

(defn set-session [context session]
  (assoc-in context [:response :session] session))

(assert (= {:response {:session {:foo :bar}}} (set-session {} {:foo :bar})))

(defn assoc-session [context k v & kvs]
  (apply update-in context [:response :session] assoc k v kvs))

(assert (= {:response {:session {:foo :bar :bar :baz}}}
           (assoc-session {} :foo :bar :bar :baz)))
