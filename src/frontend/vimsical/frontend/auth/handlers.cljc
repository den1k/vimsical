(ns vimsical.frontend.auth.handlers
  (:require
   [re-frame.core :as re-frame]
   [vimsical.remotes.backend.auth.commands :as auth.commands]
   [vimsical.frontend.util.mapgraph :as util.mg]))

;;
;; * Register
;;

(defn register-handler
  [{:keys [db]} [_ register-user]]
  {:remote
   {:id    :backend
    :event [::auth.commands/register register-user]}})

(defn register-result-handler
  [{:keys [db]} [_ result]]
  {:db (util.mg/add-linked-entities db {:app/user result})})

(re-frame/reg-event-fx ::register               register-handler)
(re-frame/reg-event-fx ::auth.commands/register register-result-handler)

;;
;; * Login
;;

(defn login-handler
  [{:keys [db]} [_ login-user]]
  {:db     (util.mg/add db login-user)
   :remote {:id    :backend
            :event [::auth.commands/login login-user]}})

(defn login-result-handler
  [{:keys [db]} [_ result]]
  {:db (util.mg/add-linked-entities db {:app/user result})})

(re-frame/reg-event-fx ::login               login-handler)
(re-frame/reg-event-fx ::auth.commands/login login-result-handler)
