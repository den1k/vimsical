(ns vimsical.integration.remotes.backend.vcs.commands-test
  (:require
   [clojure.test :refer [deftest is use-fixtures]]
   [day8.re-frame.test :as re-frame.test]
   [orchestra.spec.test :as st]
   [re-frame.core :as re-frame]
   [vimsical.backend.data :as data]
   [vimsical.backend.system.fixture :as system.fixture]
   [vimsical.common.test :refer [uuid]]
   [vimsical.frontend.db :as db]
   [vimsical.frontend.vcs.handlers :as vcs.handlers]
   [vimsical.frontend.vcs.subs :as vcs.subs]
   [vimsical.frontend.vcs.sync.handlers :as vcs.sync.handlers]
   [vimsical.vcs.data.gen.diff :as diff]))

(st/instrument)

;;
;; * Re-frame
;;

(def test-db (db/new-db
              {:app/user data/user
               :app/vims data/vims}))

(defn re-frame-fixture
  [f]
  (re-frame.test/with-temp-re-frame-state
    (re-frame.test/run-test-sync
     (re-frame/reg-event-db ::test-db-init (constantly test-db))
     (re-frame/dispatch-sync [::test-db-init])
     (re-frame/dispatch-sync [::vcs.handlers/init uuid (uuid ::data/vims) nil])
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
#_
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
    (re-frame/dispatch [::vcs.sync.handlers/start (uuid ::data/vims)])
    ;; Dispatch edit events
    (doseq [e html-edit-events]
      (re-frame/dispatch [::vcs.handlers/add-edit-event {:db/uid (uuid ::data/vims)} data/html-file e]))
    (doseq [e css-edit-events]
      (re-frame/dispatch [::vcs.handlers/add-edit-event data/vims data/css-file e]))
    (do (is (= expect-html (deref (re-frame/subscribe [::vcs.subs/file-string data/vims data/html-file]))))
        (is (= expect-css  (deref (re-frame/subscribe [::vcs.subs/file-string data/vims data/css-file])))))))
