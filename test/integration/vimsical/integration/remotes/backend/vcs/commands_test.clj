(ns vimsical.integration.remotes.backend.vcs.commands-test
  (:require
   [clojure.test :refer [deftest is use-fixtures]]
   [com.stuartsierra.mapgraph :as mg]
   [day8.re-frame.test :as re-frame.test]
   [orchestra.spec.test :as st]
   [re-frame.core :as re-frame]
   [vimsical.backend.data :as data]
   [vimsical.backend.system.fixture :as system.fixture]
   [vimsical.common.test :refer [uuid]]
   [vimsical.frontend.db :as db]
   [vimsical.frontend.util.mapgraph :as util.mg]
   [vimsical.frontend.util.re-frame :refer [<sub]]
   [vimsical.frontend.vcs.handlers :as vcs.handlers]
   [vimsical.frontend.vcs.subs :as vcs.subs]
   [vimsical.frontend.vcs.sync.handlers :as vcs.sync.handlers]
   [vimsical.vcs.branch :as branch]
   [vimsical.vcs.data.gen.diff :as diff]
   [vimsical.vcs.lib :as lib]))

(st/instrument)

;;
;; * Re-frame
;;

(def test-db
  (-> (db/new-db {:app/user data/user :app/vims data/vims})
      (mg/add data/vims2)))

(defn re-frame-fixture
  [f]
  (re-frame/clear-subscription-cache!)
  (re-frame.test/with-temp-re-frame-state
    (re-frame.test/run-test-sync
     (re-frame/reg-event-db ::test-db-init (constantly test-db))
     (re-frame/dispatch [::test-db-init])
     (re-frame/dispatch-sync [::vcs.handlers/init (uuid ::data/vims) nil])
     (re-frame/dispatch-sync [::vcs.handlers/init (uuid ::data/vims2) nil])
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
;; * Libs
;;

(deftest libs-test
  (let [lib1 (lib/new-lib :javascript "https://cdnjs.cloudflare.com/ajax/libs/three.js/84/three.min.js")
        lib2 (lib/new-lib :javascript "https://cdnjs.cloudflare.com/ajax/libs/three.js/85/three.min.js")]
    (re-frame/dispatch [::vcs.handlers/add-lib data/vims lib1])
    (re-frame/dispatch [::vcs.handlers/add-lib {:db/uid (uuid ::data/vims2)} lib1])
    (re-frame/dispatch [::vcs.handlers/add-lib {:db/uid (uuid ::data/vims2)} lib2])
    (let [libs1 (<sub [::vcs.subs/libs data/vims])
          libs2 (<sub [::vcs.subs/libs {:db/uid (uuid ::data/vims2)}])]
      (is (= #{lib1} libs1))
      (is (= #{lib1 lib2} libs2)))
    (re-frame/dispatch [::vcs.handlers/remove-lib {:db/uid (uuid ::data/vims2)} lib1])
    (let [libs1 (<sub [::vcs.subs/libs data/vims])
          libs2 (<sub [::vcs.subs/libs {:db/uid (uuid ::data/vims2)}])]
      (is (= #{lib1} libs1))
      (is (= #{lib2} libs2)))))

;;
;; * Deltas by branch-uid
;;
#_
(deftest edits-test
  (re-frame/dispatch-sync [::vcs.handlers/init uuid (uuid ::data/vims) nil])
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
