(ns vimsical.backend.components.server.test
  (:require
   [clojure.spec.alpha :as s]
   [io.pedestal.test :as pedestal.test]
   [vimsical.backend.components.service :as service]
   [vimsical.backend.components.session-store :as session-store]
   [vimsical.backend.components.session-store.spec :as session-store.spec]
   [vimsical.backend.system.fixture :refer [*service-fn* *session-key* *system*]]
   [vimsical.common.util.transit :as transit]))

(defn response-for
  ([event]
   (response-for *service-fn* event *session-key*))
  ([service-fn event]
   (response-for service-fn event *session-key*))
  ([service-fn event session-key]
   (letfn [(session-cookie [session-key]
             (str "ring-session=" session-key))
           (req-headers [session-key]
             (cond-> {"Content-Type" "application/transit+json"}
               (some? session-key) (assoc "cookie" (session-cookie session-key))))
           (parse-resp [resp]
             (try
               (update resp :body transit/read-transit)
               (catch Throwable t
                 resp)))]
     (-> service-fn
         (pedestal.test/response-for
          :post (service/url-for :events)
          :headers (req-headers session-key)
          :body (transit/write-transit event))
         (parse-resp)))))

(defn status-ok? [{:keys [status]}] (and (number? status) (<= 200 status 299)))

(defn response->session-key [{{:strs [Set-Cookie]} :headers}]
  (some->> Set-Cookie first (re-find #"ring-session=(.+);Path=.+") second))

(defn auth-session? [response]
  (if-some [session-store (:session-store *system*)]
    (let [session-key (response->session-key response)
          session     (session-store/read-session* session-store session-key)]
      (s/valid? ::session-store.spec/auth-session session))
    (assert false "Session store not found, was the system fixture started?")))
