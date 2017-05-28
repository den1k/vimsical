(ns vimsical.frontend.user.handlers
  (:require
   [re-frame.core :as re-frame]
   [vimsical.common.uuid :refer [uuid]]
   [vimsical.frontend.util.mapgraph :as util.mg]
   [vimsical.remotes.backend.user.queries :as user.queries]
   [vimsical.user :as user]
   [vimsical.vims :as vims]))

(re-frame/reg-event-fx
 ::me
 (fn [_ [_ status-key]]
   {:remote
    {:id               :backend
     :event            [::user.queries/me]
     :status-key       status-key
     :dispatch-success ::me-result}}))

(defn- assign-snapshot-uids [user]
  (letfn [(update-snapshot [snapshot]
            (assoc snapshot :db/uid (uuid)))
          (update-vims [vims]
            (update vims ::vims/snapshots
                    (partial mapv update-snapshot)))]
    (update user ::user/vimsae (partial mapv update-vims))))

(re-frame/reg-event-fx
 ::me-result
 (fn [{:keys [db]} [_ user]]
   (let [user' (assign-snapshot-uids user)
         state {:app/user user'}]
     {:db (util.mg/add-linked-entities db state)})))
