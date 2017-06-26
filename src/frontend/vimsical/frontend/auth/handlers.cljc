(ns vimsical.frontend.auth.handlers
  (:require
   [re-frame.core :as re-frame]
   [vimsical.frontend.util.mapgraph :as util.mg]
   [vimsical.remotes.backend.auth.commands :as auth.commands]
   [vimsical.remotes.backend.auth.queries :as auth.queries]
   [vimsical.frontend.router.handlers :as router.handlers]
   [vimsical.frontend.router.routes :as router.routes]))

;;
;; * Invite
;;

(def signup-success-route ::router.routes/landing)


;; Reading the invite

(defmethod router.handlers/did-mount-history-route-fx-handler ::router.routes/invite
  [_ [_ route]]
  (if-some [token (router.routes/get-arg route :token)]
    {:remote
     {:id               :backend
      :event            [::auth.queries/invite token]
      :dispatch-success ::invite-success
      :status-key       ::invite}}
    (throw (ex-info "Missing token" {:route route}))))

(defn invite-success-handler
  [{:keys [db]} [_ invite-user]]
  {:db (util.mg/add-linked-entities db {:app/user invite-user})})

(re-frame/reg-event-fx ::invite-success invite-success-handler)

;; Submitting the signup

(defn invite-signup-handler
  [_ [_ token user status-key]]
  {:remote
   {:id               :backend
    :event            [::auth.commands/invite-signup token user]
    :dispatch-success ::invite-signup-success
    :status-key       status-key}})

(defn invite-signup-success-handler
  [{:keys [db]} [_ user]]
  {:db (util.mg/add-linked-entities db {:app/user user})
   :dispatch
   [::router.handlers/route signup-success-route]})

(re-frame/reg-event-fx ::invite-signup         invite-signup-handler)
(re-frame/reg-event-fx ::invite-signup-success invite-signup-success-handler)

;;
;; * Signup
;;

(defn signup-handler
  [{:keys [db]} [_ signup-user status-key]]
  {:remote
   {:id               :backend
    :event            [::auth.commands/signup signup-user]
    :dispatch-success ::signup-success
    :status-key       status-key}})

(defn signup-success-handler
  [{:keys [db]} [_ user]]
  {:db (util.mg/add-linked-entities db {:app/user user})
   :dispatch
   [::router.handlers/route signup-success-route]})

(re-frame/reg-event-fx ::signup         signup-handler)
(re-frame/reg-event-fx ::signup-success signup-success-handler)

;;
;; * Login
;;

(defn login-event-fx
  [{:keys [db]} [_ login-user status-key]]
  {:db (util.mg/add db login-user)
   :remote
   {:id               :backend
    :event            [::auth.commands/login login-user]
    :dispatch-success ::login-success
    :dispatch-error   ::login-error
    :status-key       status-key}})

(defn login-success-event-fx
  [{:keys [db]} [_ user]]
  {:db (util.mg/add-linked-entities db {:app/user user})})

(defn login-error-event-fx
  [{:keys [db]} [_ error]]
  (re-frame.loggers/console :log "Login error" error))

(re-frame/reg-event-fx ::login         login-event-fx)
(re-frame/reg-event-fx ::login-success login-success-event-fx)
(re-frame/reg-event-fx ::login-error   login-error-event-fx)

;;
;; * Logout
;;

(defn logout-handler
  [{:keys [db]} _]
  {:db (util.mg/remove db :app/user)
   :remote
   {:id               :backend
    :event            [::auth.commands/logout]}
   :dispatch
   [::router.handlers/route ::router.routes/landing]})

(re-frame/reg-event-fx ::logout logout-handler)
