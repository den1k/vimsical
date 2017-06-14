(ns vimsical.frontend.remotes.fx
  (:require
   [clojure.spec.alpha :as s]
   [re-frame.core :as re-frame]
   [re-frame.loggers :as re-frame.loggers]
   [re-frame.interop :as interop]
   [vimsical.frontend.remotes.remote :as p]
   [vimsical.remotes.event :as event]))

;;
;; * Fx Spec
;;

(s/def ::id keyword?)
(s/def ::event ::event/event)
(s/def ::dispatch-success (s/or :disabled false? :handler-id keyword?))
(s/def ::dispatch-error keyword?)
(s/def ::status-key any?)
(s/def ::fx (s/keys :req-un [::id ::event/event]
                    :opt-un [::status-key ::dispatch-success ::dispatch-error]))

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

(s/def ::status
  (s/nilable
   (s/or :pending #{::pending}
         :success #{::success}
         :error     ::event/error)))

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
                     :status-key ::status-key
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
   {:pre [remote-id]}
   (doto (interop/make-reaction
          #(get-status @status-registry remote-id status-key))
     (interop/add-on-dispose!
      #(do (re-frame.loggers/console :log "Disposing status" status-key)
           (swap! status-registry dissoc status-key))))))

;;
;; * Event status dispatches
;;

(defn- dispatch-maybe
  [dispatch {[event-id :as event] :event} result-or-error]
  (cond
    (true? dispatch)
    (re-frame/dispatch [event-id result-or-error])

    (keyword? dispatch)
    (re-frame/dispatch [dispatch result-or-error])

    (ifn? dispatch)
    (when-some [event (dispatch result-or-error)]
      (re-frame/dispatch event))

    :else nil))

(defn dispatch-success!
  [{:keys [dispatch-success] :as fx} result]
  (dispatch-maybe dispatch-success fx result))

(defn dispatch-error!
  [{:keys [dispatch-error] :as fx} result]
  (dispatch-maybe dispatch-error fx result))

;;
;; * Fx
;;

(s/fdef remote-fx :args (s/cat :fx ::fx))

(defn- remote-fx
  "Fx interpreter for a remote command or query.

  `id` is a valid dispatch value for
  `vimsical.frontend.remotes.backend/remote-init!` and
  `vimsical.frontend.remotes.backend/remote-send!`

  `event` is the event passed to `remote-send!`

  `status-key` if provided is any value that can be used when subscribing to
  `::status` to retrieve the state of the query. The status can be:

  - `nil` no request found for that key
  - `::pending` awaiting a response
  - `::success` completed successfully
  - any other value is an error, which is a map with `:msg`, `:data`,
    `:reason` as returned by the backend

  `dispatch-success` and `dispatch-error` control what events get dispatched in
  each case.

  - `true?` will dispatch    `[<event-id> <result-or-error>]``
  - `keyword?` will dispatch `[<keyword> <result-or-error>]`
  - `ifn?` will be invoked with `event` and `result` or `error`, should return
    an event vector to dispatch.

  `dispatch-success` defaults to true.
  "
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
          (when-some [remote (p/send! fx remote result-cb error-cb)]
            (swap! remotes-registry assoc id remote))))))

(defn- remote-fx-or-fxs
  [fx-or-fxs]
  (doseq [fx (if (sequential? fx-or-fxs) fx-or-fxs [fx-or-fxs])]
    (remote-fx fx)))

(re-frame/reg-fx :remote remote-fx-or-fxs)