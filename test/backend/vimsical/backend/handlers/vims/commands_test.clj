(ns vimsical.backend.handlers.vims.commands-test
  (:require
   [vimsical.backend.handlers.vims.commands :as sut]
   [clojure.test :refer [deftest is use-fixtures]]
   [orchestra.spec.test :as st]
   [vimsical.backend.components.server.test :as server.test]
   [vimsical.backend.system.fixture :refer [system]]
   [vimsical.backend.data :as data]
   [vimsical.common.test :refer [uuid]]
   [vimsical.remotes.backend.vims.commands :as vims.commands]
   [vimsical.user :as user]))

(st/instrument)

(use-fixtures :each system)

;;
;; * New
;;

(deftest new-test
  (let [actual (server.test/response-for [::vims.commands/new data/vims])]
    (is (server.test/status-ok? actual))))

;;
;; * Title
;;

::sut/set-title!

;;
;; * Snapshots
;;

::sut/update-snapshots
