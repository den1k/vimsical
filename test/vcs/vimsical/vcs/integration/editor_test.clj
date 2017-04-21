(ns vimsical.vcs.integration.editor-test
  (:require
   [clojure.test :as t :refer [deftest is are]]
   [orchestra.spec.test :as st]
   [vimsical.common.test :refer [is= diff= uuid uuid-gen]]
   [re-frame.core :as re-frame]
   [re-frame.interop :as interop]
   [com.stuartsierra.mapgraph :as mg]
   [com.stuartsierra.subgraph :as sg]
   [vimsical.vcs.edit-event :as edit-event]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.file :as file]
   [vimsical.vcs.state.files :as state.files]
   [vimsical.vcs.data.gen.diff :as diff]))

(st/instrument)

;; NOTE on performance:
;;
;; Consider a situation where we have :vims/deltas on the vims entity. We store
;; a link to the vims in :app/vims and now we want to create a subscription for
;; the player that will display the :vims/title.


;; * DB

(let [links     {:app/user [:db/id (uuid :user)]
                 :app/vims [:db/id (uuid :vims)]}
      user      {:db/id           (uuid :user)
                 :user/first-name "foo"
                 :user/last-name  "bar"
                 :user/vimsae     [{:db/id       (uuid :vims)
                                    :vims/branches
                                    [{:db/id (uuid :branch)
                                      :branch/files
                                      [{:db/id (uuid :html) ::file/type :text ::file/sub-type :html ::file/name "My HTML file"}
                                       {:db/id (uuid :css) ::file/type :text ::file/sub-type :css ::file/name "My CSS file"}]}]
                                    :vims/deltas []
                                    ;;
                                    ;; VCS state
                                    ;;
                                    :vims/vcr    {:vcr/delta-id nil}}]}
      vcs-state {:vcs.state/by-vims-id         (uuid :vims)
                 :vimsical.vcs.state/file-id   (uuid :file)
                 :vimsical.vcs.state/branch-id (uuid :branch)
                 :vimsical.vcs.state/delta-id  nil
                 :vimsical.vcs.state/files     state.files/empty-states}]
  (def default-db
    (-> (mg/new-db)
        (mg/add-id-attr :db/id :vcs.state/by-vims-id)
        (merge links)
        (mg/add user)
        (mg/add vcs-state))))

(defn ref-for
  ([db ref-key ref] (ref-for ref-key (get db ref)))
  ([ref-key [_ ref-id]] [ref-key ref-id]))

(comment
  (ref-for default-db :foo/bar :app/user))


;; * Subscriptions

(def <sub (comp deref re-frame/subscribe))

(defn <sub-ref-for
  [ref-key sub-event]
  (let [[_ ref-id] (<sub sub-event)]
    [ref-key ref-id]))


;; ** Refs
;;

;; We want these in order to compose reactions

(re-frame/reg-sub :app.user/ref (fn [db _] (:app/user db)))
(re-frame/reg-sub :app.vims/ref (fn [db _] (:app/vims db)))

;; The state of the vcs for the current vims

(re-frame/reg-sub
 :vcs/state
 (fn [db _]
   (interop/make-reaction
    (fn []
      (get @db (<sub-ref-for :vcs.state/by-vims-id [:app.vims/ref]))))))

;; A view of the vcs state that returns only the current delta id

(re-frame/reg-sub
 :vcs.state/delta-id
 (fn [_] (re-frame/subscribe [:vcs/state]))
 (fn [vcs-state _]
   (:vimsical.vcs.state/delta-id vcs-state)))

(re-frame/reg-sub
 :vcs.state/files
 (fn [_] (re-frame/subscribe [:vcs/state]))
 (fn [vcs-state _]
   (:vimsical.vcs.state/files vcs-state)))


;; * Handlers

(re-frame/reg-event-db ::init (constantly default-db))

(defn handle-edit-event
  [db [_ editor-state editor-effects edit-event]]
  (println {:ee edit-event :editor editor-state})
  (let [state-ref                          (ref-for db :vcs.state/by-vims-id :app/vims)
        {:vimsical.vcs.state/keys [files] :as data} (get db state-ref)
        _ (println {:ref state-ref :data data})
        [files' delta-id']                 (state.files/add-edit-event files editor-state editor-effects edit-event)]
    (-> db
        (assoc-in [state-ref :vimsical.vcs.state/files] files')
        (assoc-in [state-ref :vimsical.vcs.state/delta-id] delta-id'))))

(re-frame/reg-event-db ::edit-event handle-edit-event)

;; * Fixtures

(defn setup-db [f]
  (re-frame/dispatch-sync [::init])
  (f))

(t/use-fixtures :each setup-db)


;; * Integration tests

(deftest editor-integration-test
  (let [{uuids :seq uuid-fn :f} (uuid-gen)
        html-edit-events          (diff/diffs->edit-events
                                   ""
                                   ["<body></body>"]
                                   ["<body><h1>YO</h1></body>"]
                                   ["<body><h1>Hello</h1></body>"])
        css-edit-events           (diff/diffs->edit-events
                                   ""
                                   ["body { color: red; }"]
                                   ["body { color: orange; }"])
        edit-events               (into html-edit-events css-edit-events)
        ;; Effects
        editor-effects            {::state.files/pad-fn       (constantly 0)
                                   ::state.files/uuid-fn      uuid-fn
                                   ::state.files/timestamp-fn (constantly 1)}
        ;; Dynamic data
        editor-state              {::state.files/branch-id (uuid :branch)
                                   ::state.files/file-id   (uuid :html)
                                   ::state.files/delta-id  nil}]

    (doseq [ee edit-events]
      (re-frame/dispatch-sync
       [::edit-event
        {::state.files/branch-id (uuid :branch)
         ::state.files/file-id   (uuid :html)
         ::state.files/delta-id  (<sub [:vcs.state/delta-id])}
        editor-effects ee]))))
