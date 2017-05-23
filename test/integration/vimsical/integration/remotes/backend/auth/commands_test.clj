(ns vimsical.integration.remotes.backend.auth.commands-test
  (:require
   [clojure.test :refer [deftest is are testing use-fixtures]]
   [re-frame.core :as re-frame]
   [vimsical.frontend.db :as db]
   [vimsical.remotes.backend.auth.commands :as remotes.backend.auth.commands]
   [vimsical.integration.remotes.backend :as frontend.remotes.backend]
   [vimsical.backend.system.fixture :as system.fixture]
   [vimsical.backend.handlers.auth.commands :as backend.handlers.auth.commands]
   [day8.re-frame.test :as re-frame.test]))


(defn re-frame-fixture
  [f]
  (re-frame.test/with-temp-re-frame-state
    (re-frame/dispatch [::db/init])
    (f)))

(use-fixtures :each system.fixture/system)

(deftest run-test-sync--basic-flow

  (let [hello-reaction (re-frame/subscribe [:hello-sub])
        db-reaction    (re-frame/subscribe [:db-sub])]
    (re-frame.test/run-test-sync
     (re-frame/dispatch [:initialise-db])
     (is (= "world" @hello-reaction))
     (is (= "nope" (:goodbye @db-reaction))) ; Not true, reports failure.
     (re-frame/dispatch [:update-db "bugs"])
     (is (= "world" @hello-reaction))
     (is (= {:hello "world", :goodbye "bugs"} @db-reaction)))))
