(ns vimsical.backend.handlers.me.queries-test
  (:require
   [vimsical.backend.system.fixture :refer [system *service-fn*]]
   [vimsical.backend.handlers.me.queries :as sut]
   [clojure.test :refer [deftest is are testing use-fixtures]]))

(use-fixtures :each system)

(deftest me-test)
