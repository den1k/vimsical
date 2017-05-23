(ns vimsical.integration.remotes.backend
  (:require
   [vimsical.backend.components.server.test :as server.test]
   [clojure.spec :as s]
   [vimsical.common.util.transit :as transit]
   [vimsical.frontend.remotes.remote :as remote]
   [vimsical.common.env :as env]))

;;
;; * Spec
;;

(s/def ::protocol (s/nilable #{"http" "https"}))
(s/def ::host     (s/nilable string?))
(s/def ::port     (s/nilable number?))
(s/def ::path     (s/nilable string?))
(s/def ::config   (s/keys :opt-un [::protocol ::host ::port ::path]))

;;
;; * Transit
;;

(def ^:private transit-headers
  {"Content-Type" "application/transit+json"
   "Accept"       "application/transit+json"})

(defn- response-result
  [resp]
  (some-> resp (transit/read-transit)))

(defn- post-data
  [event]
  (transit/write-transit event))

;;
;; * Remote FX Implementation
;;

(defmethod remote/init! :backend [_]
  (s/assert
   ::config
   {:protocol (env/optional :backend-protocol ::env/string)
    :host     (env/optional :backend-host ::env/string)
    :port     (env/optional :backend-port ::env/string)
    :path     (env/required :backend-path ::env/string)}))

(defmethod remote/send! :backend
  [remote config event result-cb error-cb]
  (letfn [(xhr-success-cb [resp] (result-cb (response-result resp)))
          (xhr-error-cb   [resp] (error-cb (response-result resp)))
          (ok? [{:keys [status] :as response}] (< 199 status 300))]
    (let [response (server.test/response-for event)]
      (if (ok? response)
        (xhr-success-cb response)
        (xhr-error-cb response)))))
