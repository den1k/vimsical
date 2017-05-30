(ns vimsical.integration.remotes.backend.vcs.commands-test
  (:require
   [clojure.test :refer [deftest is use-fixtures]]
   [com.stuartsierra.mapgraph :as mg]
   [day8.re-frame.test :as re-frame.test]
   [orchestra.spec.test :as st]
   [re-frame.core :as re-frame]
   [vimsical.vcs.data.gen.diff :as diff]
   [vimsical.backend.data :as data]
   [vimsical.backend.handlers.me.queries :as me.queries]
   [vimsical.backend.system.fixture :as system.fixture]
   [vimsical.common.test :refer [uuid]]
   [vimsical.frontend.db :as db]
   [vimsical.frontend.remotes.fx :as frontend.remotes.fx]
   [vimsical.frontend.vcs.handlers :as vcs.handlers]
   [vimsical.queries.user :as queries.user]
   [datomic.api :as d]
   [vimsical.vcs.core :as vcs]
   [vimsical.frontend.vcs.subs :as vcs.subs]
   [vimsical.vcs.sync :as vcs.sync]
   [vimsical.frontend.vcs.sync.handlers :as vcs.sync.handlers]))

(st/instrument)

;;
;; * Re-frame
;;

(def test-db (db/new-db {}))

(defn re-frame-fixture
  [f]
  (re-frame.test/with-temp-re-frame-state
    (re-frame.test/run-test-async
     (re-frame/reg-event-db ::test-db-init (constantly test-db))
     (re-frame/dispatch [::test-db-init])
     (f))))

;;
;; * Fixtures
;;

(use-fixtures :each
  system.fixture/system
  (system.fixture/with-user data/user)
  system.fixture/session
  system.fixture/vims
  re-frame-fixture)

;;
;; * Helpers
;;

(defn get-sync
  [db-sub]
  (get-in @db-sub [:app/sync (uuid ::data/vims)]))

;;
;; * Deltas by branch-uid
;;

(deftest edits-test
  (let [expect-html      "<body><h1>Hello</h1></body>"
        html-edit-events (diff/diffs->edit-events
                          ""
                          ["<body></body>"]
                          ["<body><h1>YO</h1></body>"]
                          [expect-html])
        expect-css       "body { color: orange; }"
        css-edit-events  (diff/diffs->edit-events
                          ""
                          ["body { color: red; }"]
                          [expect-css])]
    ;; Init
    (re-frame/dispatch [::vcs.handlers/init data/vims []])
    ;; Dispatch edit events
    (doseq [e html-edit-events]
      (re-frame/dispatch [::vcs.handlers/add-edit-event data/vims data/html-file e]))
    (doseq [e css-edit-events]
      (re-frame/dispatch [::vcs.handlers/add-edit-event data/vims data/css-file e]))
    (re-frame.test/wait-for
     [#{::vcs.sync.handlers/sync-success}
      #{::vcs.sync.handlers/sync-error}]
     (do
       (is (= expect-html (deref (re-frame/subscribe [::vcs.subs/file-string data/vims data/html-file]))))
       (is (= expect-css  (deref (re-frame/subscribe [::vcs.subs/file-string data/vims data/css-file]))))))))
