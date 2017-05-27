(ns vimsical.integration.remotes.backend
  (:require
   [clojure.spec :as s]
   [vimsical.backend.components.server.test :as server.test]
   [vimsical.backend.util.log :as log]
   [vimsical.common.env :as env]
   [vimsical.common.util.transit :as transit]
   [vimsical.frontend.remotes.remote :as remote]))

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
  [_ {:keys [body]}]
  (try
    ;; Missing util to properly decode responses depending on the content-type
    (transit/read-transit body)
    (catch Throwable _)))

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
  [{:keys [event] :as fx} state result-cb error-cb]
  (letfn [(xhr-success-cb [resp]
            (log/debug "RAW" resp)
            (result-cb (response-result fx resp)))
          (xhr-error-cb [resp]
            (log/error "ERROR" resp)
            (error-cb (response-result fx resp)))
          (ok? [{:keys [status] :as response}] (< 199 status 300))]
    (let [response (server.test/response-for event)]
      (log/debug response)
      (if (ok? response)
        (xhr-success-cb response)
        (xhr-error-cb response)))))
