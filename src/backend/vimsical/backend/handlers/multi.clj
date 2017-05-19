(ns vimsical.backend.handlers.multi
  (:require
   [clojure.spec :as s]
   [vimsical.remotes.event :as event]))

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
(s/def ::response-context (s/keys :req-un [::request]))

;;
;; * Event handler
;;

;; NOTE not spec-ed, function of:
;;
;; [context, event] => context or response or result
;;

(defn handle-event-dispatch [context [id]] id)

(defmulti handle-event handle-event-dispatch)

;;
;; * Context handler
;;

(def context-dispatch (comp first :event))      ; event id

(defmulti context-spec
  "Handlers should provide a method for the event id that they handle-event, and
  spec their dependencies."  context-dispatch)

(s/def ::context-spec (s/multi-spec context-spec context-dispatch))
(s/def ::context-in  (s/and ::request-context ::context-spec))
(s/def ::context-out (s/and ::response-context))

(s/fdef handle-context :args (s/cat :context ::context-in) :ret ::context-out)

(defn handle-context [{:keys [event] :as context}]
  (handle-event context event))
