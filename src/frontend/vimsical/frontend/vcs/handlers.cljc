(ns vimsical.frontend.vcs.handlers
  (:require
   [re-frame.core :as re-frame]
   [com.stuartsierra.mapgraph :as mg]
   [vimsical.common.util.core :as util]
   [vimsical.common.uuid :refer [uuid]]
   [vimsical.frontend.vcs.queries :as queries]
   [vimsical.vcs.core :as vcs]
   [vimsical.vcs.editor :as editor]
   [vimsical.vcs.branch :as branch]))


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
  [db [_ file-id edit-event]]
  (let [{:vims/keys [vcs] :as res} (mg/pull-link db queries/vims-vcs :app/vims)
        effects                    {::editor/pad-fn       (constantly 1)
                                    ::editor/uuid-fn      (fn [_e] (uuid))
                                    ::editor/timestamp-fn (fn [_e] (util/now))}
        vcs'                       (vcs/add-edit-event vcs effects file-id edit-event)]
    (mg/add db vcs')))

(re-frame/reg-event-db ::add-edit-event add-edit-event)
