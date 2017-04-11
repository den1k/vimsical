(ns vimsical.common.test
  (:require
   [clojure.data :as data]
   [clojure.test :as t]
   [vimsical.common.uuid :as uuid]))


;; * Assertions

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


;; * UUID

(def ^{:dynamic true :private true} *uuids* nil)

(defn uuid-fixture
  "Setup a uuid cache to retrieve stable uuids by key with `uuid`."
  [f]
  (binding [*uuids* (atom {})] (f)))

(defn uuid
  "Return a stable uuid for the given value."
  ([] (uuid/uuid))
  ([any]
   (or (get (deref *uuids*) any)
       (let [uuid (uuid/uuid)]
         (swap! *uuids* assoc any uuid)
         uuid))))

(defn- uuid-seq*
  [sym current]
  (lazy-seq
   (cons (uuid [sym current]) (uuid-seq* sym (inc current)))))

(defn uuid-seq
  "Return a lazy-seq of uuids. Values are both unique and stable per seq."
  [] (uuid-seq* (gensym) 0))

(defn uuid-gen
  "Return a map of `:seq` :and `:f` where successive invocations of `f` return
  the next uuid in `:seq`. Useful in testing functions that create uuids by
  side-effect, we can pass `f` as a generator to the sut while asserting against
  the values of `seq`."
  []
  (let [n (atom -1)
        s (uuid-seq)]
    {:seq s :f #(nth s (swap! n inc))}))
