(ns vimsical.backend.handlers.vims.commands-test
  (:require
   [clojure.test :refer [deftest is use-fixtures]]
   [orchestra.spec.test :as st]
   [vimsical.backend.components.server.test :as server.test]
   [vimsical.backend.data :as data]
   [vimsical.backend.handlers.vims.commands :as sut]
   [vimsical.backend.system.fixture :as system.fixture]
   [vimsical.remotes.backend.vims.commands :as vims.commands]))

(st/instrument)

(use-fixtures :each
  system.fixture/system
  (system.fixture/with-user data/user)
  system.fixture/session)

;;
;; * New
;;

(deftest new-test
  (let [actual (server.test/response-for [::vims.commands/new data/vims])]
    (is (server.test/status-ok? actual))))

;;
;; * Title
;;

::sut/title

;;
;; * Snapshots
;;

::sut/update-snapshots
