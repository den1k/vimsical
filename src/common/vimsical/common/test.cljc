(ns vimsical.common.test
  (:require
   [clojure.data :as data]
   #?(:clj [clojure.test :as t]
      :cljs [cljs.test :as t :include-macros true])))

(defmacro is=
  ([expected actual]
   `(t/is (~'= ~expected ~actual)))
  ([expected actual second]
   `(t/is (~'= ~expected ~actual ~second)))
  ([expected actual second third]
   `(t/is (~'= ~expected ~actual ~second ~third))))

(defmacro isnt=
  ([expected actual]
   `(t/is (~'not= ~expected ~actual)))
  ([expected actual second]
   `(t/is (~'not= ~expected ~actual ~second)))
  ([expected actual second third]
   `(t/is (~'not= ~expected ~actual ~second ~third))))

(defn diff=
  [expected actual]
  (let [[only-in-expected only-in-actual] (data/diff expected actual)]
    (t/is (nil? only-in-expected))
    (t/is (nil? only-in-actual))))
