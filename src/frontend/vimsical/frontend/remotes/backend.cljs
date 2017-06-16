(ns vimsical.frontend.remotes.backend
  (:require
   [clojure.pprint :as pprint]
   [clojure.spec.alpha :as s]
   [vimsical.common.util.transit :as transit]
   [vimsical.frontend.remotes.remote :as remote]
   [vimsical.frontend.util.xhr :as xhr]
   [vimsical.frontend.config :as config]
   [vimsical.remotes.event :as event])
  (:require-macros
   [vimsical.common.env-cljs :as env]))

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

(defn- response-result [_ resp]
  (try
    (some-> resp (xhr/response-text) (transit/read-transit))
    (catch :default _
      (.error js/console "ERROR Reading transit" (xhr/response-text resp))
      (xhr/response-text resp))))

(defn- response-error [_ resp]
  (or
   ;; Try to read a remote error
   (try
     (some-> resp (xhr/response-text) (transit/read-transit))
     (catch :default _))
   ;; Fallback to xhrio debug messages
   (xhr/response-error resp)))

(defn- request-data [event]
  (transit/write-transit event))

;;
;; * Remote FX Implementation
;;

(defn debug-resp
  [resp event]
  (let [logged {:event event :resp resp}]
    (pprint/pprint logged)
    resp))

(def backend-config
  {:protocol (env/optional :backend-protocol ::env/string)
   :host     (env/optional :backend-host ::env/string)
   :port     (env/optional :backend-port ::env/string)
   :path     (env/required :backend-path ::env/string)})

(defmethod remote/init! :backend [_] backend-config)

(defmethod remote/send! :backend
  [{:keys [event] :as fx} state result-cb error-cb]
  (s/assert ::event/event event)
  (letfn [(xhr-success-cb [resp]
            (cond-> (response-result fx resp)
              ;; config/debug? (debug-resp event)
              true          (result-cb)))
          (xhr-error-cb [resp]
            (cond-> (response-error fx resp)
              config/debug? (debug-resp event)
              true          (error-cb)))]
    (do (xhr/new-post-request
         (xhr/new-uri state)
         (request-data event)
         transit-headers
         xhr-success-cb xhr-error-cb)
        nil)))
