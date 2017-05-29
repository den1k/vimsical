(ns vimsical.frontend.vims.handlers
  (:require
   [re-frame.core :as re-frame]
   [vimsical.common.uuid :refer [uuid]]
   [vimsical.frontend.util.mapgraph :as util.mg]
   [vimsical.frontend.vcs.handlers :as vcs.handlers]
   [vimsical.queries.snapshot :as snapshot]
   [vimsical.remotes.backend.vims.commands :as vims.commands]
   [vimsical.remotes.backend.vims.queries :as vims.queries]
   [vimsical.user :as user]
   [vimsical.vcs.core :as vcs.core]
   [vimsical.vcs.file :as vcs.file]
   [vimsical.vims :as vims]))

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
  [{:keys [db]} [_ vims title status-key]]
  (let [vims'  (assoc vims ::vims/title title)
        remote (select-keys vims' [:db/uid ::vims/title])
        db'    (util.mg/add db vims')]
    {:db db'
     :remote
     {:id         :backend
      :event      [::vims.commands/title remote]
      :status-key status-key}}))

(re-frame/reg-event-fx ::title title-handler)

;;
;; * Snapshots
;;

(defn new-snapshots
  [{:as                 vims
    vcs                 ::vims/vcs
    {owner-uid :db/uid} ::vims/owner
    vims-uid            :db/uid}]
  {:pre [vcs owner-uid vims-uid]}
  (mapv
   ;; NOTE we currently doesn't use a uid for the snapshots but mapgraph
   ;; requires some non-compound id key, so we generate a random one here, but
   ;; we may want to just add it to the schema instead?
   (fn [snapshot] (assoc snapshot :db/uid (uuid)))
   (vcs.core/vims-snapshots vcs owner-uid vims-uid)))

(defn update-snapshots-handler
  [{:keys [db]} [_ {::vims/keys [vcs] :as vims} status-key]]
  (let [snapshots        (new-snapshots vims)
        remote-snapshots (mapv #(select-keys % snapshot/pull-query) snapshots)
        vims'            (assoc vims ::vims/snapshots snapshots)
        db'              (util.mg/add db vims')]
    {:db db'
     :remote
     {:id         :backend
      :event      [::vims.commands/update-snapshots remote-snapshots]
      :status-key status-key}}))

(re-frame/reg-event-fx ::update-snapshots update-snapshots-handler)

;;
;; * Remote queries
;;

;;
;; ** Vims
;;

(defn vims-handler
  [{:keys [db]} [_ vims-uid status-key]]
  {:remote
   {:id               :backend
    :status-key       status-key
    :event            [::vims.queries/vims vims-uid]
    :dispatch-success true}})

(defn vims-success-handler
  [{:keys [db]} [_ vims]]
  {:db (util.mg/add db vims)})

(re-frame/reg-event-fx ::vims              vims-handler)
(re-frame/reg-event-fx ::vims.queries/vims vims-success-handler)

;;
;; ** Deltas
;;

(defn deltas-handler
  [{:keys [db]} [_ uuid-fn vims-uid status-key]]
  {:remote
   {:id               :backend
    :status-key       status-key
    :event            [::vims.queries/deltas vims-uid]
    ;; Close over vims-uid so that the vcs handler can retrieve the vims
    :dispatch-success (fn [deltas]
                        [::vcs.handlers/init uuid-fn vims-uid deltas])}})

(re-frame/reg-event-fx ::deltas deltas-handler)
