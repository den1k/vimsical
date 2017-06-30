(ns vimsical.frontend.vims.handlers
  (:require
   [com.stuartsierra.mapgraph :as mg]
   [re-frame.core :as re-frame]
   [vimsical.common.util.core :as util]
   [vimsical.common.uuid :refer [uuid]]
   [vimsical.frontend.util.mapgraph :as util.mg]
   [vimsical.frontend.util.re-frame :as util.re-frame]
   [vimsical.frontend.vcs.subs :as vcs.subs]
   [vimsical.frontend.vcs.handlers :as vcs.handlers]
   [vimsical.frontend.vcs.sync.handlers :as vcs.sync.handlers]
   [vimsical.frontend.vims.db :as db]
   [vimsical.remotes.backend.vims.commands :as vims.commands]
   [vimsical.remotes.backend.vims.queries :as vims.queries]
   [vimsical.frontend.router.handlers :as router.handlers]
   [vimsical.frontend.router.routes :as router.routes]
   [vimsical.vcs.file :as vcs.file]
   [vimsical.vcs.snapshot :as vcs.snapshot]
   [vimsical.vims :as vims]
   [vimsical.user :as user]
   [vimsical.frontend.vims.db :as vims.db]))

;;
;; * New vims
;;

;; Could parametrize the user prefs here?

(defn- default-files
  ([] (default-files (uuid) (uuid) (uuid)))
  ([html-uid css-uid javascript-uid]
   [(vcs.file/new-file html-uid :text :html)
    (vcs.file/new-file css-uid :text :css)
    (vcs.file/new-file javascript-uid :text :javascript)]))

(defn new-event-fx
  [{:keys [db]} [_ owner status-key opts]]
  (let [new-files (vims/default-files)
        owner'    (select-keys owner [:db/uid])
        {vims-uid :db/uid :as new-vims} (vims/new-vims owner' nil new-files opts)
        ;; This is a bit low-level, we could just conj the vims onto the owner
        ;; and add that, but we'd have to make sure we're passed the full
        ;; app/user, not sure if that'd be inconvenient.
        db'       (util.mg/add-join db owner' ::user/vimsae new-vims)]
    {:db db'
     :remote
         {:id               :backend
          :event            [::vims.commands/new new-vims]
          :dispatch-success (fn [_] [::new-success vims-uid new-vims])
          :dispatch-error   (fn [error] [::new-error vims-uid new-vims error])
          :status-key       status-key}}))

(re-frame/reg-event-fx ::new new-event-fx)
(re-frame/reg-event-fx ::new-success (fn [_ _] (re-frame.loggers/console :log "New vims success")))
(re-frame/reg-event-fx ::new-error (fn [_ e] (re-frame.loggers/console :log "Vims error" e)))

;;
;; * Delete vims
;;

(defn delete-event-fx
  [{:keys [db]} [_ vims status-key]]
  (let [vims-uid (util.mg/->uid db vims)
        db'      (util.mg/remove db vims)]
    {:db db'
     :remote
         {:id               :backend
          :event            [::vims.commands/delete vims-uid]
          :dispatch-success (fn [_] [::delete-success vims-uid])
          :dispatch-error   (fn [_] [::delete-error vims-uid])
          :status-key       status-key}}))

(re-frame/reg-event-fx ::delete delete-event-fx)
(re-frame/reg-event-fx ::delete-success (fn [_ _] (re-frame.loggers/console :log "Delete success")))
(re-frame/reg-event-fx ::delete-error (fn [_ e] (re-frame.loggers/console :log "Delete error" e)))

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
          :dispatch-success (fn [_] [::title-success vims'])
          :dispatch-error   (fn [error] [::title-error vims error])
          :status-key       status-key}}))

(re-frame/reg-event-fx ::title title-event-fx)
(re-frame/reg-event-fx ::title-success (fn [_ _] (re-frame.loggers/console :log "title success")))

;;
;; * Snapshots
;;

(re-frame/reg-event-fx
 ::update-snapshots
 [(util.re-frame/inject-sub
   (fn [[_ vims]] [::vcs.subs/files vims]))]
 (fn [{:keys [db] ::vcs.subs/keys [files]} [_ vims status-key]]
   ;; Dispatch an event per file to update their state, then upload
   {:dispatch-n
    (conj (mapv (fn [file] [::update-snapshot vims file]) files)
          [::update-snapshots-remote vims status-key])}))

