(ns vimsical.backend.adapters.redis-test
  (:require
   [clojure.test :refer [deftest is use-fixtures]]
   [vimsical.backend.adapters.redis.fixture :refer [*redis* redis]]
   [taoensso.carmine :as car]))

(use-fixtures :each redis)

(deftest connection-test
  (is *redis*))

(deftest read-write-test
  (car/wcar redis (car/set :foo :bar))
  (is (= "bar" (car/wcar redis (car/get :foo)))))

(deftest flush-each-test
  (is (nil? (car/wcar redis (car/get :foo)))))
