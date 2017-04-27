(ns vimsical.common.core-test
  (:require
   [vimsical.common.core :as sut]
   [clojure.test :as t]
   #?(:clj [orchestra.spec.test :as st])))

#?(:clj (st/instrument))

(t/deftest core-test (t/is true))
