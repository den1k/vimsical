(ns vimsical.frontend.util.xhr
  "Success cb: remote, event, data
  Error cb: remote, event , errorn"
  (:require
   [clojure.string :as str]
   [goog.dom :as gdom]
   [goog.events :as gevents]
   [goog.net.EventType :as EventType])
  (:require-macros
   [vimsical.common.env-cljs :as env :refer [optional]])
  (:import
   goog.Uri
   [goog.net XhrIo ErrorCode]))

;;
;; * HTTP Helpers
;;

;;
;; ** URI
;;

(defn- new-uri
  [{:keys [protocol host port path]}]
  (str
   (doto (Uri.)
     (.setScheme protocol)
     (.setDomain host)
     (.setPort port)
     (.setPath path))))

;;
;; ** CSRF
;;

(defn- csrf-token
  "Return the csrf-token retrieved from the DOM. Note that this needs to be
  generated on every page load and is served by the default api handler."
  []
  (some-> (gdom/getElement "__anti-forgery-token")
          (.getAttribute "value")))

;; XXX conformers not aliased in cljs
(def ^:private csrf? (env/optional :csrf ::env/boolean))

(defn- assoc-csrf-token-header
  [headers]
  (cond-> headers
    csrf? (assoc "X-CSRF-Token" (csrf-token))))

(defn- xhrio-headers
  [headers]
  (-> headers assoc-csrf-token-header clj->js))

;;
;; * XHR helpers
;;

(defn response-error?
  [response-event]
  (pos? (.getLastErrorCode (.-target response-event))))

(defn response-error
  [response-event]
  (let [xhr        (.-target response-event)
        error-code (.getLastErrorCode xhr)
        status     (.getStatus xhr)
        error-msg  (ErrorCode.getDebugMessage error-code)]
    {:reason error-msg :status status}))

(defn response-status
  [response-event]
  (.getStatus (.-target response-event)))

(defn response-text
  [response-event]
  (let [text (.getResponseText (.-target response-event))]
    (when-not (str/blank? text) text)))

(defn offline?
  [response-event]
  (and (response-error? response-event)
       (zero? (response-status response-event))))

(defn new-post-request
  [uri post-body headers success-cb error-cb]
  (doto (XhrIo.)
    ;; This option is required to get the session to work with
    ;; CORS. Won't be necessary if we move the cljs server to the API.
    (.setWithCredentials true)
    (gevents/listen EventType/SUCCESS success-cb)
    (gevents/listen EventType/ERROR error-cb)
    (.send uri "POST" post-body (xhrio-headers headers))))
