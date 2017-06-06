(ns vimsical.backend.handlers.multi
  (:require
   [clojure.spec.alpha :as s]
   [io.pedestal.interceptor.chain :as interceptor.chain]
   [vimsical.remotes.event :as event]
   [vimsical.backend.util.async :as async]
   [clojure.core.async :as a]))

;;
;; * Context spec
;;

;;
;; ** Request ::event/event in request body
;;

(s/def :vimsical.backend.handlers.multi.request/body ::event/event)
(s/def ::request (s/keys :req-un [:vimsical.backend.handlers.multi.request/body]))
(s/def ::request-context (s/keys :req-un [::request]))

;;
;; ** Response ::event/result or ::event/error in response body
;;

(s/def :vimsical.backend.handlers.multi.response/body (s/or :result ::event/result :error ::event/error))
(s/def ::response (s/keys :req-un [:vimsical.backend.handlers.multi.response/body]))

(defn response-context->result-event-conformer
  "Conform a response context to a result event compatible with
  `vimsical.remotes.event/result-spec`."
  [context]
  (let [[event-id] (-> context :request :body)
        result     (-> context :response :body)]
    (if (and event-id result)
      [event-id result]
      ::s/invalid)))

(s/def ::response-context->result-event-conformer
  (s/conformer response-context->result-event-conformer))

(s/def ::response-context
  (s/and ::response-context->result-event-conformer
         (s/or :error  ::event/error
               :result ::event/result)))
;;
;; * Event handler
;;

;; NOTE not spec-ed, function of:
;;
;; [context, event] => context or response or result
;;
;; We spec the context handler instead, after lifting the event handler's result

(defn handle-event-dispatch [context [id]] id)

(defmulti handle-event handle-event-dispatch)

;;
;; * Context handler
;;

;; Define additional constraints on the request context, allowing handlers to
;; spec their components and/or session dependencies

(def event-context-dispatch (comp first :event))      ; event id

(defmulti context-spec
  "Handlers should provide a method for the event id that they handle-event, and
  spec their dependencies."  event-context-dispatch)
(defmethod context-spec :default [_] any?)
(s/def ::context-spec (s/multi-spec context-spec event-context-dispatch))

(s/def ::context-in  (s/and  ::request-context ::context-spec))
(s/def ::context-out ::response-context)

;;
;; * Session helpers
;;

(defn assoc-session
  [{:keys [session] :as context} k value]
  (let [session' (assoc session k value)]
    (assoc-in context [:response :session] session')))

(defn assoc-in-session
  [{:keys [session] :as context} ks value]
  (let [session' (assoc-in session ks value)]
    (assoc-in context [:response :session] session')))

(defn set-session
  [context session]
  (assoc-in context [:response :session] session))

(defn reset-session
  [context session]
  (set-session context (vary-meta session assoc :recreate true)))

(defn delete-session
  [context]
  (assoc-in context [:response :session] nil))

;;
;; * Error helpers
;;

(defn set-error
  [context throwable]
  (assoc context ::interceptor.chain/error throwable))

;;
;; * Response helpers
;;

(defn set-response
  ([context body] (set-response context 200 body))
  ([context status body]
   (update context :response
           (fn [response]
             (assoc response
                    :status status
                    :body (or body ""))))))

;;
;; * Async context
;;

(defmacro async
  [context & body]
  `(a/go
     (try
       (do ~@body)
       (catch Throwable t#
         (set-error ~context t#)))))
