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
(s/def ::disptach-success (s/or :disabled false? :handler-id keyword?))
(s/def ::disptach-error keyword?)
(s/def ::status-key some?)
(s/def ::fx (s/keys :req-un [::id ::event/event]
                    :opt-un [::status-key ::dispatch-success ::disptach-error]))

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

(s/def ::error map?)

(s/def ::status
  (s/nilable
   (s/or :pending #{::pending}
         :success #{::success}
         :error     ::error)))

(s/fdef get-status
        :args (s/cat :registry map?
                     :remote-id ::id
                     :status-key ::status-key)
        :ret ::status)

(defn- get-status [status-registry remote-id status-key]
  (get-in status-registry [remote-id status-key]))

(s/fdef set-status
        :args (s/cat :registry map?
                     :remote-id ::id
                     :status-key (s/nilable ::status-key)
                     :status ::status)
        :ret map?)

(defn- set-status [status-registry remote-id status-key status]
  (cond-> status-registry
    (some? status-key) (assoc-in [remote-id status-key] status)))

;;
;; * Event status subscription
;;

(re-frame/reg-sub-raw
 ::status
 (fn [_ [_ remote-id status-key]]
   (interop/make-reaction
    (fn []
      (get-status @status-registry remote-id status-key)))))

;;
;; * Event status dispatches
;;

(defn dispatch-success!
  [{:keys [event dispatch-success]} result]
  (when-not (false? dispatch-success)
    (let [[event-id] event
          dispatch-id (or dispatch-success event-id)]
      (re-frame/dispatch [dispatch-id result]))))

(defn dispatch-error!
  [{:keys [dispatch-error]} error]
  (when dispatch-error
    (re-frame/dispatch [dispatch-error error])))

;;
;; * Fx
;;

(s/fdef remote-fx :args (s/cat :fx ::fx))

(defn- remote-fx
  [{:keys [id event status-key] :as fx}]
  (let [remote (get-remote fx)]
    (letfn [(result-cb [result]
              (do (swap! status-registry set-status id status-key ::success)
                  (dispatch-success! fx result)))
            (error-cb [error]
              (do (swap! status-registry set-status id status-key error)
                  (dispatch-error! fx error)))]
      (do (swap! status-registry set-status id status-key ::pending)
          ;; Send and update the remotes registry with the new state if the method
          ;; doesn't return nil. This might not be a good fit, since it may be
          ;; more natural to want to keep track of a per-event state, like a
          ;; connection?
          (when-some [remote (p/send! id remote event result-cb error-cb)]
            (swap! remotes-registry assoc id remote))))))

(re-frame/reg-fx :remote remote-fx)
