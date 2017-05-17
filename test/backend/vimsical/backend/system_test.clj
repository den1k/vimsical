(ns vimsical.backend.system-test
  (:require
   [clojure.test :refer [deftest is use-fixtures]]
   [orchestra.spec.test :as st]
   [vimsical.backend.system.fixture :refer [*system* *service-fn* system]]))

(st/instrument)

(use-fixtures :each system)

(deftest start-test
  (is *system*)
  (is *service-fn*))
