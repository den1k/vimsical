(ns vimsical.backend.adapters.cassandra-test
  (:require
   [clojure.core.async :as a]
   [clojure.test :refer [deftest is use-fixtures]]
   [orchestra.spec.test :as st]
   [vimsical.backend.adapters.cassandra.protocol :as p]
   [vimsical.backend.adapters.cassandra.fixture
    :as
    fixture
    :refer
    [*cluster* *connection* *keyspace*]]))

(st/instrument)

(use-fixtures :once fixture/cluster)
(use-fixtures :each fixture/connection)

(deftest dymanic-vars-test
  (is *cluster*)
  (is *connection*)
  (is (:session  *connection*)))

(deftest keyspace-test
  (is (nil? (a/<!! (p/execute-chan *connection* (format "use %s;" *keyspace*))))))
