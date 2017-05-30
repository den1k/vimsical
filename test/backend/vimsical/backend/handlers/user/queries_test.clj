(ns vimsical.backend.handlers.user.queries-test
  (:require [clojure.test :refer [are deftest is testing use-fixtures]]
            [vimsical.backend.handlers.user.queries :as sut]
            [vimsical.backend.system.fixture :refer [*service-fn* system]]))

(use-fixtures :each system)

(deftest me-test)
