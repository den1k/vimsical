(ns vimsical.common.test
  #?@(:clj
      [(:require
        [vimsical.common.uuid :as uuid]
        [clojure.test :as t]
        [clojure.data :as data])
       (:import java.util.UUID)]
      :cljs
      [(:refer-clojure :exclude [uuid])
       (:require [vimsical.common.uuid :as uuid])]))

;;
;; * Helpers
;;

;; Only use this when working through tests,
#?(:clj
   (defn diff=
     [expected actual]
     (let [[only-in-expected only-in-actual] (data/diff expected actual)]
       (t/is (nil? only-in-expected))
       (t/is (nil? only-in-actual)))))

;;
;; * UUID
;;

(def ^{:dynamic true :private true} *print-tags* true)
(def ^{:dynamic true :private true} *tag->uuid* (atom {}))
(def ^{:dynamic true :private true} *uuid->tag* (atom {}))

;; Override print-method to show the tags instead of the UUID values, useful
;; when looking at test data!

#?(:clj
   (defmethod print-method UUID [^UUID uuid ^java.io.Writer w]
     (letfn [(print-default []
               (print-method (symbol "#uuid ") w)
               (print-method (.toString uuid) w))
             (print-tag [tag]
               (print-method (symbol "#uuid ") w)
               (print-method tag w))]
       (if *print-tags*
         (if-some [tag (get @*uuid->tag* uuid)]
           (print-tag tag)
           (print-default))
         (print-default)))))

(defn uuid-fixture
  "Setup a uuid cache to retrieve stable uuids by key with `uuid`."
  [f]
  (binding [*tag->uuid* (atom @*tag->uuid*)]
    (binding [*uuid->tag* (atom @*uuid->tag*)]
      (f))))

(defn uuid
  "Return a stable uuid for the given value."
  ([] (uuid/uuid))
  ([& [?uuid-fn & ?rest :as tags]]
   (let [uuid-fn (if (fn? ?uuid-fn) ?uuid-fn uuid/uuid)
         tags    (if (fn? ?uuid-fn) ?rest tags)]
     (or (get (deref *tag->uuid*) tags)
         (let [uuid (uuid-fn)]
           (swap! *tag->uuid* assoc tags uuid)
           (swap! *uuid->tag* assoc uuid tags)
           uuid)))))

(defn- uuid-seq*
  [uuid-fn sym current]
  (lazy-seq
   (cons
    (uuid uuid-fn sym current)
    (uuid-seq* uuid-fn sym (inc current)))))

(defn uuid-seq
  "Return a lazy-seq of uuids. Values are both unique and stable per seq."
  ([] (uuid-seq uuid/uuid))
  ([uuid-fn] (uuid-seq* uuid-fn (gensym) 0)))

(defn uuid-gen
  "Return a map of `:seq` :and `:f` where successive invocations of `f` return
  the next uuid in `:seq`. Useful in testing functions that create uuids by
  side-effect, we can pass `f` as a generator to the sut while asserting against
  the values of `seq`."
  ([] (uuid-gen uuid/uuid))
  ([uuid-fn]
   (let [n (atom -1)
         s (uuid-seq uuid-fn)]
     {:seq s :f (fn [& _] (nth s (swap! n inc))) :reset #(reset! n -1)})))
