(ns vimsical.frontend.auth.handlers
  (:require
   [re-frame.core :as re-frame]
   [vimsical.frontend.util.mapgraph :as util.mg]
   [vimsical.remotes.backend.auth.commands :as auth.commands]))

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


(defn signup-result-handler
  [{:keys [db]} [_ result]]
  {:db (util.mg/add-linked-entities db {:app/user result})})

(re-frame/reg-event-fx ::signup        signup-handler)
(re-frame/reg-event-fx ::signup-success signup-result-handler)

;;
;; * Login
;;

(defn login-handler
  [{:keys [db]} [_ login-user status-key]]
  {:db (util.mg/add db login-user)
   :remote
   {:id               :backend
    :event            [::auth.commands/login login-user]
    :dispatch-success ::login-success
    :status-key       status-key}})

(defn login-result-handler
  [{:keys [db]} [_ result]]
  {:db (util.mg/add-linked-entities db {:app/user result})})

(re-frame/reg-event-fx ::login         login-handler)
(re-frame/reg-event-fx ::login-success login-result-handler)
