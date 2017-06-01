(ns vimsical.frontend.user.handlers
  (:require
   [re-frame.core :as re-frame]
   [vimsical.frontend.util.mapgraph :as util.mg]
   [vimsical.remotes.backend.user.queries :as user.queries]))

;;
;; * Me
;;

(re-frame/reg-event-fx
 ::me
 (fn [_ [_ status-key]]
   {:remote
    {:id               :backend
     :event            [::user.queries/me]
     :status-key       status-key
     :dispatch-success ::me-success
     :dispatch-error   ::me-error}}))

(re-frame/reg-event-fx
 ::me-success
 (fn [{:keys [db]} [_ user]]
   {:db (util.mg/add-linked-entities db {:app/user user})}))
