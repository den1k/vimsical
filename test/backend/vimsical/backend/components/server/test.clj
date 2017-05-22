(ns vimsical.backend.components.server.test
  (:require [clojure.spec :as s]
            [io.pedestal.test :as pedestal.test]
            [vimsical.backend.components.service :as service]
            [vimsical.backend.components.session-store :as session-store]
            [vimsical.backend.components.session-store.spec :as session-store.spec]
            [vimsical.backend.system.fixture :refer [*service-fn* *system*]]
            [vimsical.common.util.transit :as transit]))

(defn response-for
  ([event] (response-for *service-fn* event))
  ([service-fn event]
   (pedestal.test/response-for
    service-fn
    :post (service/url-for :events)
    :headers {"Content-Type" "application/transit+json"}
    :body (transit/write-transit event))))

(defn status-ok? [{:keys [status]}] (<= 200 status 299))

(defn response->session-key [{{:strs [Set-Cookie]} :headers}]
  (some->> Set-Cookie first (re-find #"ring-session=(.+);Path=.+") second))

(defn active-session? [response]
  (if-some [session-store (:session-store *system*)]
    (let [session-key (response->session-key response)
          session     (session-store/read-session* session-store session-key)]
      (s/valid? ::session-store.spec/active-session session))
    (assert false "Session store not found, was the system fixture started?")))
