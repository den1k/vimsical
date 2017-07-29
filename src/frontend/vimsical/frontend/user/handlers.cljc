(ns vimsical.frontend.user.handlers
  (:require
   [re-frame.core :as re-frame]
   [vimsical.subgraph :as sg]
   [vimsical.frontend.util.subgraph :as util.sg]
   [vimsical.remotes.backend.user.queries :as user.queries]
   [medley.core :as md]
   [vimsical.common.uuid :refer [uuid]]
   [vimsical.user :as user])
  (:refer-clojure :exclude [uuid]))

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
   {:db (util.sg/add-linked-entities db {:app/user user})}))

;; We mosly ignore this because this would happened for every anon user when trying
;; to initialize an non-existing session

(re-frame/reg-event-fx
 ::me-error
 (fn [{:keys [db]} _]))

;;
;; * Settings
;;

(re-frame/reg-event-fx
 ::playback-speed
 (fn [{:keys [db]} [_ settings direction]]
   {:pre [(contains? #{:inc :dec} direction)]}
   (let [range     [1 1.5 1.75 2 2.25 2.5]
         range     (cond-> range (= :dec direction) reverse)
         cur-speed (get settings :settings/playback-speed 1)
         speed     (first (md/drop-upto #(= cur-speed %) (cycle range)))
         settings  (assoc (or settings {:db/uid (uuid)})
                          :settings/playback-speed speed)
         db'       (sg/add
                    db
                    {:db/uid         (second (:app/user db))
                     ::user/settings settings})]
     {:db        db'
      :scheduler {:action :set-speed :speed speed}})))