(re-frame/reg-event-fx
 ::update-snapshot
 [(util.re-frame/inject-sub
   (fn [[_ vims file]]
     [::vcs.subs/preprocessed-file-string vims file]))]
 (fn [{:keys           [db]
       ::vcs.subs/keys [preprocessed-file-string]}
      [_ vims file]]
   (when preprocessed-file-string
     (let [[_ vims-uid :as vims-ref] (util.mg/->ref db vims)
           {:as vims' {user-uid :db/uid} ::vims/owner} (mg/pull db [:db/uid {::vims/owner [:db/uid]} {::vims/snapshots ['*]}] vims-ref)
           snapshot (vcs.snapshot/new-frontend-snapshot (uuid) user-uid vims-uid file preprocessed-file-string)
           vims''   (update vims' ::vims/snapshots (fnil util/replace-by-or-conj []) ::vcs.snapshot/file-uid snapshot)]
       {:db (util.mg/add db vims'')}))))

(re-frame/reg-event-fx
 ::update-snapshots-remote
 (fn upload-snapshots-event-fx
   [{:keys [db]} [_ vims status-key]]
   ;; Pull the vims again since we know it is now stale
   (let [vims-ref         (util.mg/->ref db vims)
         {::vims/keys [snapshots]} (mg/pull db [{::vims/snapshots ['*]}] vims-ref)
         remote-snapshots (mapv vcs.snapshot/->remote-snapshot snapshots)]
     (when (seq remote-snapshots)
       {:remote
        {:id               :backend
         :event            [::vims.commands/update-snapshots remote-snapshots]
         :dispatch-success (fn [resp] (re-frame.loggers/console :log "SNAPSHOTS success"))
         :dispatch-error   (fn [e] (re-frame.loggers/console :log "SNAPSHOTS error" e))
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
    :dispatch-success (fn [vims] [::vims-success vims-uid vims])
    :dispatch-error   (fn [e] [::vims-error vims-uid e])}})

(defn vims-success-event-fx
  [{:keys [db]} [_ _ vims]]
  {:db (util.mg/add db vims)})

(defn vims-error-event-fx
  [{:keys [db]} [_ _ {:keys [status]}]]
  (when (== 404 status)
    {:dispatch [::router.handlers/route ::router.routes/landing]}))

(re-frame/reg-event-fx ::vims vims-handler)
(re-frame/reg-event-fx ::vims-success vims-success-event-fx)
(re-frame/reg-event-fx ::vims-error vims-error-event-fx)

;;
;; * Async loading flow
;;

(defn load-vims-async-flow
  [vims-uid {:keys [uuid-fn first-dispatch] :or {uuid-fn uuid} :as options}]
  {:pre [(uuid? vims-uid)]}
  (letfn [(event+uuid [[id uuid]] [id uuid])]
    {:id                [::load-vims-async vims-uid]
     :first-dispatch    [::load-vims-async-did-start vims-uid]
     :event-id->seen-fn {::deltas-success event+uuid
                         ::vims-success   event+uuid}
     :rules             [{:when     :seen-all-of?
                          :events   [[::deltas-success vims-uid]
                                     [::vims-success vims-uid]]
                          :dispatch [::load-vims-async-did-complete vims-uid options]}]
     :halt?             true}))

(re-frame/reg-event-fx
 ::load-vims-async-did-start
 (fn [{:keys [db]} [_ vims-uid]]
   {:dispatch-n
    [[::vims vims-uid]
     [::deltas vims-uid]]}))

(re-frame/reg-event-fx
 ::load-vims-async-did-complete
 (fn [{:keys [db]} [_ vims-uid {:keys [uuid-fn] :or {uuid-fn uuid} :as options} ]]
   (let [deltas (vims.db/get-deltas db vims-uid)]
     {:dispatch-n
      [[::vcs.handlers/init vims-uid deltas options]
       [::vcs.sync.handlers/start vims-uid]]})))

(re-frame/reg-event-fx
 ::load-vims
 (fn [{:keys [db]} [_ vims {:keys [uuid-fn] :or {uuid-fn uuid} :as options}]]
   {:pre [vims]}
   (when vims
     (let [[_ vims-uid] (util.mg/->ref db vims)]
       (when (not (vims.db/loaded? db vims))
         {:async-flow (load-vims-async-flow vims-uid options)})))))

;;
;; ** Deltas
;;

(defn deltas-handler
  [{:keys [db]} [_ vims-uid status-key]]
  {:remote
   {:id               :backend
    :status-key       status-key
    :event            [::vims.queries/deltas vims-uid]
    :dispatch-success (fn [deltas] [::deltas-success vims-uid deltas])}})

(defn deltas-success-handler
  [{:keys [db]} [_ vims-uid deltas]]
  {:db (db/set-deltas db vims-uid deltas)})

(re-frame/reg-event-fx ::deltas deltas-handler)
(re-frame/reg-event-fx ::deltas-success deltas-success-handler)
