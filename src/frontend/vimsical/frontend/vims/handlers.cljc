(ns vimsical.frontend.vims.handlers
  (:require
   [re-frame.core :as re-frame]
   [vimsical.common.uuid :refer [uuid]]
   [vimsical.frontend.util.mapgraph :as util.mg]
   [vimsical.frontend.vcs.subs :as vcs.subs]
   [vimsical.frontend.vcs.handlers :as vcs.handlers]
   [vimsical.vcs.snapshot :as vcs.snapshot]
   [vimsical.remotes.backend.vims.commands :as vims.commands]
   [vimsical.remotes.backend.vims.queries :as vims.queries]
   [vimsical.user :as user]
   [vimsical.vcs.core :as vcs.core]
   [vimsical.vcs.core :as vcs.core]
   [vimsical.queries.snapshot :as queries.snapshot]
   [vimsical.vcs.file :as vcs.file]
   [vimsical.frontend.vcs.queries :as vcs.queries]
   [vimsical.vims :as vims]
   [com.stuartsierra.mapgraph :as mg]
   [vimsical.frontend.util.re-frame :as util.re-frame]
   [vimsical.common.util.core :as util]
   [vimsical.vcs.file :as file]))

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

(defn new-event-fx
  [{:keys [db]} [_ owner status-key opts]]
  (let [new-files (vims/default-files)
        owner'    (select-keys owner [:db/uid])
        new-vims  (vims/new-vims owner' nil new-files opts)
        ;; This is a bit low-level, we could just conj the vims onto the owner
        ;; and add that, but we'd have to make sure we're passed the full
        ;; app/user, not sure if that'd be inconvenient.
        db'       (util.mg/add db new-vims)]
    {:db db'
     :remote
     {:id               :backend
      :event            [::vims.commands/new new-vims]
      :dispatch-success (fn [_] [::new-success new-vims])
      :dispatch-error   (fn [error] [::new-error new-vims error])
      :status-key       status-key}}))

(re-frame/reg-event-fx ::new new-event-fx)
(re-frame/reg-event-fx ::new-success (fn [_ _] (println "New vims success")))
(re-frame/reg-event-fx ::new-error (fn [_ e] (println "Vims error" e)))

;;
;; * Title
;;

(defn title-event-fx
  [{:keys [db]} [_ vims title status-key]]
  (let [vims'  (assoc vims ::vims/title title)
        remote (select-keys vims' [:db/uid ::vims/title])
        db'    (util.mg/add db vims')]
    {:db db'
     :remote
     {:id               :backend
      :event            [::vims.commands/title remote]
      :dispatch-success (fn [_] [::title-success vims])
      :dispatch-error   (fn [error] [::title-error vims error])
      :status-key       status-key}}))

(re-frame/reg-event-fx ::title title-event-fx)
(re-frame/reg-event-fx ::title-success (fn [_ _] (println "title success")))

;;
;; * Snapshots
;;

(re-frame/reg-event-fx
 ::update-snapshots
 [(util.re-frame/inject-sub
   (fn [[_ vims]] [::vcs.subs/files vims]))]
 (fn [{:keys           [db]
       ::vcs.subs/keys [files]}
      [_ vims status-key]]
   ;; XXX passed vims doesn't have an owner
   (let [vims' (mg/pull db [:db/uid {::vims/owner [:db/uid]}] (util.mg/->ref db vims))]
     ;; Dispatch an event per file to update their state, then upload
     {:dispatch-n
      (conj
       (mapv (fn [file] [::update-snapshot vims' file]) files)
       [::update-snapshots-remote vims' status-key])})))

(re-frame/reg-event-fx
 ::update-snapshot
 [(util.re-frame/inject-sub
   (fn [[_ vims file]]
     [::vcs.subs/preprocessed-file-string vims file]))]
 (fn [{:keys           [db]
       ::vcs.subs/keys [preprocessed-file-string]}
      [_ {:as                vims
          vims-uid           :db/uid
          {user-uid :db/uid} ::vims/owner}
       file]]
   (when preprocessed-file-string
     (let [snapshot (vcs.snapshot/new-frontend-snapshot (uuid) user-uid vims-uid file preprocessed-file-string)
           _ (assert (nil? (::file/type snapshot)))
           vims'    (update vims ::vims/snapshots util/replace-by-or-conj ::vcs.snapshot/file-uid snapshot)]
       {:db (util.mg/add db vims')}))))

(re-frame/reg-event-fx
 ::update-snapshots-remote
 (fn upload-snapshots-event-fx
   [{:keys [db]} [_ vims status-key]]
   ;; Pull the vims again since we know it is now stale
   (let [vims-ref                  (util.mg/->ref db vims)
         {::vims/keys [snapshots]} (mg/pull db [{::vims/snapshots ['*]}] vims-ref)
         remote-snapshots          (mapv vcs.snapshot/->remote-snapshot snapshots)]
     (when (seq remote-snapshots)
       {:remote
        {:id               :backend
         :event            [::vims.commands/update-snapshots remote-snapshots]
         :dispatch-success (fn [resp] (println "SNAPSHOTS success"))
         :dispatch-error   (fn [e] (println "SNAPSHOTS error" e))
         :status-key       status-key}}))))

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

(defn vims-success-event-fx
  [{:keys [db]} [_ vims]]
  {:db (util.mg/add db vims)})

(re-frame/reg-event-fx ::vims              vims-handler)
(re-frame/reg-event-fx ::vims.queries/vims vims-success-event-fx)

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
