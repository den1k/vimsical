(ns vimsical.frontend.remotes.fx
  "Usage:
  (re-frame/reg-event-fx ::example
    (fn [db _]
      ...
      {:db ...
       :remote
        {:id    :backend
         :event [::auth.commands/login {::user/email \"foo@bar.com\" ::user/password \"123\"}]}}"
  (:require
   [clojure.spec :as s]
   [re-frame.core :as re-frame]
   [re-frame.interop :as interop]
   [vimsical.frontend.remotes.remote :as p]
   [vimsical.remotes.event :as event]))

;;
;; * Fx Spec
;;

(s/def ::id keyword?)
(s/def ::event ::event/event)
(s/def ::disptach-error keyword?)
(s/def ::fx (s/keys :req-un [::id ::event/event] :opt-un [::disptach-error]))

;;
;; * Remotes registry
;;

(defonce ^:private remotes-registry (interop/ratom {}))

(defn- get-remote [{:keys [id] :as fx}]
  (if-some [remote (get @remotes-registry id)]
    remote
    (let [remote (p/init! id)]
      (swap! remotes-registry assoc id remote)
      remote)))

;;
;; * Event status registry
;;

(defonce ^:private status-registry (interop/ratom {}))

;;
;; ** Spec
;;

(defn- ex-info? [x] (and x (instance? clojure.lang.ExceptionInfo x)))

(s/def ::error ex-info?)

(s/def ::status
  (s/nilable
   (s/or :pending #{::pending}
         :success #{::success}
         :error     ::error)))

(s/fdef get-status :args (s/cat :registry map? :id ::id :event ::event?))

(defn- get-status [status-registry remote-id event]
  (get-in status-registry [remote-id event]))

(s/fdef set-fx-status :args (s/cat :registry map? :fx ::fx :status ::status) :ret map?)

(defn- set-fx-status [status-registry {:keys [id event] :as fx} status]
  (assoc-in status-registry [id event] status))

;;
;; * Event status subscription
;;

(re-frame/reg-sub-raw
 ::status
 (fn [_ [_ remote-id event]]
   (s/assert ::id remote-id)
   (s/assert ::event event)
   (interop/make-reaction
    (fn []
      (get-status @status-registry remote-id event)))))

;;
;; * Fx
;;

(s/fdef remote-fx :args (s/cat :fx ::fx))

(defn- remote-fx
  [{:keys [id event dispatch-error] :as fx}]
  (let [remote (get-remote fx)]
    (letfn [(result-cb [result]
              (let [[event-id] event]
                (do
                  ;; Update the status registry with the ::success flag
                  (swap! status-registry set-fx-status fx ::success)
                  ;; Disptach the event with the result
                  (re-frame/dispatch [event-id result]))))
            (error-cb [error]
              (do
                ;; Update the registry with the error
                (swap! status-registry set-fx-status fx error)
                ;; Conditionally dispatch if the fx specifies a handler
                (when dispatch-error
                  (re-frame/dispatch [dispatch-error error]))))]
      (do
        ;; Update the status to ::pending
        (swap! status-registry set-fx-status fx ::pending)
        ;; Send and update the remotes registry with the new state if the method
        ;; doesn't return nil. This might not be a good fit, since it may be
        ;; more natural to want to keep track of a per-event state, like a
        ;; connection?
        (when-some [remote (p/send! id remote event result-cb error-cb)]
          (swap! remotes-registry assoc id remote))))))

(re-frame/reg-fx :remote remote-fx)
