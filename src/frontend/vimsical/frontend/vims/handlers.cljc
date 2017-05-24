(ns vimsical.frontend.vims.handlers
  (:require
   [re-frame.core :as re-frame]
   [vimsical.common.uuid :refer [uuid]]
   [vimsical.frontend.util.mapgraph :as util.mg]
   [vimsical.remotes.backend.vims.commands :as vims.commands]
   [vimsical.user :as user]
   [vimsical.vcs.file :as vcs.file]
   [vimsical.vims :as vims]
   [vimsical.vcs.core :as vcs.core]))

;;
;; * New vims
;;

;; Could parametrize the user prefs here?

(defn- default-files
  ([] (default-files (uuid) (uuid) (uuid)))
  ([html-uid css-uid javascript-uid]
   [(vcs.file/new-file html-uid       :text :html)
    (vcs.file/new-file css-uid        :text :css)
    (vcs.file/new-file javascript-uid :text :javascript)]))

(defn new-handler
  [{:keys [db]} [_ owner status-key]]
  (let [files    (default-files)
        new-vims (vims/new-vims owner nil files)
        ;; This is a bit low-level, we could just conj the vims onto the owner
        ;; and add that, but we'd have to make sure we're passed the full
        ;; app/user, not sure if that'd be inconvenient.
        db'      (util.mg/add-join db owner ::user/vimsae new-vims)]
    {:db db'
     :remote
     {:id         :backend
      :event      [::vims.commands/new new-vims]
      :status-key status-key}}))

(re-frame/reg-event-fx ::new new-handler)

;;
;; * Title
;;

(defn title-handler
  [{:keys [db]} [_ vims title]]
  (let [vims' (assoc vims ::vims/title title)
        db'   (util.mg/add db vims')]
    {:db db'
     :remote
     {:id    :backend
      :event [::vims.commands/title vims']}}))

(re-frame/reg-event-fx ::title title-handler)

;;
;; * Snapshots
;;

(defn update-snapshots-handler
  [{:keys [db]} [_ {::vims/keys [vcs] :as vims}]]
  (let [owner-uid (-> vims ::vims/owner :db/uid)
        vims-uid  (-> vims :db/uid)
        snapshots (vcs.core/vims-snapshots vcs owner-uid vims-uid)
        vims'     (assoc vims ::vims/snapshots snapshots)
        db'       (util.mg/add db vims')]
    {:db db'
     :remote
     {:id    :backend
      :event [::vims.commands/update-snapshots snapshots]}}))

(re-frame/reg-event-fx ::update-snapshots update-snapshots-handler)
