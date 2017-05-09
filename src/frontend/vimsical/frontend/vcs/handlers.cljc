(ns vimsical.frontend.vcs.handlers
  "TODO
  - advance vcs playhead-entry to newly inserted entry
  - change `init-vims` to init from an exitsing vims
  - add cofxs for add-edit-event (timestamp, uuid and padding)
  "
  (:require
   [com.stuartsierra.mapgraph :as mg]
   [re-frame.core :as re-frame]
   [vimsical.common.util.core :as util]
   [vimsical.common.uuid :refer [uuid]]
   [vimsical.frontend.util.re-frame :as util.re-frame]
   [vimsical.frontend.vcs.queries :as queries]
   [vimsical.frontend.vcs.subs :as subs]
   [vimsical.frontend.vcs.db :as vcs.db]
   [vimsical.vcs.core :as vcs]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.editor :as editor]))

;;
;; * VCS Vims init
;;

(defn init-vims
  [db [_]]
  (let [{:as        vims
         :keys      [db/id]
         :vims/keys [branches]} (mg/pull-link db queries/vims :app/vims)
        master                  (branch/master branches)
        vcs-state               (vcs/empty-vcs branches)
        vcs-frontend-state      {:db/id             (uuid)
                                 ::vcs.db/branch-id (:db/id master)
                                 ::vcs.db/delta-id  nil}
        vcs-entity              (merge vcs-state vcs-frontend-state)
        vims'                   (assoc vims :vims/vcs vcs-entity)]
    (mg/add db vims')))

(re-frame/reg-event-db ::init-vims init-vims)

;;
;; * Editor cofxs
;;

(re-frame/reg-cofx
 ::editor/effects
 (fn [context _]
   (assoc context ::editor/effects
          {::editor/uuid-fn      (fn [_] (uuid))
           ::editor/timestamp-fn (fn [_] (util/now))
           ::editor/pad-fn       (constantly 1000)})))

;;
;; * Edit events
;;

(defn add-edit-event
  [{::vcs.db/keys [branch-id delta-id] :as vcs} effects file-id edit-event]
  (let [[vcs delta-id'] (vcs/add-edit-event vcs effects file-id branch-id delta-id edit-event)]
    (assoc vcs ::vcs.db/delta-id delta-id')))

(re-frame/reg-event-fx
 ::add-edit-event
 [(re-frame/inject-cofx ::editor/effects)
  (util.re-frame/inject-sub [::subs/vcs])]
 (fn [{:keys         [db]
       ::subs/keys   [vcs]
       ::editor/keys [effects]}
      [_ {file-id :db/id} edit-event]]
   {:db (mg/add db (add-edit-event vcs effects file-id edit-event))}))
