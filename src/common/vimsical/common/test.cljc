(ns vimsical.common.test
  #?@(:clj
      [(:require [vimsical.common.uuid :as uuid])
       (:import java.util.UUID)]
      :cljs
      [(:require [vimsical.common.uuid :as uuid])]))

;; * UUID

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
  ([tag]
   (or (get (deref *tag->uuid*) tag)
       (let [uuid (uuid/uuid)]
         (swap! *tag->uuid* assoc tag uuid)
         (swap! *uuid->tag* assoc uuid tag)
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
    {:seq s :f (fn [& _] (nth s (swap! n inc)))}))
