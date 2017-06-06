(ns vimsical.frontend.auth.handlers
  (:require
   [re-frame.core :as re-frame]
   [vimsical.frontend.util.mapgraph :as util.mg]
   [vimsical.remotes.backend.auth.commands :as auth.commands]
   [vimsical.frontend.app.handlers :as app.handlers]))

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
   [::app.handlers/route :route/landing]})

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
  {:db (util.mg/remove-links db :app/user)
   :remote
   {:id               :backend
    :event            [::auth.commands/logout]
    :dispatch-success false}})

(re-frame/reg-event-fx ::logout logout-handler)
