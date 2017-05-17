(ns vimsical.backend.components.datomic-test
  (:require
   [clojure.test :as t]
   [orchestra.spec.test :as st]
   [vimsical.backend.components.datomic :as sut]
   [vimsical.backend.components.datomic.fixture :refer [datomic *datomic*]]))

(st/instrument)

(t/use-fixtures :each datomic)

(t/deftest connection-test
  (t/is (:conn *datomic*)))

(t/deftest create-schema-test
  (t/is (pos? (:datoms (sut/create-schema! *datomic*)))))
