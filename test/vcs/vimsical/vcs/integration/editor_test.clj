(ns vimsical.vcs.integration.editor-test
  ;; (:require
  ;;  [clojure.test :as t :refer [deftest is are]]
  ;;  [orchestra.spec.test :as st]
  ;;  [vimsical.common.test :refer [is= diff= uuid uuid-gen]]
  ;;  [re-frame.core :as re-frame]
  ;;  [re-frame.subs :as re-frame.subs]
  ;;  [re-frame.interop :as re-frame.interop]
  ;;  [com.stuartsierra.mapgraph :as mg]
  ;;  [com.stuartsierra.subgraph :as sg]
  ;;  [vimsical.vcs.edit-event :as edit-event]
  ;;  [vimsical.vcs.branch :as branch]
  ;;  [vimsical.vcs.file :as file]
  ;;  [vimsical.vcs.state.files :as state.files]
  ;;  [vimsical.vcs.data.gen.diff :as diff])
  )

;; (st/instrument)

;; NOTE on performance:
;;
;; We need to consider whether there are performance implications in joining the
;; vcs state onto the vims vs keeping it separate in a top level entity

;; * DB

;; (def links
;;   {:app/user [:db/id (uuid :user)]
;;    :app/vims [:db/id (uuid :vims)]})

;; (def user
;;   {:db/id           (uuid :user)
;;    :user/first-name "foo"
;;    :user/last-name  "bar"
;;    :user/vimsae     [{:db/id       (uuid :vims)
;;                       :vims/branches
;;                       [{:db/id (uuid :branch)
;;                         :branch/files
;;                         [{:db/id (uuid :html) ::file/type :text ::file/sub-type :html ::file/name "My HTML file"}
;;                          {:db/id (uuid :css) ::file/type :text ::file/sub-type :css ::file/name "My CSS file"}]}]
;;                       :vims/deltas []
;;                       ;;
;;                       ;; VCS state
;;                       ;;
;;                       :vims/vcr    {:vcr/delta-id nil}}]})
;; (def vcs-state
;;   {:vcs.state/by-vims-id         (uuid :vims)
;;    :vimsical.vcs.state/file-id   (uuid :file)
;;    :vimsical.vcs.state/branch-id (uuid :branch)
;;    :vimsical.vcs.state/delta-id  nil
;;    :vimsical.vcs.state/files     state.files/empty-state-by-file-id})

;; (def default-db
;;   (-> (mg/new-db)
;;       (mg/add-id-attr :db/id :vcs.state/by-vims-id)
;;       (merge links)
;;       (mg/add user)
;;       (mg/add vcs-state)))

;; (defn ref-for
;;   ([db ref-key ref] (ref-for ref-key (get db ref)))
;;   ([ref-key [_ ref-id]] [ref-key ref-id]))

;; (deftest ref-for-test
;;   (is (= [:foo/bar (uuid :user)] (ref-for default-db :foo/bar :app/user)))
;;   (is (= [:foo/bar (uuid :vims)] (ref-for default-db :foo/bar :app/vims))))


;; ;; * Subscriptions

;; (def <sub (comp deref re-frame/subscribe))


;; ;; ** Refs
;; ;;

;; ;; We want these in order to compose reactions

;; (re-frame/reg-sub :app.user/ref (fn [db _] (:app/user db)))
;; (re-frame/reg-sub :app.vims/ref (fn [db _] (:app/vims db)))

;; (deftest ref-subs-test
;;   (is (= [:db/id (uuid :user)] (<sub [:app.user/ref])))
;;   (is (= [:db/id (uuid :vims)] (<sub [:app.vims/ref]))))

;; ;; The state of the vcs for the current vims

;; (defn vcs-state-sub-handler [db _]
;;   (re-frame.interop/make-reaction
;;    (fn []
;;      (let [state-ref (ref-for @db :vcs.state/by-vims-id :app/vims)]
;;        (get @db state-ref {})))))

;; (re-frame/reg-sub-raw :vcs/state vcs-state-sub-handler)

;; (deftest vcs-state-sub-test
;;   (is (= vcs-state (<sub [:vcs/state]))))

;; ;; A view of the vcs state that returns only the current delta id

;; (re-frame/reg-sub
;;  :vcs.state/delta-id
;;  (fn [_] (re-frame/subscribe [:vcs/state]))
;;  (fn [vcs-state _]
;;    (get vcs-state :vimsical.vcs.state/delta-id)))

;; (re-frame/reg-sub
;;  :vcs.state/files
;;  (fn [_] (re-frame/subscribe [:vcs/state]))
;;  (fn [vcs-state [_ delta-id]]
;;    (cond-> vcs-state
;;      true     (get :vimsical.vcs.state/files state.files/empty-state-by-file-id)
;;      delta-id (get delta-id))))


;; ;; * Handlers

;; (defn handle-edit-event
;;   [db [_ editor-state editor-effects edit-event]]
;;   (let [state-ref                                   (ref-for db :vcs.state/by-vims-id :app/vims)
;;         {:vimsical.vcs.state/keys [files] :as data} (get db state-ref)
;;         [files' delta-id']                          (state.files/add-edit-event files editor-state editor-effects edit-event)]
;;     (-> db
;;         (assoc-in [state-ref :vimsical.vcs.state/files] files')
;;         (assoc-in [state-ref :vimsical.vcs.state/delta-id] delta-id'))))

;; (re-frame/reg-event-db ::edit-event handle-edit-event)

;; ;; * Fixtures

;; (re-frame/reg-event-db ::init (fn [_ _] default-db))

;; (defn setup-db [f]
;;   (println "setup...")
;;   (re-frame.subs/clear-subscription-cache!)
;;   (re-frame/dispatch-sync [::init])
;;   (f))

;; (t/use-fixtures :each setup-db)


;; ;; * Integration tests

;; (deftest editor-integration-test
;;   (letfn [(dispatch-edit-events! [file-id editor-effects edit-events]
;;             (doseq [edit-event edit-events]
;;               (let [editor-state {::state.files/branch-id (uuid :branch)
;;                                   ::state.files/file-id   file-id
;;                                   ::state.files/delta-id  (<sub [:vcs.state/delta-id])}]
;;                 (re-frame/dispatch-sync
;;                  [::edit-event editor-state editor-effects edit-event]))))]
;;     (let [{uuids :seq uuid-fn :f} (uuid-gen)
;;           expect-html             "<body><h1>Hello</h1></body>"
;;           html-edit-events        (diff/diffs->edit-events
;;                                    ""
;;                                    ["<body></body>"]
;;                                    ["<body><h1>YO</h1></body>"]
;;                                    [expect-html])
;;           expect-css              "body { color: orange; }"
;;           css-edit-events         (diff/diffs->edit-events
;;                                    ""
;;                                    ["body { color: red; }"]
;;                                    [expect-css])
;;           editor-effects          {::state.files/pad-fn       (constantly 0)
;;                                    ::state.files/uuid-fn      (fn [e] (uuid-fn))
;;                                    ::state.files/timestamp-fn (constantly 1)}
;;           _                       (dispatch-edit-events! (uuid :html) editor-effects html-edit-events)
;;           _                       (dispatch-edit-events! (uuid :css)  editor-effects css-edit-events)
;;           latest-delta-id         (<sub [:vcs.state/delta-id])
;;           latest-files            (<sub [:vcs.state/files latest-delta-id])
;;           actual-html             (-> latest-files (get (uuid :html)) ::state.files/string)
;;           actual-css              (-> latest-files (get (uuid :css))  ::state.files/string)]
;;       (is (= expect-html actual-html))
;;       (is (= expect-css actual-css)))))
