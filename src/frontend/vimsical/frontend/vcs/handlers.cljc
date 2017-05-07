(ns vimsical.frontend.vcs.handlers
  (:require
   [com.stuartsierra.mapgraph :as mg]
   [re-frame.core :as re-frame]
   [vimsical.common.util.core :as util]
   [vimsical.common.uuid :refer [uuid]]
   [vimsical.frontend.util.re-frame :as util.re-frame]
   [vimsical.frontend.vcs.queries :as queries]
   [vimsical.frontend.vcs.subs :as subs]
   [vimsical.vcs.core :as vcs]
   [vimsical.vcs.editor :as editor]))

;; TODO parametrize vims
(defn init-vims
  [db [_]]
  (let [{:keys      [db/id]
         :vims/keys [branches] :as vims} (mg/pull-link db queries/vims :app/vims)
        vcs-state                        (vcs/empty-vcs branches)
        vcs-entity                       (assoc vcs-state :db/id (uuid))
        vims'                            (assoc vims :vims/vcs vcs-entity)]
    (mg/add db vims')))

(re-frame/reg-event-db ::init-vims init-vims)

(defn add-edit-event
  [vcs file-id edit-event]
  (let [effects {::editor/pad-fn       (constantly 1000)
                 ::editor/uuid-fn      (fn [_e] (uuid))
                 ::editor/timestamp-fn (fn [_e] (util/now))}]
    (vcs/add-edit-event vcs effects file-id edit-event)))

(re-frame/reg-event-fx
 ::add-edit-event
 [(util.re-frame/inject-sub [::subs/vcs])]
 (fn [{:keys [db] ::subs/keys [vcs]} [_ file-id edit-event]]
   {:db (mg/add db (add-edit-event vcs file-id edit-event))}))
